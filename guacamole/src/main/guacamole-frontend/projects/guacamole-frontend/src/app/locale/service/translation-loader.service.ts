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

import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Translation, TranslocoLoader } from '@ngneat/transloco';
import { TranslocoLoaderData } from '@ngneat/transloco/lib/transloco.loader';
import { LanguageService } from '../../rest/service/language.service';
import { SKIP_ALL_INTERCEPTORS } from '../../util/interceptor.service';

/**
 * Service for loading translation definition files.
 */
@Injectable({
    providedIn: 'root'
})
export class TranslationLoaderService implements TranslocoLoader {

    /**
     * Inject required services.
     */
    constructor(private languageService: LanguageService, private http: HttpClient) {
    }

    /**
     * Custom loader function for Transloco which loads the desired
     * language file dynamically via HTTP. If the language file cannot be
     * found, an empty translation object is returned.
     *
     * @param languageKey
     *     The requested language key.
     *
     * @param data
     *     Optional data to be passed to the loader.
     *
     * @returns
     *     A promise which resolves to the requested translation object.
     */
    getTranslation(languageKey: string, data?: TranslocoLoaderData): Promise<Translation> {

        // Return promise which is resolved only after the translation file is loaded
        return new Promise<Translation>((resolve, reject) => {

            // Satisfy the translation request using possible variations of the given key
            this.satisfyTranslation(resolve, reject, languageKey, this.getKeyVariations(languageKey));

        });

    }

    /**
     * Satisfies a translation request for the given key by searching for the
     * translation files for each key in the given array, in order. The request
     * fails only if none of the files can be found.
     *
     * @param resolve
     *     The function to call if at least one translation file can be
     *     successfully loaded.
     *
     * @param reject
     *     The function to call if none of the translation files can be
     *     successfully loaded.
     *
     * @param requestedKey
     *     The originally-requested language key.
     *
     * @param remainingKeys
     *     The keys of the languages to attempt to load, in order, where the
     *     first key in this array is the language to try within this function
     *     call. The first key in the array is not necessarily the originally-
     *     requested language key.
     */
    satisfyTranslation(resolve: (value: Translation) => void, reject: () => void, requestedKey: string, remainingKeys: string[]): void {

        // Get current language key
        const currentKey = remainingKeys.shift();

        // If no languages to try, "succeed" with an empty translation (force fallback)
        if (!currentKey) {
            resolve({});
            return;
        }

        /**
         * Continues trying possible translation files until no possibilities
         * exist.
         *
         * @private
         */
        const tryNextTranslation = () => {
            this.satisfyTranslation(resolve, reject, requestedKey, remainingKeys);
        };

        // Retrieve list of supported languages
        this.languageService.getLanguages()
            // Attempt to retrieve translation if language is supported
            .subscribe({
                next: (languages) => {

                    // Skip retrieval if language is not supported
                    if (!(currentKey in languages)) {
                        tryNextTranslation();
                        return;
                    }

                    // Attempt to retrieve language
                    //     TODO: cache: cacheService.languages,
                    const httpContext: HttpContext = new HttpContext();
                    httpContext.set(SKIP_ALL_INTERCEPTORS, true);
                    this.http.get<Translation>(
                        'translations/' + encodeURIComponent(currentKey) + '.json',
                        { context: httpContext }
                    ).subscribe({
                        next: (translationData) => {
                            resolve(translationData);
                        },
                        // Retry with remaining languages if translation file could not be
                        // retrieved
                        error: tryNextTranslation
                    });

                },

                // Retry with remaining languages if translation does not exist
                error: tryNextTranslation
            });

    }

    /**
     * Given a valid language key, returns all possible legal variations of
     * that key. Currently, this will be the given key and the given key
     * without the country code. If the key has no country code, only the
     * given key will be included in the returned array.
     *
     * @param key
     *     The language key to generate variations of.
     *
     * @returns
     *     All possible variations of the given language key.
     */
    getKeyVariations(key: string): string[] {

        const underscore = key.indexOf('_');

        // If no underscore, only one possibility
        if (underscore === -1)
            return [key];

        // Otherwise, include the lack of country code as an option
        return [key, key.substring(0, underscore)];

    }


}
