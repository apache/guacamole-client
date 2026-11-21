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

import { TestBed } from '@angular/core/testing';
import { provideTranslocoInterceptor, TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { provideTranslocoMessageformat } from '@jsverse/transloco-messageformat';
import { TranslationReferenceInterceptor } from './locale.module';

describe('TranslationReferenceInterceptor', () => {
    let transloco: TranslocoService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports  : [
                TranslocoTestingModule.forRoot({
                    langs          : {},
                    translocoConfig: {
                        availableLangs: ['en'],
                        defaultLang   : 'en',

                        // Interpolation of the application, see TranslationService
                        interpolation: ['@:', ':@']
                    }
                })
            ],
            providers: [
                provideTranslocoMessageformat(),
                provideTranslocoInterceptor(TranslationReferenceInterceptor)
            ]
        });
        transloco = TestBed.inject(TranslocoService);
    });

    /**
     * Stores the given translation for English, as Transloco does once the
     * translation file has been loaded.
     *
     * @param translation
     *     The translation to store, nested like a translation file.
     */
    function store(translation: Record<string, unknown>): void {
        transloco.setTranslation(translation, 'en');
    }

    it('should resolve a reference without closing delimiter', () => {
        store({
            APP  : { ERROR_PASSWORD_MISMATCH: 'The provided passwords do not match.' },
            LOGIN: { ERROR_PASSWORD_MISMATCH: '@:APP.ERROR_PASSWORD_MISMATCH' }
        });

        expect(transloco.translate('LOGIN.ERROR_PASSWORD_MISMATCH')).toBe('The provided passwords do not match.');
    });

    it('should resolve a reference with closing delimiter', () => {
        store({
            APP  : { NAME: 'Apache Guacamole' },
            LOGIN: { NAME: '@:APP.NAME:@' }
        });

        expect(transloco.translate('LOGIN.NAME')).toBe('Apache Guacamole');
    });

    it('should resolve several references within one text', () => {
        store({
            APP  : { NAME: 'Apache Guacamole', VERSION: '1.6.1' },
            LOGIN: { TITLE: '@:APP.NAME (@:APP.VERSION:@)' }
        });

        expect(transloco.translate('LOGIN.TITLE')).toBe('Apache Guacamole (1.6.1)');
    });

    it('should leave an ICU message without reference unchanged', () => {
        store({
            APP: { TEXT_DURATION: '{VALUE, plural, one{# second} other{# seconds}}' }
        });

        expect(transloco.translate('APP.TEXT_DURATION', { VALUE: 1 })).toBe('1 second');
        expect(transloco.translate('APP.TEXT_DURATION', { VALUE: 2 })).toBe('2 seconds');
    });
});
