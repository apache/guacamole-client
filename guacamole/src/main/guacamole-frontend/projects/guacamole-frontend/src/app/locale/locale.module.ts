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

import { Injectable, Injector, NgModule, inject, provideAppInitializer } from '@angular/core';
import {
    DefaultFallbackStrategy,
    getValue,
    provideTranslocoFallbackStrategy,
    provideTranslocoInterceptor,
    provideTranslocoTranspiler,
    TranspileParams,
    TRANSLOCO_CONFIG,
    TRANSLOCO_LOADER,
    TRANSLOCO_MISSING_HANDLER,
    Translation,
    TranslocoConfig,
    TranslocoInterceptor,
    TranslocoMissingHandler,
    TranslocoModule,
    TranslocoService
} from '@jsverse/transloco';
import { MessageFormatTranspiler, TRANSLOCO_MESSAGE_FORMAT_CONFIG } from '@jsverse/transloco-messageformat';
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
 * Pattern which matches a brace or backslash escaped with a backslash ("\{",
 * "\}", "\\"), as messageformat 1 of angular-translate reads it.
 * @messageformat/core has no such escapes, but displays a brace as is unless
 * it starts an argument ("{NAME}") or closes a plural or select case. Quoting
 * with apostrophes is no alternative: the browser build of
 * @messageformat/core 3.4.0 embeds the lexer of @messageformat/parser 5.1.0,
 * whose quotes run on to a later apostrophe ("'{' x '}'" displays "{' x }'").
 */
const BACKSLASH_ESCAPE = /\\([\\{}])/g;

/**
 * Interceptor which rewrites references to other translation keys into the
 * interpolation syntax of Transloco ("@:KEY:@", see TranslationService).
 * Translations of extensions still use the syntax of angular-translate
 * ("@:KEY"), which Transloco would display as is. Translations also escape
 * braces with a backslash for messageformat 1 of angular-translate; the
 * interceptor removes these backslashes, which @messageformat/core would
 * display. Transloco calls this interceptor whenever it stores a
 * translation, including the translations which are reloaded after login.
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
     * Rewrites the references within the given translation value and removes
     * the backslashes of its escaped braces and backslashes.
     *
     * @param key
     *     The translation key of the value.
     *
     * @param value
     *     The translation value which is about to be stored.
     *
     * @returns
     *     The value with all references in the syntax "@:KEY:@" and without
     *     escaping backslashes.
     */
    preSaveTranslationKey(key: string, value: string): string {
        return typeof value === 'string'
            ? value.replace(TRANSLATION_REFERENCE, '@:$1:@').replace(BACKSLASH_ESCAPE, '$1')
            : value;
    }
}

/**
 * A locale of MessageFormat, as accepted by MessageFormatTranspiler.setLocale().
 */
type MessageFormatLocale = Parameters<MessageFormatTranspiler['setLocale']>[0];

/**
 * Transpiler which uses the fallback language like angular-translate. A
 * reference to a key which the translation lacks displays the text of the
 * fallback language, and values of the fallback language are formatted with
 * the locale of the fallback language. Transloco itself replaces such a
 * reference with an empty string, and it formats every value with the locale
 * of the active language, whose plural rules may reject or misplace the
 * plural cases of English ("one" is no plural case in Japanese).
 */
@Injectable()
export class FallbackMessageFormatTranspiler extends MessageFormatTranspiler {

    /**
     * Injector providing the TranslocoService, which depends on this
     * transpiler and thus cannot be injected into it directly.
     */
    private readonly injector = inject(Injector);

    /**
     * The locale which currently formats values, as last passed to
     * setLocale(). Null stands for the default locale of MessageFormat.
     */
    private locale: MessageFormatLocale = null;

    /**
     * Creates the transpiler with the locale of the language which Transloco
     * starts with. Transloco sets the locale of the transpiler only when the
     * active language changes (setActiveLang()), and not for its start
     * language (defaultLang), while angular-translate sets the locale of the
     * language it starts with, too.
     */
    constructor() {
        super(inject(TRANSLOCO_MESSAGE_FORMAT_CONFIG, { optional: true }) ?? {});
        this.setLocale(this.config.defaultLang);
    }

    /**
     * Sets the locale which formats values and remembers it, so that
     * transpile() can restore it after formatting a value of the fallback
     * language. A language key with region ("pt_BR") formats values with the
     * rules of its language ("pt"), like messageformat 1 of angular-translate,
     * whose locales separate a region with a hyphen ("pt-PT"). Given the key
     * itself, @messageformat/core would fail in Intl.NumberFormat, which
     * rejects the underscore.
     *
     * @param locale
     *     The locale or language key which shall format values.
     */
    override setLocale(locale: MessageFormatLocale): void {
        super.setLocale(typeof locale === 'string' ? locale.split('_')[0] : locale);
        this.locale = locale;
    }

    /**
     * Transpiles the given value like MessageFormatTranspiler, using the
     * fallback language like angular-translate. References to keys which the
     * translation lacks are replaced with the transpiled value of the
     * fallback language, or with the key itself if the fallback language
     * lacks it, too, as the translate directive of angular-translate displays
     * it. Values of the fallback language are formatted with its locale.
     *
     * @param params
     *     The value to transpile, its parameters, the translation of the
     *     value and its key.
     *
     * @returns
     *     The transpiled value.
     */
    override transpile(params: TranspileParams): unknown {

        // Transloco takes missing keys from the first fallback language
        const fallbackLang = [this.config.fallbackLang ?? []].flat()[0];
        const transloco = this.injector.get(TranslocoService);
        const fallback = transloco.getTranslation(fallbackLang);
        const { translation } = params;

        let value = params.value;
        if (typeof value === 'string') {
            value = value.replace(this.interpolationMatcher, (reference: string, name: string) => {

                // Parameters and keys of the translation take precedence, as in DefaultTranspiler
                const key = name.trim();
                if (getValue(params.params ?? {}, key) !== undefined || translation[key] !== undefined)
                    return reference;

                return fallback[key] !== undefined
                    ? String(this.transpile({ value: fallback[key], params: params.params, translation: fallback, key }))
                    : key;

            });
        }

        // Format values of the fallback language with its locale, unless the
        // fallback language is the active language anyway
        if (translation !== fallback || transloco.getActiveLang() === fallbackLang)
            return super.transpile({ ...params, value });

        const locale = this.locale;
        this.setLocale(fallbackLang);
        try {
            return super.transpile({ ...params, value });
        }
        finally {
            this.setLocale(locale);
        }

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
        provideTranslocoTranspiler(FallbackMessageFormatTranspiler),

        // Required by TranslocoService; TranslocoModule does not provide them
        provideTranslocoInterceptor(TranslationReferenceInterceptor),
        provideTranslocoFallbackStrategy(DefaultFallbackStrategy)
    ]
})
export class LocaleModule {
}
