

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
import { TRANSLOCO_MISSING_HANDLER, TranslocoService, TranslocoTestingModule } from '@ngneat/transloco';
import { TestScheduler } from 'rxjs/testing';
import { GuacMissingHandler } from '../../locale/locale.module';
import { getTestScheduler } from '../../util/test-helper';
import { TranslationResult } from '../types/TranslationResult';
import { GuacTranslateService } from './guac-translate.service';


describe('GuacTranslateService', () => {
    let service: GuacTranslateService;
    let transloco: TranslocoService;
    let testScheduler: TestScheduler;

    beforeEach(() => {
        testScheduler = getTestScheduler();
        TestBed.configureTestingModule({
            imports  : [
                TranslocoTestingModule.forRoot({
                    langs          : {},
                    translocoConfig: {
                        availableLangs: ['en'],
                        defaultLang   : 'en',

                    },
                    preloadLangs   : true,
                })
            ],
            providers: [
                {
                    provide : TRANSLOCO_MISSING_HANDLER,
                    useClass: GuacMissingHandler
                }
            ]
        });
        service = TestBed.inject(GuacTranslateService);
    });

    beforeEach(() => {
        transloco = TestBed.inject(TranslocoService);
    })

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should return requested translation if available', () => {
        testScheduler.run(({ expectObservable }) => {

            const translationId = 'ID';
            const translationMessage = 'message';
            const defaultTranslationId = 'DEFAULT_ID';

            transloco.setTranslation({ [translationId]: translationMessage });

            const result = service.translateWithFallback(translationId, defaultTranslationId);

            const expected = new TranslationResult({
                id     : translationId,
                message: translationMessage
            });

            expectObservable(result).toBe('(a|)', { a: expected });
        });
    });


    it('should return translation for default id if available but requested id is not', () => {
        testScheduler.run(({ expectObservable }) => {

            const translationId = 'ID';
            const defaultTranslationId = 'DEFAULT_ID';
            const defaultTranslationMessage = 'default message';

            transloco.setTranslation({ [defaultTranslationId]: defaultTranslationMessage });

            const result = service.translateWithFallback(translationId, defaultTranslationId);

            const expected = new TranslationResult({
                id     : defaultTranslationId,
                message: defaultTranslationMessage
            });

            expectObservable(result).toBe('(a|)', { a: expected });
        });
    });

    it('should return the literal value of `defaultTranslationId` for both the ID and message if neither id could be translated', () => {
        testScheduler.run(({ expectObservable }) => {

            const translationId = 'ID';
            const defaultTranslationId = 'DEFAULT_ID';

            const result = service.translateWithFallback(translationId, defaultTranslationId);

            const expected = new TranslationResult({
                id     : defaultTranslationId,
                message: defaultTranslationId
            });

            expectObservable(result).toBe('(a|)', { a: expected });
        });
    });


});
