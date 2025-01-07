

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

import { Injectable } from '@angular/core';
import { TranslocoService } from '@ngneat/transloco';
import { Observable, of, switchMap, take } from 'rxjs';
import { TranslationResult } from '../types/TranslationResult';
import { HashMap } from '@ngneat/transloco/lib/types';
import { translationValueIfMissing } from '../../locale/locale.module';

/**
 * A wrapper around the angular-translate $translate service that offers a
 * convenient way to fall back to a default translation if the requested
 * translation is not available.
 */
@Injectable({
    providedIn: 'root'
})
export class GuacTranslateService {

    /**
     * Inject required services.
     */
    constructor(private translocoService: TranslocoService) {
    }

    /**
     * Returns an observable that will emit a TranslationResult containing either the
     * requested ID and message (if translated), or the default ID and message if translated,
     * or the literal value of `defaultTranslationId` for both the ID and message if neither
     * is translated.
     *
     * @param translationId
     *     The requested translation ID, which may or may not be translated.
     *
     * @param defaultTranslationId
     *     The translation ID that will be used if no translation is found for `translationId`.
     *
     * @returns
     *     An observable which emits a TranslationResult containing the results from
     *     the translation attempt.
     */
    translateWithFallback(translationId: string, defaultTranslationId: string): Observable<TranslationResult> {

        // Use null as a fallback value to detect missing translations more easily
        const params: HashMap = translationValueIfMissing(null);

        // Attempt to translate the requested translation ID
        return this.translocoService.selectTranslate<string | null>(translationId, params)
            .pipe(
                take(1),
                switchMap((translation) => {

                    // If the requested translation is available, use that
                    if (translation !== null)
                        return of(new TranslationResult({ id: translationId, message: translation }));

                    // Otherwise, try the default translation ID
                    return this.translocoService.selectTranslate<string | null>(defaultTranslationId, params)
                        .pipe(
                            take(1),
                            switchMap((defaultTranslation) => {

                                // Default translation worked, so use that
                                if (defaultTranslation !== null)
                                    return of(new TranslationResult({
                                        id     : defaultTranslationId,
                                        message: defaultTranslation
                                    }));

                                // Neither translation is available; as a fallback, return default ID for both
                                return of(new TranslationResult({
                                    id     : defaultTranslationId,
                                    message: defaultTranslationId
                                }));
                            })
                        )
                })
            );

    }
}
