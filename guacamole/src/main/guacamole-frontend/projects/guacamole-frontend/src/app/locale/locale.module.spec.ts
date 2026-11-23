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
import {
    provideTranslocoInterceptor,
    Translation,
    TRANSLOCO_LOADER,
    TranslocoService,
    TranslocoTestingModule
} from '@jsverse/transloco';
import { provideTranslocoMessageformat } from '@jsverse/transloco-messageformat';
import { firstValueFrom, of } from 'rxjs';
import cs from '../../translations/cs.json';
import de from '../../translations/de.json';
import en from '../../translations/en.json';
import ja from '../../translations/ja.json';
import ru from '../../translations/ru.json';
import { LanguageService } from '../rest/service/language.service';
import { PreferenceService } from '../settings/services/preference.service';
import { LocaleModule, TranslationReferenceInterceptor } from './locale.module';

describe('TranslationReferenceInterceptor', () => {
    let transloco: TranslocoService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports  : [
                TranslocoTestingModule.forRoot({
                    langs          : {},
                    translocoConfig: {
                        availableLangs: ['en', 'de'],
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

    describe('with the translation files of the application', () => {

        beforeEach(() => {
            transloco.setTranslation(en, 'en');
            transloco.setTranslation(de, 'de');
        });

        it.each(['en', 'de'])('should display braces escaped with a backslash without the backslash (%s)', lang => {
            const example = JSON.parse(transloco.translate('IMPORT.HELP_JSON_EXAMPLE', {}, lang));

            expect(example[0].parameters).toEqual({ username: 'alice', password: 'pass1', hostname: 'conn1.web.com' });
            expect(example[1].attributes).toEqual({ 'guacd-encryption': 'none' });
        });

        it('should display a backslash escaped with a backslash once', () => {
            expect(transloco.translate('IMPORT.HELP_SEMICOLON_FOOTNOTE', {}, 'en'))
                .toBe('If present, semicolons can be escaped with a backslash, e.g. "first\\;last"');
            expect(transloco.translate('IMPORT.HELP_SEMICOLON_FOOTNOTE', {}, 'de'))
                .toBe('Falls vorhanden, können Semikolons mit einem Backslash maskiert werden, z.B. "first\\;last"');
        });

        it.each([{ lang: 'en', file: en }, { lang: 'de', file: de }])('should store all values without backslash escapes unchanged ($lang)', ({ lang, file }) => {
            const changed = Object.entries(transloco.getTranslation(lang))
                .filter(([key, value]) => key.split('.')
                    .reduce<unknown>((node, name) => (node as Record<string, unknown>)[name], file) !== value)
                .map(([key]) => key);

            expect(changed).toEqual(['IMPORT.HELP_JSON_EXAMPLE', 'IMPORT.HELP_SEMICOLON_FOOTNOTE']);
        });

        it('should display ICU messages in English as before', () => {
            expect(transloco.translate('SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION', { VALUE: 1, UNIT: 'second' })).toBe('1 second');
            expect(transloco.translate('SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION', { VALUE: 2, UNIT: 'hour' })).toBe('2 hours');
            expect(transloco.translate('IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS', { NUMBER: 1 })).toBe('1 connection imported successfully.');
            expect(transloco.translate('IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS', { NUMBER: 3 })).toBe('3 connections imported successfully.');
            expect(transloco.translate('CLIENT.TEXT_RECONNECT_COUNTDOWN', { REMAINING: 1 })).toBe('Reconnecting in 1 second...');
            expect(transloco.translate('CLIENT.INFO_USER_COUNT', { USERNAME: 'guacadmin', COUNT: 2 })).toBe('guacadmin (2)');
            expect(transloco.translate('CLIENT.TEXT_FILE_TRANSFER_PROGRESS', { PROGRESS: '1.5', UNIT: 'mb' })).toBe('1.5 MB');
            expect(transloco.translate('IMPORT.TABLE_HEADER_ROW_NUMBER')).toBe('Row #');
        });

        it('should display ICU messages in German as before', () => {
            transloco.setActiveLang('de');

            expect(transloco.translate('SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION', { VALUE: 1, UNIT: 'minute' })).toBe('1 Minute');
            expect(transloco.translate('SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION', { VALUE: 3, UNIT: 'day' })).toBe('3 Tage');
            expect(transloco.translate('IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS', { NUMBER: 2 })).toBe('2 connections erfolgreich importiert.');
            expect(transloco.translate('CLIENT.TEXT_RECONNECT_COUNTDOWN', { REMAINING: 5 })).toBe('Neuverbindung in 5 Sekunden...');
            expect(transloco.translate('CLIENT.INFO_USER_COUNT', { USERNAME: 'guacadmin', COUNT: 1 })).toBe('guacadmin');
            expect(transloco.translate('IMPORT.TABLE_HEADER_ROW_NUMBER')).toBe('Zeile #');
        });
    });
});

describe('LocaleModule', () => {
    let transloco: TranslocoService;

    /**
     * The translation files which the mocked loader returns, by language key,
     * as the backend serves them in translations/<language>.json.
     */
    let files: Record<string, Translation>;

    /**
     * Creates the TranslocoService with the providers of LocaleModule. It
     * starts with the language of the preferences, as on loading the page.
     *
     * @param language
     *     The key of the language which the preferences contain.
     */
    function start(language: string): void {
        TestBed.configureTestingModule({
            imports  : [LocaleModule],
            providers: [
                {
                    provide : LanguageService,
                    useValue: { getLanguages: () => of({ en: 'English', ja: '日本語', ru: 'Русский' }) }
                },
                { provide: PreferenceService, useValue: { preferences: { language } } },
                { provide: TRANSLOCO_LOADER, useValue: { getTranslation: (lang: string) => of(files[lang]) } }
            ]
        });
        transloco = TestBed.inject(TranslocoService);
    }

    beforeEach(() => {
        files = { en, ja, ru };
        start('en');
    });

    /**
     * Makes the given language the active language, as the preferences do,
     * and loads it, as Transloco does for the active language.
     *
     * @param lang
     *     The key of the language to activate.
     */
    async function activate(lang: string): Promise<void> {
        transloco.setActiveLang(lang);
        await firstValueFrom(transloco.load(lang));
    }

    describe('with a reference to a key which the active language lacks', () => {

        it('should display the English text of the acknowledge buttons in Japanese', async () => {
            await activate('ja');

            for (const section of ['CLIENT', 'LOGIN', 'MANAGE_CONNECTION', 'MANAGE_CONNECTION_GROUP',
                'MANAGE_SHARING_PROFILE', 'MANAGE_USER', 'MANAGE_USER_GROUP', 'SETTINGS_CONNECTIONS',
                'SETTINGS_PREFERENCES', 'SETTINGS_USERS', 'SETTINGS_USER_GROUPS', 'SETTINGS_SESSIONS'])
                expect(transloco.translate(`${section}.ACTION_ACKNOWLEDGE`), section).toBe('OK');
        });

        it('should display the English date formats in Japanese', async () => {
            await activate('ja');

            for (const key of ['MANAGE_CONNECTION.FORMAT_HISTORY_START', 'SETTINGS_CONNECTION_HISTORY.FORMAT_DATE',
                'SETTINGS_USERS.FORMAT_DATE', 'SETTINGS_USER_GROUPS.FORMAT_DATE', 'SETTINGS_SESSIONS.FORMAT_STARTDATE'])
                expect(transloco.translate(key), key).toBe('yyyy-MM-dd HH:mm:ss');
        });

        it.each(['MANAGE_CONNECTION', 'SETTINGS_CONNECTION_HISTORY'])('should display English durations with the plural rules of English in Japanese (%s)', async section => {
            await activate('ja');

            const duration = (VALUE: number, UNIT: string) => transloco.translate(`${section}.TEXT_HISTORY_DURATION`, { VALUE, UNIT });
            expect(duration(1, 'second')).toBe('1 second');
            expect(duration(2, 'minute')).toBe('2 minutes');
            expect(duration(0, 'hour')).toBe('0 hours');
            expect(duration(5, 'day')).toBe('5 days');
            expect(duration(1, 'other')).toBe('1 ');
        });

        it('should display the English titles of the player buttons in Russian', async () => {
            await activate('ru');

            expect(transloco.translate('PLAYER.ACTION_PLAY')).toBe('Play');
            expect(transloco.translate('PLAYER.ACTION_PAUSE')).toBe('Pause');
        });

        it('should resolve references of the English text in English', async () => {
            files = {
                en: { ...en, APP: { ...en.APP, ACTION_ACKNOWLEDGE: '@:APP.ACTION_OK', ACTION_OK: 'OK (English)' } },
                ja: { ...ja, APP: { ...ja.APP, ACTION_OK: 'OK (Japanese)' } }
            };
            await activate('ja');

            expect(transloco.translate('SETTINGS_PREFERENCES.ACTION_ACKNOWLEDGE')).toBe('OK (English)');
        });

        it('should display the referenced key if English lacks it, too', async () => {
            files = {
                en: { ...en, SETTINGS_PREFERENCES: { ...en.SETTINGS_PREFERENCES, ACTION_ACKNOWLEDGE: '@:APP.ACTION_MISSING' } },
                ja: { ...ja, SETTINGS_PREFERENCES: { ...ja.SETTINGS_PREFERENCES, ACTION_ACKNOWLEDGE: '@:APP.ACTION_MISSING' } }
            };

            await activate('ja');
            expect(transloco.translate('SETTINGS_PREFERENCES.ACTION_ACKNOWLEDGE')).toBe('APP.ACTION_MISSING');

            await activate('en');
            expect(transloco.translate('SETTINGS_PREFERENCES.ACTION_ACKNOWLEDGE')).toBe('APP.ACTION_MISSING');
        });

        it('should resolve references between translations of extensions and of the application', async () => {
            files = {
                en: { ...en, EXAMPLE_EXTENSION: { TEXT_ENGLISH_ONLY: 'Text of the extension' } },
                ja: {
                    ...ja,
                    LOGIN            : { ...ja.LOGIN, INFO_PASSWORD_EXPIRED: '@:EXAMPLE_EXTENSION.TEXT_ENGLISH_ONLY' },
                    EXAMPLE_EXTENSION: { ACTION_ACKNOWLEDGE: '@:APP.ACTION_ACKNOWLEDGE' }
                }
            };
            await activate('ja');

            expect(transloco.translate('EXAMPLE_EXTENSION.ACTION_ACKNOWLEDGE')).toBe('OK');
            expect(transloco.translate('LOGIN.INFO_PASSWORD_EXPIRED')).toBe('Text of the extension');
        });

        it('should keep resolving references to keys of the active language in that language', async () => {
            await activate('ja');

            expect(transloco.translate('LOGIN.ACTION_LOGIN')).toBe('ログイン');
        });
    });

    describe('with a key which the active language lacks', () => {

        it('should display the English text with the plural rules of English in Japanese', async () => {
            await activate('ja');

            expect(transloco.translate('IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS', { NUMBER: 1 })).toBe('1 connection imported successfully.');
            expect(transloco.translate('HOME.INFO_AVAILABLE_CONNECTIONS', { AVAILABLE: 1, TOTAL: 1 })).toBe('1 of 1 connection available');
        });

        it('should display the English text with the plural rules of English in Russian', async () => {
            await activate('ru');

            expect(transloco.translate('CLIENT.INFO_USER_COUNT', { USERNAME: 'guacadmin', COUNT: 21 })).toBe('guacadmin (21)');
        });

        it('should keep displaying texts of the active language with its plural rules', async () => {
            await activate('ru');

            expect(transloco.translate('PLAYER.ACTION_PLAY')).toBe('Play');
            expect(transloco.translate('SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION', { VALUE: 5, UNIT: 'second' })).toBe('5 секунд');
            expect(transloco.translate('SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION', { VALUE: 2, UNIT: 'second' })).toBe('2 секунды');
        });
    });

    describe('with the language of the preferences as start language', () => {

        /**
         * Starts Transloco anew with the given language of the preferences
         * and loads that language without activating it, as on reloading the
         * page with an existing session.
         *
         * @param language
         *     The key of the language which the preferences contain.
         */
        async function restart(language: string): Promise<void> {
            TestBed.resetTestingModule();
            start(language);
            await firstValueFrom(transloco.load(language));
        }

        /**
         * Returns the duration of a history entry as the connection history
         * displays it.
         */
        const duration = (VALUE: number, UNIT: string) =>
            transloco.translate('SETTINGS_CONNECTION_HISTORY.TEXT_HISTORY_DURATION', { VALUE, UNIT });

        it('should format Russian texts with the plural rules of Russian', async () => {
            await restart('ru');

            expect(duration(3, 'day')).toBe('3 дня');
            expect(duration(5, 'minute')).toBe('5 минут');
            expect(duration(21, 'second')).toBe('21 секунда');
        });

        it('should keep the plural rules of Russian after an English text', async () => {
            await restart('ru');

            expect(transloco.translate('CLIENT.INFO_USER_COUNT', { USERNAME: 'guacadmin', COUNT: 21 })).toBe('guacadmin (21)');
            expect(duration(3, 'day')).toBe('3 дня');
        });

        it('should display English texts for a language without translation file', async () => {

            // The loader resolves languages without translation file with an empty translation
            files = { ...files, sv: {} };
            await restart('sv');

            expect(transloco.translate('LOGIN.ACTION_LOGIN')).toBe('Login');
            expect(transloco.translate('HOME.INFO_AVAILABLE_CONNECTIONS', { AVAILABLE: 1, TOTAL: 2 })).toBe('1 of 2 connections available');
        });

        it('should display the texts of the language of a key with region', async () => {

            // The AngularJS frontend stores the language of the browser with region ("cs_CZ")
            files = { ...files, cs_CZ: cs };
            await restart('cs_CZ');

            expect(transloco.translate('LOGIN.ACTION_LOGIN')).toBe('Přihlásit');
            expect(transloco.translate('HOME.SECTION_HEADER_RECENT_CONNECTIONS')).toBe('Nedávná spojení');
        });

        it('should format texts of a language key with region with the rules of its language', async () => {
            files = { ...files, cs_CZ: cs };
            await restart('cs_CZ');

            // Reloading the translations after login activates the language again
            transloco.setActiveLang('cs_CZ');

            expect(transloco.translate('PLAYER.INFO_NUMBER_OF_RESULTS', { RESULTS: 3 })).toBe('3 shody');
            expect(transloco.translate('CLIENT.INFO_USER_COUNT', { USERNAME: 'guacadmin', COUNT: 3 })).toBe('guacadmin (3)');
        });
    });
});
