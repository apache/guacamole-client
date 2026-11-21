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

import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Injector } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, Routes } from '@angular/router';
import { BootstrapExtensionFunction } from 'guacamole-frontend-ext-lib';
import { appRoutes, authGuard, fallbackRoute, titleResolver } from '../../app-routing.module';
import { ErrorHandlingInterceptor } from '../../rest/interceptor/error-handling.interceptor';
import { Error as RestError } from '../../rest/types/Error';
import { LogService } from '../../util/log.service';
import { LOAD_REMOTE_MODULE, LoadRemoteModule } from '../types/LoadRemoteModule';
import { ExtensionLoaderService } from './extension-loader.service';

/**
 * Stub for the component of a route of an extension.
 */
@Component({ template: '' })
class ExtensionPageStubComponent {
}

describe('ExtensionLoaderService', () => {
    let service: ExtensionLoaderService;

    /**
     * Fake for the loadRemoteModule() function of Native Federation.
     */
    const loadRemoteModule = vi.fn<LoadRemoteModule>();

    beforeEach(() => {
        loadRemoteModule.mockReset();
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                // The real AuthenticationService against a fake REST API
                { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlingInterceptor, multi: true },
                provideHttpClient(withXhr(), withInterceptorsFromDi()),
                provideHttpClientTesting(),
                { provide: LOAD_REMOTE_MODULE, useValue: loadRemoteModule }
            ]
        });
        service = TestBed.inject(ExtensionLoaderService);
    });

    describe('buildRoutes()', () => {

        it('should add the routes of an extension between the app routes and the fallback route', async () => {
            const extensionRoutes: Routes = [{ path: 'about', redirectTo: '' }];
            const bootstrapExtension = vi.fn().mockReturnValue(extensionRoutes);
            loadRemoteModule.mockResolvedValue({ bootstrapExtension });

            const routes = await service.buildRoutes({
                'guacamole-frontend-extension-example': {
                    routePath: 'angular-extension',
                    pageTitle: 'Angular Extension'
                }
            });

            expect(loadRemoteModule).toHaveBeenCalledWith('guacamole-frontend-extension-example', 'bootstrapExtension');
            expect(bootstrapExtension).toHaveBeenCalledWith(TestBed.inject(Injector));
            expect(routes).toEqual([
                ...appRoutes,
                {
                    path: 'angular-extension',
                    children: extensionRoutes,
                    canActivate: [authGuard],
                    title: 'Angular Extension'
                },
                fallbackRoute
            ]);
        });

        it('should call the configured bootstrap function and use the app title if no page title is set', async () => {
            const bootstrapExtension = vi.fn().mockReturnValue([]);
            loadRemoteModule.mockResolvedValue({ startExtension: bootstrapExtension });

            const routes = await service.buildRoutes({
                'other-extension': {
                    routePath: 'other',
                    bootstrapFunctionName: 'startExtension'
                }
            });

            expect(loadRemoteModule).toHaveBeenCalledWith('other-extension', 'startExtension');
            expect(bootstrapExtension).toHaveBeenCalledOnce();
            expect(routes).toContainEqual({
                path: 'other',
                children: [],
                canActivate: [authGuard],
                data: { titleKey: 'APP.NAME' },
                title: titleResolver
            });
        });

    });

    describe('routes of an extension', () => {
        let router: Router;
        let httpMock: HttpTestingController;

        beforeEach(async () => {
            router = TestBed.inject(Router);
            httpMock = TestBed.inject(HttpTestingController);
            loadRemoteModule.mockResolvedValue({
                bootstrapExtension: () => [{ path: 'about', component: ExtensionPageStubComponent }]
            });
            router.resetConfig(await service.buildRoutes({
                'guacamole-frontend-extension-example': {
                    routePath: 'angular-extension',
                    pageTitle: 'Angular Extension'
                }
            }));
        });

        afterEach(() => {
            localStorage.removeItem('GUAC_AUTH_TOKEN');
            httpMock.verify();
        });

        /**
         * Navigates to a route of the extension and answers the request to
         * the token REST API endpoint which the navigation triggers.
         *
         * @param body
         *     The body of the response to the token request.
         *
         * @param status
         *     The HTTP status of the response to the token request.
         *
         * @returns
         *     A promise which resolves with the result of the navigation.
         */
        const navigateAndAuthenticate = async (body: object, status: number): Promise<boolean> => {
            const navigated = router.navigateByUrl('/angular-extension/about');
            await new Promise(resolve => setTimeout(resolve));
            httpMock.expectOne('api/tokens').flush(body, { status, statusText: status === 200 ? 'OK' : 'Forbidden' });
            return navigated;
        };

        it('should not open the routes of an extension without a valid session', async () => {
            const navigated = await navigateAndAuthenticate({
                type   : RestError.Type.INVALID_CREDENTIALS,
                message: 'Permission Denied.'
            }, 403);

            expect(navigated).toBe(false);
            expect(router.url).toBe('/');
        });

        it('should open the routes of an extension after updating the session', async () => {
            const navigated = await navigateAndAuthenticate({
                authToken           : 'TOKEN',
                username            : 'guacadmin',
                dataSource          : 'postgresql',
                availableDataSources: ['postgresql']
            }, 200);

            expect(navigated).toBe(true);
            expect(router.url).toBe('/angular-extension/about');
        });

    });

    describe('loadExtensionAndSetRouterConfig()', () => {

        it('should leave the routes unchanged if no extensions are installed', async () => {
            const router = TestBed.inject(Router);
            const resetConfig = vi.spyOn(router, 'resetConfig');

            const loaded = service.loadExtensionAndSetRouterConfig();
            TestBed.inject(HttpTestingController).expectOne('native-federation-config.json').flush({});

            await expect(loaded).resolves.toBeUndefined();
            expect(loadRemoteModule).not.toHaveBeenCalled();
            expect(resetConfig).not.toHaveBeenCalled();
        });

        it('should resolve only after the routes of the extensions have been set', async () => {
            const router = TestBed.inject(Router);

            // Hold back the module of the extension until the test releases it
            let releaseModule!: (module: Record<string, BootstrapExtensionFunction>) => void;
            loadRemoteModule.mockReturnValue(new Promise(resolve => releaseModule = resolve));

            let resolved = false;
            const loaded = service.loadExtensionAndSetRouterConfig().then(() => resolved = true);
            TestBed.inject(HttpTestingController).expectOne('native-federation-config.json')
                .flush({ 'guacamole-angular-example': { routePath: 'angular-extension' } });

            // Run all pending promise callbacks while the module is still loading
            await new Promise(resolve => setTimeout(resolve));
            expect(resolved).toBe(false);

            releaseModule({ bootstrapExtension: () => [] });
            await loaded;
            expect(router.config.map(route => route.path)).toContain('angular-extension');
        });

        it('should keep the routes and log the error if an extension cannot be loaded', async () => {
            const router = TestBed.inject(Router);
            const routes = router.config;
            const logError = vi.spyOn(TestBed.inject(LogService), 'error').mockImplementation(() => undefined);
            loadRemoteModule.mockRejectedValue(new Error('Failed to load module'));

            const loaded = service.loadExtensionAndSetRouterConfig();
            TestBed.inject(HttpTestingController).expectOne('native-federation-config.json')
                .flush({ 'missing-extension': { routePath: 'missing-extension' } });

            await expect(loaded).resolves.toBeUndefined();
            expect(router.config).toBe(routes);
            expect(logError).toHaveBeenCalledOnce();
        });

    });

});
