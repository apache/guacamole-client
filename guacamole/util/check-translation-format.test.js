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

// Run with: node --test guacamole/util/check-translation-format.test.js
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { test } = require('node:test');

function check(original, translated) {
    const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'guac-translation-'));
    try {
        const source = path.join(directory, 'en.json');
        const target = path.join(directory, 'sv.json');
        fs.writeFileSync(source, JSON.stringify(original));
        fs.writeFileSync(target, JSON.stringify(translated));
        return spawnSync(process.execPath, [
            path.join(__dirname, 'check-translation-format.js'), source, target
        ], { encoding: 'utf8' });
    }
    finally {
        fs.rmSync(directory, { recursive: true, force: true });
    }
}

const source = '{COUNT, plural, one{One file for {USER}} other{# files for {USER}}}';
const translated = '{COUNT, plural, one{En fil för {USER}} other{# filer för {USER}}}';

test('accepts Swedish plural syntax and inherited application metadata', () => {
    const result = check({ APP: { NAME: 'Guacamole', VERSION: '${project.version}' }, TEXT: source }, { TEXT: translated });
    assert.equal(result.status, 0, result.stderr);
});

for (const [name, original, target, message] of [
    ['missing argument', { TEXT: source }, { TEXT: '{COUNT, plural, one{En fil} other{# filer}}' }, 'Interpolation contract changed'],
    ['renamed argument', { TEXT: 'Hello {USER}' }, { TEXT: 'Hej {NAME}' }, 'Interpolation contract changed'],
    ['changed argument type', { TEXT: '{COUNT}' }, { TEXT: '{COUNT, plural, one{En} other{Flera}}' }, 'Interpolation contract changed'],
    ['changed select case', { TEXT: '{UNIT, select, b{B} kb{KB} other{}}' }, { TEXT: '{UNIT, select, b{B} mb{MB} other{}}' }, 'Interpolation contract changed'],
    ['changed explicit plural case', { TEXT: '{N, plural, =0{None} one{One} other{Many}}' }, { TEXT: '{N, plural, =2{Två} one{En} other{Flera}}' }, 'Interpolation contract changed'],
    ['changed plural offset', { TEXT: '{N, plural, offset:1 one{One} other{Many}}' }, { TEXT: '{N, plural, offset:2 one{En} other{Flera}}' }, 'Interpolation contract changed'],
    ['changed reference', { TEXT: '@:APP.ACTION_SAVE' }, { TEXT: '@:APP.ACTION_CANCEL' }, 'Translation reference changed'],
    ['missing key', { TEXT: source }, {}, 'Missing translation key'],
    ['unexpected key', {}, { TEXT: translated }, 'Unexpected translation key']
]) {
    test('rejects ' + name, () => {
        const result = check(original, target);
        assert.equal(result.status, 1);
        assert(result.stderr.includes(message), result.stderr);
    });
}

test('rejects malformed MessageFormat', () => {
    const result = check({ TEXT: source }, { TEXT: '{COUNT, plural, one{En}' });
    assert.equal(result.status, 1);
    assert(result.stderr.includes('TEXT'), result.stderr);
});
