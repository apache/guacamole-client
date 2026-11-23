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

import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GuacEventService } from 'guacamole-frontend-lib';
import { Observable } from 'rxjs';
import { AuthenticationService } from '../../auth/service/authentication.service';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { ErrorHandlingInterceptor } from '../../rest/interceptor/error-handling.interceptor';
import { Error } from '../../rest/types/Error';
import { UserPageService } from './user-page.service';

/**
 * The URL of the REST resource of the connection tree of the data source.
 */
const TREE_URL = 'api/session/data/postgresql/connectionGroups/ROOT/tree';

/**
 * The URL of the REST resource of the effective permissions of the user.
 */
const PERMISSIONS_URL = 'api/session/data/postgresql/self/effectivePermissions';

/**
 * A connection tree without connections, as the REST API returns it.
 */
const EMPTY_TREE = { identifier: 'ROOT', name: 'ROOT', type: 'ORGANIZATIONAL' };

/**
 * Effective permissions without any granted permission, as the REST API
 * returns them.
 */
const NO_PERMISSIONS = {
    connectionPermissions      : {},
    connectionGroupPermissions : {},
    sharingProfilePermissions  : {},
    activeConnectionPermissions: {},
    userPermissions            : {},
    userGroupPermissions       : {},
    systemPermissions          : []
};

/**
 * The body of a failed response of the REST API.
 */
const REST_ERROR = { type: Error.Type.INTERNAL_ERROR, message: 'Simulated failure' };

/**
 * Waits until all pending promise callbacks have run.
 *
 * @returns
 *     A promise which resolves after all pending promise callbacks.
 */
const settle = (): Promise<void> => new Promise(resolve => setTimeout(resolve));

describe('UserPageService', () => {
    let service: UserPageService;
    let httpMock: HttpTestingController;
    let fatalPageErrors: Error[];

    /**
     * Subscribes to the given observable and records the values it emits.
     *
     * @param observable
     *     The observable to subscribe to.
     *
     * @returns
     *     The array to which each emitted value is appended.
     */
    const record = <T>(observable: Observable<T>): T[] => {
        const values: T[] = [];
        observable.subscribe(value => values.push(value));
        return values;
    };

    /**
     * Answers the pending request for the given URL and waits until the
     * service has handled the response.
     *
     * @param url
     *     The URL of the pending request.
     *
     * @param body
     *     The body of the response.
     *
     * @param status
     *     The HTTP status of the response.
     */
    const respond = async (url: string, body: object, status = 200): Promise<void> => {
        await settle();
        httpMock.expectOne(url).flush(body, { status, statusText: status === 200 ? 'OK' : 'Internal Server Error' });
        await settle();
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlingInterceptor, multi: true },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
                {
                    provide : AuthenticationService,
                    useValue: { getAvailableDataSources: () => ['postgresql'], getCurrentUsername: () => 'guacadmin' }
                }
            ]
        });

        service = TestBed.inject(UserPageService);
        httpMock = TestBed.inject(HttpTestingController);

        fatalPageErrors = [];
        TestBed.inject(GuacEventService<GuacFrontendEventArguments>).on('guacFatalPageError')
            .subscribe(({ error }) => fatalPageErrors.push(error));

        vi.spyOn(console, 'error').mockImplementation(() => undefined);
    });

    afterEach(() => {
        httpMock.verify();
        vi.restoreAllMocks();
    });

    describe('getHomePage', () => {

        it('should emit the home page of the user', async () => {
            const homePages = record(service.getHomePage());

            await respond(TREE_URL, EMPTY_TREE);
            await respond(PERMISSIONS_URL, NO_PERMISSIONS);

            expect(homePages.map(page => page.url)).toEqual(['/']);
            expect(fatalPageErrors).toEqual([]);
        });

        it('should replace the page with an error and emit no home page if a request fails', async () => {
            const homePages = record(service.getHomePage());

            await respond(TREE_URL, REST_ERROR, 500);
            await respond(PERMISSIONS_URL, NO_PERMISSIONS);

            expect(homePages).toEqual([]);
            expect(fatalPageErrors.map(error => error.message)).toEqual(['Simulated failure']);
        });

    });

    describe('getSettingsPages', () => {

        it('should emit the settings pages of the user', async () => {
            const settingsPages = record(service.getSettingsPages());

            await respond(PERMISSIONS_URL, NO_PERMISSIONS);

            expect(settingsPages.map(pages => pages.map(page => page.url))).toEqual([['/settings/sessions', '/settings/preferences']]);
            expect(fatalPageErrors).toEqual([]);
        });

        it('should replace the page with an error and emit no settings pages if the request fails', async () => {
            const settingsPages = record(service.getSettingsPages());

            await respond(PERMISSIONS_URL, REST_ERROR, 500);

            expect(settingsPages).toEqual([]);
            expect(fatalPageErrors.map(error => error.message)).toEqual(['Simulated failure']);
        });

    });

});
