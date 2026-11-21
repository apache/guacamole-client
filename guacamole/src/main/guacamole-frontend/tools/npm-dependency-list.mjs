/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Lists each npm package whose code the production build of the application
 * bundles, for the LICENSE generation of doc/licenses/generate-license-files.sh.
 * The list is written to dist/guacamole-frontend/npm-dependencies.txt, one
 * "name:version" line per package, in the same format as the list of the
 * AngularJS frontend.
 *
 * Run from the root of the Angular workspace after a production build of the
 * application, as the "build:app" script does.
 *
 * The packages come from two sources: the esbuild metafile of the application
 * build ("statsJson" in angular.json), and a rebuild of the shared bundles of
 * native federation, which builds them without a metafile. The license file
 * of the Angular build (3rdpartylicenses.txt) cannot serve instead: it names
 * no versions and covers the application bundle only.
 */

import { existsSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import * as esbuild from 'esbuild';

/**
 * The output directory of the application build.
 *
 * @type {string}
 */
const DIST_DIRECTORY = 'dist/guacamole-frontend';

/**
 * The esbuild metafile of the application build, written due to "statsJson".
 * It contains local paths and must not be deployed, so it is deleted once read.
 *
 * @type {string}
 */
const STATS_FILE = path.join(DIST_DIRECTORY, 'browser-stats.json');

/**
 * The file in which native federation lists the shared bundles.
 *
 * @type {string}
 */
const REMOTE_ENTRY_FILE = path.join(DIST_DIRECTORY, 'remoteEntry.json');

/**
 * The file receiving the list of bundled npm packages.
 *
 * @type {string}
 */
const OUTPUT_FILE = path.join(DIST_DIRECTORY, 'npm-dependencies.txt');

/**
 * The prefix of the internal chunks that native federation lists among the
 * shared bundles. Their code belongs to the shared bundles.
 *
 * @type {string}
 */
const CHUNK_PREFIX = '@nf-internal/';

/**
 * The npm packages that other packages inline within a prebuilt bundle, by
 * the package providing that bundle. esbuild sees only the prebuilt bundle,
 * so the inlined packages are added with their installed version whenever
 * the package providing the bundle is listed. Recheck on each update of one
 * of these packages.
 *
 * @type {Object.<string, string[]>}
 */
const INLINED = {

    // messageformat.js, which esbuild uses due to the "browser" field
    '@messageformat/core': [
        '@messageformat/date-skeleton',
        '@messageformat/number-skeleton',
        '@messageformat/parser',
        '@messageformat/runtime',
        'make-plural',
        'moo',
        'safe-identifier'
    ],

    // dist/pickr.min.js, see "sources" of dist/pickr.min.js.map
    '@simonwep/pickr': [
        'nanopop'
    ],

    // fesm2022/@softarc/native-federation-orchestrator.mjs, see "sources" of its source map
    '@softarc/native-federation-orchestrator': [
        '@softarc/native-federation',
        'semver'
    ]

};

/**
 * The direct dependencies of the application whose code is not bundled,
 * each with the reason. Any other direct dependency missing from the list is
 * an error.
 *
 * @type {Set<string>}
 */
const NOT_BUNDLED = new Set([

    // Compiler for JIT compilation, used by the unit tests only; the
    // application is compiled ahead of time
    '@angular/compiler'

]);

/**
 * The esbuild options for rebuilding the shared bundles. Packages resolve as
 * in the production build of shared bundles by native federation (see
 * src/tools/esbuild/node-modules-bundler.js and script-options.js of
 * @angular-architects/native-federation).
 *
 * @type {Object}
 */
const SHARED_BUNDLE_OPTIONS = {
    bundle: true,
    write: false,
    metafile: true,
    logLevel: 'error',

    // Required for multiple entry points, though nothing is written
    outdir: DIST_DIRECTORY,

    platform: 'browser',
    format: 'esm',
    mainFields: [ 'es2020', 'es2015', 'browser', 'module', 'main' ],
    conditions: [ 'es2015', 'es2020', 'module', 'production' ],
    resolveExtensions: [ '.ts', '.mjs', '.js', '.cjs' ]
};

/**
 * Reads and parses the given JSON file.
 *
 * @param {string} file
 *     The path of the JSON file.
 *
 * @returns {*}
 *     The parsed contents of the file.
 */
function readJson(file) {
    return JSON.parse(readFileSync(file, 'utf8'));
}

/**
 * Returns the coordinates ("name:version") of the npm package containing the
 * given file, or null if the file is not part of an npm package. As in the
 * license extraction of the Angular build, the innermost "node_modules"
 * directory of the path determines the package.
 *
 * @param {string} file
 *     The path of the file, relative to the workspace root.
 *
 * @returns {string|null}
 *     The coordinates of the npm package containing the file, or null.
 */
function packageOf(file) {

    const segments = file.split(/[\\/]/);
    const index = segments.lastIndexOf('node_modules');
    if (index < 0 || index + 1 >= segments.length)
        return null;

    const depth = segments[index + 1].startsWith('@') ? 2 : 1;
    const directory = path.join(...segments.slice(0, index + 1 + depth));
    const { name, version } = readJson(path.join(directory, 'package.json'));
    return `${name}:${version}`;

}

/**
 * Returns the name of the npm package identified by the given coordinates.
 *
 * @param {string} coordinate
 *     The coordinates of the package ("name:version").
 *
 * @returns {string}
 *     The name of the package.
 */
function nameOf(coordinate) {
    return coordinate.slice(0, coordinate.lastIndexOf(':'));
}

/**
 * Adds the coordinates of each npm package that contributes code to any
 * output of the given esbuild metafile.
 *
 * @param {Object} metafile
 *     The esbuild metafile to read.
 *
 * @param {Set<string>} coordinates
 *     The set receiving the coordinates ("name:version") of each package.
 */
function addPackages(metafile, coordinates) {
    for (const output of Object.values(metafile.outputs)) {
        for (const [ input, { bytesInOutput } ] of Object.entries(output.inputs)) {
            const coordinate = bytesInOutput > 0 && packageOf(input);
            if (coordinate)
                coordinates.add(coordinate);
        }
    }
}

/**
 * Returns the entry point of each shared bundle listed in the given
 * remoteEntry.json, including shared mappings, excluding internal chunks.
 *
 * @param {Object} remoteEntry
 *     The parsed contents of remoteEntry.json.
 *
 * @returns {string[]}
 *     The import specifier of each shared bundle.
 */
function sharedEntryPoints(remoteEntry) {
    return remoteEntry.shared

        // With the "denseExternals" feature, one entry lists all entry
        // points of a package
        .flatMap(entry => entry.entries ? Object.keys(entry.entries) : [ entry.packageName ])

        .filter(entryPoint => !entryPoint.startsWith(CHUNK_PREFIX));
}

if (!existsSync(STATS_FILE))
    throw new Error(`${STATS_FILE} is missing. Build the application with the production configuration first.`);

const coordinates = new Set();

// Application bundle, without the shared bundles, which it imports as externals
addPackages(readJson(STATS_FILE), coordinates);
rmSync(STATS_FILE);

// Shared bundles, rebuilt like native federation builds them: in one build
// with each shared bundle as entry point and as external, so that each
// contains the code of all packages it imports except other shared bundles
const entryPoints = sharedEntryPoints(readJson(REMOTE_ENTRY_FILE));
const { metafile } = await esbuild.build({
    ...SHARED_BUNDLE_OPTIONS,
    entryPoints,
    external: entryPoints
});
addPackages(metafile, coordinates);

// Packages inlined within prebuilt bundles
for (const [ bundle, inlined ] of Object.entries(INLINED)) {
    if ([ ...coordinates ].some(coordinate => nameOf(coordinate) === bundle)) {
        for (const name of inlined)
            coordinates.add(packageOf(path.join('node_modules', name, 'package.json')));
    }
}

// Each direct dependency must be bundled or excepted with a reason
const names = new Set([ ...coordinates ].map(nameOf));
const missing = Object.keys(readJson('package.json').dependencies ?? {})
    .filter(name => !names.has(name) && !NOT_BUNDLED.has(name));

if (missing.length)
    throw new Error('Direct dependencies not found in any bundle: ' + missing.join(', ')
        + '. Add each to NOT_BUNDLED in tools/npm-dependency-list.mjs with the reason, or check the build.');

writeFileSync(OUTPUT_FILE, [ ...coordinates ].sort().join('\n') + '\n');
console.log(`Listed ${coordinates.size} bundled npm packages in ${OUTPUT_FILE}.`);
