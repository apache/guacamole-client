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

import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InterceptorService } from '../../util/interceptor.service';
import { AuthenticationService } from '../service/authentication.service';

/**
 * Interceptor which automatically includes the user's current authentication token.
 */
@Injectable()
export class AuthenticationInterceptor implements HttpInterceptor {

    /**
     * Inject required services.
     */
    constructor(private interceptorService: InterceptorService,
                private authenticationService: AuthenticationService) {
    }

    /**
     * Makes an HTTP request automatically including the given authentication
     * token via {@link AUTHENTICATION_TOKEN} using the "Guacamole-Token" header.
     * If no token is provided, the user's current authentication token is used instead.
     * If the user is not logged in, the "Guacamole-Token" header is simply omitted.
     */
    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

        if (this.interceptorService.skipAuthenticationInterceptor(request)) {
            return next.handle(request);
        }

        // Attempt to use current token if none is provided in the request context
        const token = this.interceptorService.getAuthenticationToken(request) || this.authenticationService.getCurrentToken();

        // Add "Guacamole-Token" header if an authentication token is available
        if (token) {
            request = request.clone({
                setHeaders: {
                    'Guacamole-Token': token
                }
            });
        }

        return next.handle(request);

    }
}
