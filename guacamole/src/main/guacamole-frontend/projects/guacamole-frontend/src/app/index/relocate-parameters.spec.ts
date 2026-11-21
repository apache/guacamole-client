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

import { relocateParameters } from './relocate-parameters';

describe('relocateParameters', () => {

    /**
     * Calls relocateParameters() for a page with the given URL and returns
     * the URL passed to History.replaceState(), or null if the URL of the
     * page was left untouched.
     *
     * @param href
     *     The URL of the current page.
     *
     * @returns
     *     The URL the page was reformatted to, or null if it was not
     *     reformatted.
     */
    function relocate(href: string): string | null {

        const url = new URL(href);
        const history = { replaceState: vi.fn() };

        relocateParameters(url, history, 'Guacamole');

        // The history entry is replaced without state and keeps the title
        const calls = history.replaceState.mock.calls;
        if (calls.length === 0)
            return null;

        expect(calls).toHaveLength(1);
        expect(calls[0].slice(0, 2)).toEqual([null, 'Guacamole']);
        return calls[0][2];

    }

    it('leaves a URL without query parameters untouched', () => {
        expect(relocate('http://example.org/guacamole/')).toBeNull();
        expect(relocate('http://example.org/guacamole/#/settings/preferences')).toBeNull();
        expect(relocate('http://example.org/guacamole/?')).toBeNull();
    });

    it('moves query parameters behind the default path if there is no fragment', () => {
        expect(relocate('http://example.org/guacamole/?foo=bar'))
            .toBe('http://example.org/guacamole/#/?foo=bar');
        expect(relocate('http://example.org/guacamole/?foo=bar#'))
            .toBe('http://example.org/guacamole/#/?foo=bar');
    });

    it('moves query parameters behind the path within the fragment', () => {
        expect(relocate('http://example.org/guacamole/?foo=bar#/settings/preferences'))
            .toBe('http://example.org/guacamole/#/settings/preferences?foo=bar');
    });

    it('merges query parameters present both before and within the fragment', () => {
        expect(relocate('http://example.org/guacamole/?a=1&b=2#/settings?c=3'))
            .toBe('http://example.org/guacamole/#/settings?c=3&a=1&b=2');
    });

    it('keeps the context path of the page', () => {
        expect(relocate('http://example.org/remote/guac/?username=guacadmin&password=guacadmin'))
            .toBe('http://example.org/remote/guac/#/?username=guacadmin&password=guacadmin');
    });

});
