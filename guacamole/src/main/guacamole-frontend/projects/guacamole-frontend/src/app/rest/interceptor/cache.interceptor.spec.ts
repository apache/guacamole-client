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

import { HTTP_INTERCEPTORS, HttpClient, HttpContext, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GuacEventService } from 'guacamole-frontend-lib';
import { firstValueFrom } from 'rxjs';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { CACHE, CacheName, CacheService } from '../service/cache.service';
import { CacheInterceptor } from './cache.interceptor';

/**
 * The part of a connection group tree which the tests below inspect.
 */
interface Tree {
    identifier: string;
    childConnections?: { identifier: string }[];
}

describe('CacheInterceptor', () => {

    const TREE_URL = 'api/session/data/postgresql/connectionGroups/ROOT/tree';
    const PATCHES_URL = 'api/patches';

    let http: HttpClient;
    let httpMock: HttpTestingController;

    /**
     * Issues a GET request for the given URL which is marked as cacheable
     * within the given cache, resolving with the response body.
     */
    const get = <T = Tree>(url: string = TREE_URL, cache: CacheName = 'connections'): Promise<T> =>
        firstValueFrom(http.get<T>(url, { context: new HttpContext().set(CACHE, cache) }));

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: HTTP_INTERCEPTORS, useClass: CacheInterceptor, multi: true },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });
        http = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should not send a second request for a cached response', async () => {
        const first = get();
        httpMock.expectOne(TREE_URL).flush({ identifier: 'ROOT' });
        await first;

        expect(await get()).toEqual({ identifier: 'ROOT' });
        httpMock.expectNone(TREE_URL);
    });

    it('should share a single request among concurrent callers', async () => {
        const first = get();
        const second = get();
        httpMock.expectOne(TREE_URL).flush({ identifier: 'ROOT' });

        expect(await first).toEqual({ identifier: 'ROOT' });
        expect(await second).toEqual({ identifier: 'ROOT' });
    });

    it('should provide each caller with its own copy of the response', async () => {
        const first = get();
        const second = get();
        httpMock.expectOne(TREE_URL).flush({ identifier: 'ROOT', childConnections: [] });

        const firstTree = await first;
        firstTree.childConnections!.push({ identifier: '1' });

        expect(await second).toEqual({ identifier: 'ROOT', childConnections: [] });
        expect(await get()).toEqual({ identifier: 'ROOT', childConnections: [] });
    });

    it('should not cache error responses', async () => {
        const getWithToken = (token: string): Promise<Tree> => firstValueFrom(http.get<Tree>(TREE_URL, {
            context: new HttpContext().set(CACHE, 'connections'),
            headers: { 'Guacamole-Token': token }
        }));

        const failed = getWithToken('expired');
        httpMock.expectOne(TREE_URL).flush({ message: 'Permission denied.' }, { status: 403, statusText: 'Forbidden' });
        await expect(failed).rejects.toMatchObject({ status: 403 });

        // The next caller issues its own request rather than repeating the failed one
        const retried = getWithToken('valid');
        const request = httpMock.expectOne(TREE_URL);
        expect(request.request.headers.get('Guacamole-Token')).toBe('valid');
        request.flush({ identifier: 'ROOT' });
        expect(await retried).toEqual({ identifier: 'ROOT' });
    });

    it('should send the request again once its cache has been cleared', async () => {
        const first = get();
        httpMock.expectOne(TREE_URL).flush({ identifier: 'ROOT', childConnections: [] });
        await first;

        TestBed.inject(CacheService).connections.clear();

        const reloaded = get();
        httpMock.expectOne(TREE_URL).flush({ identifier: 'ROOT', childConnections: [{ identifier: '1' }] });
        expect(await reloaded).toEqual({ identifier: 'ROOT', childConnections: [{ identifier: '1' }] });
    });

    it('should clear all caches except the patches cache upon logout', async () => {
        const tree = get();
        const patches = get<string[]>(PATCHES_URL, 'patches');
        httpMock.expectOne(TREE_URL).flush({ identifier: 'ROOT' });
        httpMock.expectOne(PATCHES_URL).flush(['<meta name="after" content=".login-ui">']);
        await tree;
        await patches;

        TestBed.inject<GuacEventService<GuacFrontendEventArguments>>(GuacEventService)
            .broadcast('guacLogout', { token: 'token' });

        const reloadedTree = get();
        httpMock.expectOne(TREE_URL).flush({ identifier: 'ROOT' });
        expect(await reloadedTree).toEqual({ identifier: 'ROOT' });

        expect(await get<string[]>(PATCHES_URL, 'patches')).toEqual(['<meta name="after" content=".login-ui">']);
        httpMock.expectNone(PATCHES_URL);
    });

});
