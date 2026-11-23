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

import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslocoInterceptor, TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { GuacEventService } from 'guacamole-frontend-lib';
import { firstValueFrom, of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { TranslationReferenceInterceptor } from '../../../locale/locale.module';
import { TranslationLoaderService } from '../../../locale/service/translation-loader.service';
import { CacheService } from '../../../rest/service/cache.service';
import { Error as RestError } from '../../../rest/types/Error';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
    let fixture: ComponentFixture<LoginComponent>;
    let guacEventService: GuacEventService<GuacFrontendEventArguments>;
    let translocoService: TranslocoService;
    let cacheService: CacheService;

    /**
     * The translations the backend delivers after the login, by language key.
     */
    const translationsAfterLogin: Record<string, Record<string, string>> = {
        de: { APP: 'Anwendung (neu)' },
        en: { APP: 'Application (new)' }
    };

    /**
     * Mock of the loader of translation files. Resolves with the translations
     * the backend delivers after the login.
     */
    let loader: { getTranslation: ReturnType<typeof vi.fn> };

    /**
     * Notifies the component that a login succeeded.
     */
    function login(): void {
        guacEventService.broadcast('guacLogin', { authToken: 'token' });
    }

    beforeEach(async () => {
        loader = { getTranslation: vi.fn((lang: string) => Promise.resolve(translationsAfterLogin[lang])) };

        TestBed.configureTestingModule({
            declarations: [LoginComponent],
            imports: [
                TranslocoTestingModule.forRoot({
                    langs: {
                        de: { APP: 'Anwendung (alt)', REMOVED: 'entfällt' },
                        en: { APP: 'Application (old)', REMOVED: 'removed' }
                    },
                    translocoConfig: {
                        availableLangs: ['de', 'en'],
                        defaultLang: 'de',
                        fallbackLang: 'en',

                        // Interpolation of the application, see TranslationService
                        interpolation: ['@:', ':@']
                    }
                })
            ],
            providers: [
                provideTranslocoInterceptor(TranslationReferenceInterceptor),
                provideHttpClient(),
                provideRouter([]),
                { provide: AuthenticationService, useValue: { authenticate: vi.fn() } },
                { provide: TranslationLoaderService, useValue: loader }
            ]
        });
        TestBed.overrideComponent(LoginComponent, { set: { template: '' } });

        guacEventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);
        translocoService = TestBed.inject(TranslocoService);
        cacheService = TestBed.inject(CacheService);

        // Load the translations which exist before the login
        await firstValueFrom(translocoService.load('de'));
        await firstValueFrom(translocoService.load('en'));

        fixture = TestBed.createComponent(LoginComponent);
        fixture.componentRef.setInput('form', []);
        fixture.detectChanges();
    });

    it('should reload the translations of the active and the fallback language after a successful login', async () => {
        login();

        await vi.waitFor(() => {
            expect(translocoService.getTranslation('de')).toEqual({ APP: 'Anwendung (neu)' });
            expect(translocoService.getTranslation('en')).toEqual({ APP: 'Application (new)' });
        });
        expect(loader.getTranslation).toHaveBeenCalledTimes(2);
        expect(loader.getTranslation).toHaveBeenCalledWith('de');
        expect(loader.getTranslation).toHaveBeenCalledWith('en');
    });

    it('should drop translation keys which the reloaded files no longer contain', async () => {
        login();

        await vi.waitFor(() => {
            expect(translocoService.getTranslation('de')).not.toHaveProperty('REMOVED');
        });
    });

    it('should load the language only once if the active language is the fallback language', async () => {
        translocoService.setActiveLang('en');

        login();

        await vi.waitFor(() => {
            expect(translocoService.getTranslation('en')).toEqual({ APP: 'Application (new)' });
        });
        expect(loader.getTranslation).toHaveBeenCalledTimes(1);
        expect(loader.getTranslation).toHaveBeenCalledWith('en');
    });

    it('should resolve references of extensions within the reloaded translations', async () => {
        loader.getTranslation.mockImplementation((lang: string) => Promise.resolve({
            APP: { ERROR_PASSWORD_MISMATCH: lang === 'de' ? 'Die Passwörter stimmen nicht überein.' : 'The provided passwords do not match.' },
            LOGIN: { ERROR_PASSWORD_MISMATCH: '@:APP.ERROR_PASSWORD_MISMATCH' }
        }));

        login();

        await vi.waitFor(() => {
            expect(translocoService.translate('LOGIN.ERROR_PASSWORD_MISMATCH', {}, 'de')).toBe('Die Passwörter stimmen nicht überein.');
            expect(translocoService.translate('LOGIN.ERROR_PASSWORD_MISMATCH', {}, 'en')).toBe('The provided passwords do not match.');
        });
    });

    it('should clear the cache of languages after a successful login', () => {
        cacheService.languages.set('api/languages', of(new HttpResponse({ body: { en: 'English' } })));

        login();

        expect(cacheService.languages.size).toBe(0);
    });

    it('should clear the cache of languages before it reloads the translations', async () => {
        cacheService.languages.set('api/languages', of(new HttpResponse({ body: { en: 'English' } })));
        const cacheSizesAtReload: number[] = [];
        loader.getTranslation.mockImplementation((lang: string) => {
            cacheSizesAtReload.push(cacheService.languages.size);
            return Promise.resolve(translationsAfterLogin[lang]);
        });

        login();

        await vi.waitFor(() => expect(cacheSizesAtReload).toEqual([0, 0]));
    });

    it('should leave the other caches untouched after a successful login', () => {
        cacheService.connections.set('api/session/data/postgresql/connectionGroups/ROOT/tree', of(new HttpResponse({ body: {} })));

        login();

        expect(cacheService.connections.size).toBe(1);
    });

    it('should not clear the cache of languages if the login failed', async () => {
        cacheService.languages.set('api/languages', of(new HttpResponse({ body: { en: 'English' } })));

        guacEventService.broadcast('guacLoginFailed', {
            parameters: Promise.resolve({}),
            error: new RestError({ type: RestError.Type.INVALID_CREDENTIALS })
        });
        await fixture.whenStable();

        expect(cacheService.languages.size).toBe(1);
    });

    it('should not reload the translations if the login failed', async () => {
        guacEventService.broadcast('guacLoginFailed', {
            parameters: Promise.resolve({}),
            error: new RestError({ type: RestError.Type.INVALID_CREDENTIALS })
        });
        await fixture.whenStable();

        expect(loader.getTranslation).not.toHaveBeenCalled();
    });
});
