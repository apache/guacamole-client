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

/**
 * Validate MessageFormat syntax and interpolation contracts for a translation.
 * Install the frontend dependencies first (npm install in src/main/frontend).
 * Usage: node check-translation-format.js ORIGINAL.json TRANSLATED.json
 *
 * This complements check-translation.py. Plural categories may differ between
 * locales; argument names/types, select cases and explicit numeric cases must
 * remain unchanged. Both languages must compile using the application's own
 * MessageFormat runtime.
 */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');
const frontendRequire = createRequire(path.resolve(__dirname,
        '../src/main/frontend/package.json'));
const MessageFormat = frontendRequire('messageformat');
const parser = frontendRequire('messageformat-parser');

function flatten(value, prefix = '', result = {}) {
    if (typeof value === 'string')
        result[prefix] = value;
    else {
        assert(value && typeof value === 'object' && !Array.isArray(value),
                `Invalid translation object: ${prefix}`);
        for (const [key, child] of Object.entries(value))
            flatten(child, prefix ? `${prefix}.${key}` : key, result);
    }
    return result;
}

function contract(tokens, result = new Set()) {
    for (const token of tokens) {
        if (typeof token === 'string')
            continue;
        if (token.arg) {
            result.add(`${token.type}:${token.arg}`);
            if (token.type === 'function')
                result.add(`function:${token.arg}:${token.key}:${token.params || ''}`);
            if ('offset' in token)
                result.add(`offset:${token.arg}:${token.offset}`);
        }
        for (const branch of token.cases || []) {
            if (token.type === 'select' || /^=?[0-9]/.test(branch.key))
                result.add(`${token.type}:${token.arg}:case:${branch.key}`);
            contract(branch.tokens, result);
        }
    }
    return [...result].sort();
}

const [originalPath, translatedPath] = process.argv.slice(2);
if (!originalPath || !translatedPath || process.argv.length !== 4) {
    console.error('Usage: node check-translation-format.js ORIGINAL.json TRANSLATED.json');
    process.exit(2);
}

const original = flatten(JSON.parse(fs.readFileSync(originalPath, 'utf8')));
const translated = flatten(JSON.parse(fs.readFileSync(translatedPath, 'utf8')));
const locale = path.basename(translatedPath, '.json');
const sourceLocale = path.basename(originalPath, '.json');
const sourceFormat = new MessageFormat(sourceLocale);
const targetFormat = new MessageFormat(locale);
const inherited = new Set(['APP.NAME', 'APP.VERSION']);
let errors = 0;
let checked = 0;
for (const key of new Set([...Object.keys(original), ...Object.keys(translated)])) {
    try {
        if (inherited.has(key) && !(key in translated))
            continue;
        assert(key in original, 'Unexpected translation key');
        assert(key in translated, 'Missing translation key');
        const source = original[key];
        const target = translated[key];
        if (source.startsWith('@:'))
            assert.equal(target, source, 'Translation reference changed');
        else {
            assert(!target.startsWith('@:'), 'Unexpected translation reference');
            sourceFormat.compile(source);
            targetFormat.compile(target);
            assert.deepEqual(contract(parser.parse(target)),
                    contract(parser.parse(source)), 'Interpolation contract changed');
        }
        checked++;
    }
    catch (error) {
        console.error(`${translatedPath}: ${key}: ${error.message}`);
        errors++;
    }
}
console.log(`${translatedPath}: ${checked} strings checked, ${errors} errors`);
process.exitCode = errors ? 1 : 0;
