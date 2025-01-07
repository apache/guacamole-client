

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

import { APP_INITIALIZER, NgModule } from '@angular/core';
import {
    TRANSLOCO_CONFIG,
    TRANSLOCO_LOADER,
    TRANSLOCO_MISSING_HANDLER,
    TranslocoConfig,
    TranslocoMissingHandler,
    TranslocoModule
} from '@ngneat/transloco';
import { TranslocoMessageFormatModule } from '@ngneat/transloco-messageformat';
import { HashMap } from '@ngneat/transloco/lib/types';
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
 * Returns a HashMap which can be passed to translate() functions of Transloco as additional
 * parameter. Allows the caller to specify a default value to be returned if the requested
 * translation is not available.
 *
 * @param value
 *     The value to return if the requested translation is not available.
 */
export const translationValueIfMissing = (value: any): HashMap => {
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
    handle(key: string, config: TranslocoConfig, params?: HashMap) {

        if (params && params[VALUE_IF_MISSING_PROPERTY] !== undefined)
            return params[VALUE_IF_MISSING_PROPERTY];

        return key;
    }
}

/**
 * Module for handling common localization-related tasks.
 */
@NgModule({
    exports  : [TranslocoModule],
    providers: [
        {
            provide   : APP_INITIALIZER,
            useFactory: (translationService: TranslationService) =>
                () => initializeTranslationService(translationService),
            deps      : [TranslationService],
            multi     : true,
        },
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
        }
    ],
    imports  : [
        TranslocoMessageFormatModule.forRoot()
    ]
})
export class LocaleModule {
}
