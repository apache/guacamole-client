

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

import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { InterceptorService } from '../../util/interceptor.service';
import { Error } from '../types/Error';

/**
 * TODO
 */
@Injectable()
export class ErrorHandlingInterceptor implements HttpInterceptor {

    /**
     * Inject required services.
     */
    constructor(private interceptorService: InterceptorService) {
    }

    /**
     * If an HTTP error response is received from the REST API, this interceptor will
     * map the response to an Observable that will throw an error strictly with an
     * instance of an {@link Error} object.
     */
    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

        // Skip this interceptor if the corresponding HttpContextToken is set
        if (this.interceptorService.skipErrorHandlingInterceptor(request)) {
            return next.handle(request);
        }

        return next.handle(request)
            .pipe(
                // Catch any errors and map them to an Error object if the
                // received error originates from the REST API
                catchError(this.handleError)
            );
    }

    /**
     * Wraps a HttpErrorResponse into an Error object.
     *
     * @param error
     *     The HttpErrorResponse to wrap.
     *
     * @returns
     *     An Observable that will emit a single Error object wrapping the given HttpErrorResponse.
     */
    private handleError(error: HttpErrorResponse) {

        // Wrap true error responses within REST Error objects
        if (error.status !== 0) {
            return throwError(() => new Error(error.error));
        }

        // Fall back to a generic internal error if the request couldn't
        // even be issued (webapp is down, etc.)
        return throwError(() => new Error({ message: 'Unknown failure sending HTTP request' }));
    }
}
