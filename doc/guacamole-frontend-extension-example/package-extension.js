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

const fs = require('fs');
const path = require('path');
const {exec} = require('child_process');
const {promisify} = require('util');

// Configuration
const CONFIG = {
    sourceDir: 'dist/guacamole-frontend-extension-example',
    outputFile: 'guacamole-angular-extension.jar',
    manifestFile: 'guac-manifest.json',
    nativeFederationConfiguration: {
        bootstrapFunctionName: 'bootsrapExtension',
        pageTitle: 'Angular Extension',
        routePath: 'angular-extension'
    },
    manifestTemplate: {
        guacamoleVersion: '*',
        name: 'Guacamole Angular Example',
        namespace: 'guacamole-angular-example',
        css: [],
        html: [],
        resources: {},
        nativeFederationConfiguration: 'nativeFederationConfiguration.json'
    }
};

// MIME type mapping
const MIME_TYPES = {
    // JavaScript files
    '.js': 'application/javascript',

    // JSON files
    '.json': 'application/json',

    // Default fallback
    'default': 'application/octet-stream'
};

// Promisify fs functions for cleaner async/await usage
const readdir = promisify(fs.readdir);
const stat = promisify(fs.stat);
const writeFile = promisify(fs.writeFile);
const execAsync = promisify(exec);

/**
 * Recursively gets all files in a directory
 * @param {string} dir - Directory to scan
 * @returns {Promise<string[]>} Array of file paths relative to sourceDir
 */
async function getAllFiles(dir) {
    const files = await readdir(dir);
    const allFiles = [];

    for (const file of files) {
        const filePath = path.join(dir, file);
        const stats = await stat(filePath);

        if (stats.isDirectory()) {
            const subFiles = await getAllFiles(filePath);
            allFiles.push(...subFiles);
        } else {
            allFiles.push(filePath);
        }
    }

    return allFiles;
}

/**
 * Gets the MIME type for a file based on its extension
 * @param {string} filepath - Path to the file
 * @returns {string} MIME type
 */
function getMimeType(filepath) {
    const ext = path.extname(filepath).toLowerCase();
    return MIME_TYPES[ext] || MIME_TYPES.default;
}

/**
 * Generates the manifest file based on build artifacts
 * @param {string[]} files - Array of file paths
 * @returns {Object} Manifest object
 */
function generateManifest(files) {
    const manifest = {...CONFIG.manifestTemplate};

    // Convert absolute paths to relative paths
    const relativePaths = files.map(file =>
        path.relative(CONFIG.sourceDir, file)
    );

    // Sort files into css and resources
    relativePaths.forEach(file => {
        if (file.startsWith('html/')) {
            manifest.html.push(file);
        } else if (file.endsWith('.css')) {
            manifest.css.push(file);
        } else if (file.endsWith('.js') || file.endsWith('.json') ||
            /\.(otf|ttf|woff|woff2|eot|png|jpg|jpeg|gif|svg|ico)$/i.test(file)) {
            manifest.resources[file] = getMimeType(file);
        }
    });

    return manifest;
}

/**
 * Creates a JAR (ZIP) file containing all build artifacts and manifest
 * @param {string[]} files - Array of file paths
 * @param {Object} manifest - Manifest object
 */
async function createJarFile(files, manifest) {
    // Write manifest to the source directory
    const manifestPath = path.join(CONFIG.sourceDir, CONFIG.manifestFile);
    await writeFile(manifestPath, JSON.stringify(manifest, null, 4));

    // Write native federation configuration
    const nativeFederationConfigurationPath = path.join(CONFIG.sourceDir, CONFIG.manifestTemplate.nativeFederationConfiguration);
    await writeFile(nativeFederationConfigurationPath, JSON.stringify(CONFIG.nativeFederationConfiguration, null, 4));

    // Create zip file using zip command (available on macOS and Linux)
    const currentDir = process.cwd();
    process.chdir(CONFIG.sourceDir);

    try {
        await execAsync(`zip -r "${path.join(currentDir, CONFIG.outputFile)}" ./*`);
        console.log(`Successfully created ${CONFIG.outputFile}`);
    } catch (error) {
        console.error('Error creating zip file:', error);
        throw error;
    } finally {
        process.chdir(currentDir);
        // Clean up manifest file
        // TODO fs.unlinkSync(manifestPath);
    }
}

/**
 * Main function to orchestrate the packaging process
 */
async function main() {
    try {
        // Verify source directory exists
        if (!fs.existsSync(CONFIG.sourceDir)) {
            throw new Error(`Source directory ${CONFIG.sourceDir} does not exist`);
        }

        // Get all files in the build directory
        const files = await getAllFiles(CONFIG.sourceDir);

        // Generate manifest
        const manifest = generateManifest(files);

        // Create JAR file
        await createJarFile(files, manifest);

    } catch (error) {
        console.error('Error during packaging:', error);
        process.exit(1);
    }
}

main();