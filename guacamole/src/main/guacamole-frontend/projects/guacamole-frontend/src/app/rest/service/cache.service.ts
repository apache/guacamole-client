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

import { HttpContextToken, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import { Observable } from 'rxjs';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';

/**
 * The name of one of the REST API response caches of {@link CacheService}.
 */
export type CacheName = 'connections' | 'languages' | 'patches' | 'schema' | 'users';

/**
 * HTTP context token which marks a GET request as cacheable. Its value is the
 * name of the {@link CacheService} cache which should store the response, or
 * null if the response should not be cached.
 */
export const CACHE: HttpContextToken<CacheName | null> = new HttpContextToken<CacheName | null>(() => null);

/**
 * A cache of REST API responses, where each key is the URL of a request,
 * including its parameters, and each value is an observable which emits the
 * response to that request once it has been received.
 */
export type ResponseCache = Map<string, Observable<HttpResponse<unknown>>>;

/**
 * Service which contains all REST API response caches.
 */
@Injectable({
    providedIn: 'root'
})
export class CacheService {

    /**
     * Shared cache used by ConnectionGroupService, ConnectionService and
     * SharingProfileService.
     */
    readonly connections: ResponseCache = new Map();

    /**
     * Cache used by LanguageService.
     */
    readonly languages: ResponseCache = new Map();

    /**
     * Cache used by PatchService.
     */
    readonly patches: ResponseCache = new Map();

    /**
     * Cache used by SchemaService.
     */
    readonly schema: ResponseCache = new Map();

    /**
     * Shared cache used by UserService, UserGroupService, PermissionService,
     * and MembershipService.
     */
    readonly users: ResponseCache = new Map();

    /**
     * Inject required services.
     */
    constructor(private guacEventService: GuacEventService<GuacFrontendEventArguments>) {

        // Clear caches on logout
        this.guacEventService.on('guacLogout')
            .subscribe(() => {
                this.clearCaches();
            });

    }

    /**
     * Clear all caches defined in this service, except the cache of HTML
     * patches, which do not depend on the current user.
     */
    clearCaches(): void {
        this.connections.clear();
        this.languages.clear();
        this.schema.clear();
        this.users.clear();
    }

}
