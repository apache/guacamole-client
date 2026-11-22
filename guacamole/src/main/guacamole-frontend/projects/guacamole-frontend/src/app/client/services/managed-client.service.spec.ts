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
import { config } from 'rxjs';
import { ErrorHandlingInterceptor } from '../../rest/interceptor/error-handling.interceptor';
import { Error } from '../../rest/types/Error';
import { Field } from '../../rest/types/Field';
import { SharingProfile } from '../../rest/types/SharingProfile';
import { ManagedClient } from '../types/ManagedClient';
import { ManagedClientService } from './managed-client.service';

describe('ManagedClientService', () => {
    let service: ManagedClientService;
    let httpMock: HttpTestingController;

    /**
     * Errors which RxJS reports as unhandled, such as errors thrown by the
     * callback of a subscriber.
     */
    let unhandledErrors: unknown[];

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlingInterceptor, multi: true },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });

        service = TestBed.inject(ManagedClientService);
        httpMock = TestBed.inject(HttpTestingController);

        unhandledErrors = [];
        config.onUnhandledError = error => unhandledErrors.push(error);
    });

    afterEach(() => {
        config.onUnhandledError = null;
        httpMock.verify();
        vi.restoreAllMocks();
    });

    describe('createShareLink', () => {

        /**
         * The URL of the REST resource which generates the sharing
         * credentials for the client and the sharing profile below.
         */
        const CREDENTIALS_URL = 'api/session/tunnels/0123-4567/activeConnection/sharingCredentials/42';

        let client: ManagedClient;
        let sharingProfile: SharingProfile;

        /**
         * Waits until all pending callbacks, including those of timers, have
         * run.
         *
         * @returns
         *     A promise which resolves after all pending callbacks.
         */
        const settle = (): Promise<void> => new Promise(resolve => setTimeout(resolve));

        beforeEach(() => {
            client = new ManagedClient({
                id    : 'c2lnbmFs',
                client: {} as Guacamole.Client,
                tunnel: { uuid: '0123-4567' } as Guacamole.Tunnel
            });
            sharingProfile = new SharingProfile({ identifier: '42', name: 'Watch' });
        });

        it('should add a share link once the sharing credentials are received', async () => {
            service.createShareLink(client, sharingProfile);

            httpMock.expectOne(CREDENTIALS_URL).flush({
                expected: [{ name: 'key', type: Field.Type.QUERY_PARAMETER }],
                values  : { key: 'secret' }
            });
            await settle();

            expect(Object.keys(client.shareLinks)).toEqual(['42']);
            expect(client.shareLinks['42'].href).toMatch(/#\/\?key=secret$/);
            expect(unhandledErrors).toEqual([]);
        });

        it('should only log a warning and add no share link if the sharing credentials cannot be retrieved', async () => {
            const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

            service.createShareLink(client, sharingProfile);

            httpMock.expectOne(CREDENTIALS_URL).flush(
                { type: Error.Type.INTERNAL_ERROR, message: 'Simulated failure' },
                { status: 500, statusText: 'Internal Server Error' }
            );
            await settle();

            expect(client.shareLinks).toEqual({});
            expect(warn).toHaveBeenCalledWith(Error.Type.INTERNAL_ERROR, 'Simulated failure');
            expect(unhandledErrors).toEqual([]);
        });

    });

});
