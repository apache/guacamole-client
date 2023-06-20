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

import { Injectable, isDevMode } from '@angular/core';
import { translocoConfig, TranslocoConfig } from '@ngneat/transloco';
import { LanguageService } from '../../rest/service/language.service';
import { PreferenceService } from '../../settings/services/preference.service';
import { map, Observable, tap } from 'rxjs';

/**
 * The default language to use if no preference is specified or other languages are not available.
 */
export const DEFAULT_LANGUAGE = 'en';

/**
 * Service for providing a configuration object for Transloco.
 */
@Injectable({
    providedIn: 'root'
})
export class TranslationService {

    /**
     * The preferred language to use when translating.
     * The value is provided by the PreferenceService and is set during initialization.
     */
    preferredLanguage: string = DEFAULT_LANGUAGE;

    /**
     * The list of languages available for translation.
     * The value is provided by the LanguageService and is set during initialization.
     */
    availableLanguages: string[] = [];

    /**
     * Inject required services.
     */
    constructor(private languageService: LanguageService, private preferenceService: PreferenceService) {
    }

    /**
     * Initializes the TranslationService by retrieving the list of available languages from the REST API and the user's
     * preferred language from the PreferenceService.
     *
     * Has to be called once inside an APP_INITIALIZER.
     *
     * @returns
     *     An observable which completes when the initialization is done.
     */
    initialize(): Observable<void> {
        this.preferredLanguage = this.preferenceService.preferences.language;

        return this.languageService.getLanguages()
            .pipe(
                // extract language keys from the response
                map(langauges => Object.keys(langauges)),
                // store available languages
                tap(languageKeys => this.availableLanguages = languageKeys),
                // ignore the result
                map(() => void (0))
            );
    }

    /**
     * Returns the configuration object for Transloco.
     *
     * @returns
     *     The configuration object for Transloco.
     */
    getTranslocoConfig(): TranslocoConfig {

        return translocoConfig({

            // Provide the list of all available languages retrieved from the REST API
            availableLangs: this.availableLanguages,

            // Set the default language according to the user's preferences
            defaultLang: this.preferredLanguage,

            // If the file of the preferred language could not be loaded, fall back to English
            fallbackLang: DEFAULT_LANGUAGE,

            // If a translation key is missing for the current language, fall back to English
            missingHandler: {
                // If a translation key is missing for the current language, fall back to English
                useFallbackTranslation: true,

                // Do not use the missing handler if a translation value is an empty string
                allowEmpty: true
            },

            // Allow changing language at runtime
            reRenderOnLangChange: true,
            prodMode: !isDevMode()
        });
    }
}
