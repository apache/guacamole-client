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

import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { StyleLoaderService } from './style-loader.service';

describe('StyleLoaderService', () => {

    /**
     * The meta element which provides the build identifier, if the current
     * test adds one to the document.
     */
    let buildMeta: HTMLMetaElement | null;

    /**
     * Adds a meta element with the given content to the document head, just
     * like index.html provides the build identifier.
     *
     * @param content
     *     The content of the meta element.
     */
    function addBuildMeta(content: string): void {
        buildMeta = document.createElement('meta');
        buildMeta.name = 'build';
        buildMeta.content = content;
        document.head.appendChild(buildMeta);
    }

    /**
     * Creates the service, loads the given CSS file and returns the href
     * attribute of the resulting link element, exactly as the browser
     * requests it.
     *
     * @param styleName
     *     The name of the CSS file to load.
     *
     * @returns
     *     The href attribute of the link element that was added.
     */
    function loadStyle(styleName: string): string | null {
        TestBed.inject(StyleLoaderService).loadStyle(styleName);
        return TestBed.inject(DOCUMENT).getElementById('loaded-style-' + styleName)?.getAttribute('href') ?? null;
    }

    beforeEach(() => {
        buildMeta = null;
    });

    afterEach(() => {
        buildMeta?.remove();
        document.head.querySelectorAll('link[id^="loaded-style-"]').forEach(link => link.remove());
    });

    it('should append the build identifier as the b parameter', () => {
        addBuildMeta('20261121103211');
        expect(loadStyle('app.css')).toBe('app.css?b=20261121103211');
    });

    it('should encode the unfiltered placeholder of the build identifier', () => {
        addBuildMeta('${guacamole.build.identifier}');
        expect(loadStyle('app.css')).toBe('app.css?b=%24%7Bguacamole.build.identifier%7D');
    });

    it('should request the plain file name without a build meta element', () => {
        expect(loadStyle('app.css')).toBe('app.css');
    });

    it('should request the plain file name if the build meta element is empty', () => {
        addBuildMeta('');
        expect(loadStyle('app.css')).toBe('app.css');
    });

    it('should add the link element only once per style', () => {
        addBuildMeta('20261121103211');
        const service = TestBed.inject(StyleLoaderService);

        service.loadStyle('app.css');
        service.loadStyle('app.css');

        expect(document.head.querySelectorAll('link[id="loaded-style-app.css"]').length).toBe(1);
    });

});
