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

import { Injectable } from '@angular/core';
import { HttpContextToken, HttpRequest } from '@angular/common/http';

/**
 * HTTP context token that can be used to skip all interceptors when issuing a
 * request.
 * TODO: Maybe remove this and just use SKIP_AUTHENTICATION_INTERCEPTOR and SKIP_ERROR_HANDLING_INTERCEPTOR
 */
export const SKIP_ALL_INTERCEPTORS: HttpContextToken<boolean> = new HttpContextToken<boolean>(() => false);

/**
 * HTTP context token that can be used to skip the {@link ErrorHandlingInterceptor} when issuing a request.
 */
export const SKIP_ERROR_HANDLING_INTERCEPTOR: HttpContextToken<boolean> = new HttpContextToken<boolean>(() => false);

/**
 * HTTP context token that can be used to skip the {@link AuthenticationInterceptor} when issuing a request.
 */
export const SKIP_AUTHENTICATION_INTERCEPTOR: HttpContextToken<boolean> = new HttpContextToken<boolean>(() => false);

/**
 * HTTP context token that can be used to set the authentication token to pass with the "Guacamole-Token" header.
 * If omitted, and the user is logged in, the user's current authentication token will be used.
 */
export const AUTHENTICATION_TOKEN: HttpContextToken<string | null> = new HttpContextToken<string | null>(() => null);


/**
 * Service that can be used by interceptors to determine whether they should
 * skip processing for a given request.
 */
@Injectable({
    providedIn: 'root'
})
export class InterceptorService {

    /**
     * Returns whether the given request should skip all interceptors.
     *
     * @param request
     *    The request to check.
     *
     * @returns
     *     true if the request should skip all interceptors, false otherwise.
     */
    skipAllInterceptors(request: HttpRequest<any>): boolean {
        return request.context.get(SKIP_ALL_INTERCEPTORS);
    }

    /**
     * Returns whether the given request should skip the {@link ErrorHandlingInterceptor}.
     *
     * @param request
     *    The request to check.
     *
     * @returns
     *     true if the request should skip all interceptors, false otherwise.
     */
    skipErrorHandlingInterceptor(request: HttpRequest<any>): boolean {
        return request.context.get(SKIP_ERROR_HANDLING_INTERCEPTOR);
    }

    /**
     * Returns whether the given request should skip the {@link AuthenticationInterceptor}.
     *
     * @param request
     *    The request to check.
     *
     * @returns
     *     true if the request should skip all interceptors, false otherwise.
     */
    skipAuthenticationInterceptor(request: HttpRequest<any>): boolean {
        return request.context.get(SKIP_AUTHENTICATION_INTERCEPTOR);
    }


    /**
     * Returns the authentication token to pass with the "Guacamole-Token" header.
     *
     * @param request
     *     The request to check.
     *
     * @returns
     *     The authentication token if set, null otherwise.
     */
    getAuthenticationToken(request: HttpRequest<any>): string | null {
        return request.context.get(AUTHENTICATION_TOKEN);
    }

}
