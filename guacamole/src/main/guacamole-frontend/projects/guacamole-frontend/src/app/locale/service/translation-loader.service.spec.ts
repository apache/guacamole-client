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

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { LanguageService } from '../../rest/service/language.service';
import { TranslationLoaderService } from './translation-loader.service';

describe('TranslationLoaderService', () => {
    let httpTestingController: HttpTestingController;

    /**
     * The meta element which provides the build identifier, if the current
     * test adds one to the document.
     */
    let buildMeta: HTMLMetaElement | null;

    /**
     * Adds a meta element with the given content to the document, just like
     * index.html provides the build identifier.
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
     * Creates the loader and requests the translation of the given language,
     * which the mocked REST API lists as supported.
     *
     * @param languageKey
     *     The language key to request.
     *
     * @returns
     *     The promise of the loader.
     */
    function loadTranslation(languageKey: string): Promise<unknown> {
        return TestBed.inject(TranslationLoaderService).getTranslation(languageKey);
    }

    beforeEach(() => {
        buildMeta = null;

        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: LanguageService, useValue: { getLanguages: () => of({ en: 'English', de: 'Deutsch' }) } }
            ]
        });

        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        buildMeta?.remove();
        httpTestingController.verify();
    });

    it('should append the build identifier to the URL of the translation file', async () => {
        addBuildMeta('20261120234823');

        const translation = loadTranslation('en');
        httpTestingController.expectOne('translations/en.json?b=20261120234823').flush({ APP: 'Application' });

        await expect(translation).resolves.toEqual({ APP: 'Application' });
    });

    it('should encode the build identifier within the URL of the translation file', () => {
        addBuildMeta('a b&c');

        loadTranslation('en');

        httpTestingController.expectOne('translations/en.json?b=a%20b%26c').flush({});
    });

    it('should not append a build identifier if the document has no build meta element', async () => {
        const translation = loadTranslation('en');
        httpTestingController.expectOne('translations/en.json').flush({ APP: 'Application' });

        await expect(translation).resolves.toEqual({ APP: 'Application' });
    });

    it('should not append a build identifier if the build meta element is empty', () => {
        addBuildMeta('');

        loadTranslation('en');

        httpTestingController.expectOne('translations/en.json').flush({});
    });

    it('should append the build identifier to the file of the language without country code', async () => {
        addBuildMeta('20261120234823');

        const translation = loadTranslation('de_DE');
        httpTestingController.expectOne('translations/de.json?b=20261120234823').flush({ APP: 'Anwendung' });

        await expect(translation).resolves.toEqual({ APP: 'Anwendung' });
    });
});
