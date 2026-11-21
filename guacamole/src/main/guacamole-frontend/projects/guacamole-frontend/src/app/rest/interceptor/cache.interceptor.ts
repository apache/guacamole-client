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

import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { filter, map, Observable, shareReplay, tap } from 'rxjs';
import { CACHE, CacheService } from '../service/cache.service';

/**
 * Interceptor which caches the responses of GET requests marked with the
 * {@link CACHE} context token, storing each response within the cache of
 * {@link CacheService} named by that token.
 */
@Injectable()
export class CacheInterceptor implements HttpInterceptor {

    /**
     * Inject required services.
     */
    constructor(private cacheService: CacheService) {
    }

    /**
     * Satisfies GET requests marked as cacheable from the cache named by the
     * {@link CACHE} context token, issuing the request only if that cache
     * does not yet contain a response for the same URL. Concurrent requests
     * for the same URL share a single request, error responses are never
     * cached, and each caller receives its own copy of the response body.
     * All other requests are passed through unchanged.
     */
    intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {

        // Pass through requests which are not marked as cacheable
        const cacheName = request.context.get(CACHE);
        if (!cacheName || request.method !== 'GET')
            return next.handle(request);

        const cache = this.cacheService[cacheName];
        const key = request.urlWithParams;

        // Issue the request only if no response is cached or pending
        let response = cache.get(key);
        if (!response) {
            const pending: Observable<HttpResponse<unknown>> = next.handle(request).pipe(
                filter((event): event is HttpResponse<unknown> => event instanceof HttpResponse),

                // Remove failed requests from the cache, unless the cache
                // has been cleared and refilled in the meantime
                tap({
                    error: () => {
                        if (cache.get(key) === pending)
                            cache.delete(key);
                    }
                }),
                shareReplay(1)
            );
            cache.set(key, pending);
            response = pending;
        }

        // Copy the response body for each caller, such that changes made by
        // one caller do not affect the cached response
        return response.pipe(map(cached => cached.clone({ body: structuredClone(cached.body) })));

    }

}
