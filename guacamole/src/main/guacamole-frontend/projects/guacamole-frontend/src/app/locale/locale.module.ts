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

import { NgModule, inject, provideAppInitializer } from '@angular/core';
import {
    DefaultFallbackStrategy,
    provideTranslocoFallbackStrategy,
    provideTranslocoInterceptor,
    TRANSLOCO_CONFIG,
    TRANSLOCO_LOADER,
    TRANSLOCO_MISSING_HANDLER,
    Translation,
    TranslocoConfig,
    TranslocoInterceptor,
    TranslocoMissingHandler,
    TranslocoModule
} from '@jsverse/transloco';
import { provideTranslocoMessageformat } from '@jsverse/transloco-messageformat';
import { Observable } from 'rxjs';
import { TranslationLoaderService } from './service/translation-loader.service';
import { TranslationService } from './service/translation.service';

/**
 * Initializes the translation service.
 *
 * @returns
 *     An observable which completes when the translation service has been initialized.
 */
const initializeTranslationService = (translationService: TranslationService): Observable<void> => {
    return translationService.initialize();
};

/**
 * Provider factory which returns the TranslocoConfig.
 */
const translocoConfigFactory = (translationService: TranslationService): TranslocoConfig => {
    return translationService.getTranslocoConfig();
};

/**
 * Property name to use in a parameter object of translate() functions to specify a default value
 * that should be returned if a translation key is missing.
 */
export const VALUE_IF_MISSING_PROPERTY = 'valueIfMissing';

/**
 * Returns a parameter object which can be passed to translate() functions of Transloco as additional
 * parameter. Allows the caller to specify a default value to be returned if the requested
 * translation is not available.
 *
 * @param value
 *     The value to return if the requested translation is not available.
 */
export const translationValueIfMissing = (value: unknown): Record<string, unknown> => {
    return { [VALUE_IF_MISSING_PROPERTY]: value };
};

/**
 * Custom missing handler.
 */
export class GuacMissingHandler implements TranslocoMissingHandler {

    /**
     * Simply return the key itself if not even the fallback language contains a
     * translation for the given key.
     *
     * This behavior can be modified by providing a parameter object with a
     * property named `valueIfMissing`. If this property is not undefined,
     * the value of this property will be returned instead.
     *
     * @see VALUE_IF_MISSING_PROPERTY
     * @see translationValueIfMissing
     */
    handle(key: string, config: TranslocoConfig, params?: Record<string, unknown>) {

        if (params && params[VALUE_IF_MISSING_PROPERTY] !== undefined)
            return params[VALUE_IF_MISSING_PROPERTY];

        return key;
    }
}

/**
 * Pattern which matches a reference to another translation key, with or
 * without the closing delimiter ":@". The key is matched as a whole, so a
 * closed reference is never matched at a shorter prefix of its key.
 */
const TRANSLATION_REFERENCE = /@:([A-Za-z0-9_.]+)(?::@)?/g;

/**
 * Interceptor which rewrites references to other translation keys into the
 * interpolation syntax of Transloco ("@:KEY:@", see TranslationService).
 * Translations of extensions still use the syntax of angular-translate
 * ("@:KEY"), which Transloco would display as is. Transloco calls this
 * interceptor whenever it stores a translation, including the translations
 * which are reloaded after login.
 */
export class TranslationReferenceInterceptor implements TranslocoInterceptor {

    /**
     * Rewrites the references within all values of the given flattened
     * translation.
     *
     * @param translation
     *     The flattened translation which is about to be stored.
     *
     * @returns
     *     The translation with all references in the syntax "@:KEY:@".
     */
    preSaveTranslation(translation: Translation): Translation {
        return Object.fromEntries(Object.entries(translation)
            .map(([key, value]) => [key, this.preSaveTranslationKey(key, value)]));
    }

    /**
     * Rewrites the references within the given translation value.
     *
     * @param key
     *     The translation key of the value.
     *
     * @param value
     *     The translation value which is about to be stored.
     *
     * @returns
     *     The value with all references in the syntax "@:KEY:@".
     */
    preSaveTranslationKey(key: string, value: string): string {
        return typeof value === 'string' ? value.replace(TRANSLATION_REFERENCE, '@:$1:@') : value;
    }
}

/**
 * Module for handling common localization-related tasks.
 */
@NgModule({
    exports  : [TranslocoModule],
    providers: [
        provideAppInitializer(() => {
        const initializerFn = ((translationService: TranslationService) =>
                () => initializeTranslationService(translationService))(inject(TranslationService));
        return initializerFn();
      }),
        {
            provide   : TRANSLOCO_CONFIG,
            useFactory: translocoConfigFactory,
            deps      : [TranslationService],
        },
        {
            provide : TRANSLOCO_LOADER,
            useClass: TranslationLoaderService
        },
        {
            provide : TRANSLOCO_MISSING_HANDLER,
            useClass: GuacMissingHandler
        },
        provideTranslocoMessageformat(),

        // Required by TranslocoService; TranslocoModule does not provide them
        provideTranslocoInterceptor(TranslationReferenceInterceptor),
        provideTranslocoFallbackStrategy(DefaultFallbackStrategy)
    ]
})
export class LocaleModule {
}
