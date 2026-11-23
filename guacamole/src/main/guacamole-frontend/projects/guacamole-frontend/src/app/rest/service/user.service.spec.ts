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
import { firstValueFrom } from 'rxjs';
import { CacheInterceptor } from '../interceptor/cache.interceptor';
import { UserService } from './user.service';

describe('UserService', () => {

    const USERS_URL = 'api/session/data/postgresql/users';

    let service: UserService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: HTTP_INTERCEPTORS, useClass: CacheInterceptor, multi: true },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });
        service = TestBed.inject(UserService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('getUsers', () => {

        it('should not send a permission filter if none is given', async () => {
            const result = firstValueFrom(service.getUsers('postgresql'));

            httpMock.expectOne(USERS_URL).flush({});
            await result;
        });

        it('should send the permission filter if one is given', async () => {
            const result = firstValueFrom(service.getUsers('postgresql', ['UPDATE']));

            httpMock.expectOne(`${USERS_URL}?permission=UPDATE`).flush({});
            await result;
        });

        it('should send one permission parameter for each permission type', async () => {
            const result = firstValueFrom(service.getUsers('postgresql', ['UPDATE', 'DELETE']));

            httpMock.expectOne(`${USERS_URL}?permission=UPDATE&permission=DELETE`).flush({});
            await result;
        });

        it('should cache the filtered and the unfiltered list separately', async () => {
            const unfiltered = firstValueFrom(service.getUsers('postgresql'));
            httpMock.expectOne(USERS_URL).flush({ alice: { username: 'alice' }, bob: { username: 'bob' } });
            await unfiltered;

            const filtered = firstValueFrom(service.getUsers('postgresql', ['UPDATE']));
            httpMock.expectOne(`${USERS_URL}?permission=UPDATE`).flush({ bob: { username: 'bob' } });

            expect(Object.keys(await filtered)).toEqual(['bob']);
            expect(Object.keys(await firstValueFrom(service.getUsers('postgresql')))).toEqual(['alice', 'bob']);
        });

    });

    describe('updateUserPassword', () => {

        it('should send the old and the new password', async () => {
            const result = firstValueFrom(service.updateUserPassword('postgresql', 'alice', 'old', 'new'));

            const request = httpMock.expectOne(`${USERS_URL}/alice/password`);
            expect(request.request.method).toBe('PUT');
            expect(JSON.parse(request.request.serializeBody() as string)).toEqual({ oldPassword: 'old', newPassword: 'new' });
            request.flush(null);
            await result;
        });

    });

});
