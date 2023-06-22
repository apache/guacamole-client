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
import { GuacEventService } from 'guacamole-frontend-lib';
import { Observable, of, throwError } from 'rxjs';
import {
    GuacFrontendEventArguments
} from '../../events/types/GuacFrontendEventArguments';
import { LogService } from '../../util/log.service';
import { Error } from '../types/Error';

/**
 * Service for converting observables from the HttpService that pass the entire response into
 * promises that pass only the data from that response.
 */
@Injectable({
    providedIn: 'root'
})
export class RequestService {

    constructor(private log: LogService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>) {
    }

    /**
     * Creates a Observable error callback which invokes the given callback only
     * if the Observable failed with a REST @link{Error} object. If the
     * Observable failed without an @link{Error} object, such as when a
     * JavaScript error occurs within a callback earlier in the Observable chain,
     * the error is logged without invoking the given callback and rethrown.
     *
     * @param callback
     *     The callback to invoke if the Observable failed with an
     *     @link{Error} object.
     *
     * @returns
     *     A function which can be provided as the catchError callback for an
     *     Observable.
     */
    createErrorCallback(callback: (error: any) => Observable<any>): (error: any) => Observable<any> {
        return (error: any): Observable<any> => {

            // Invoke given callback ONLY if due to a legitimate REST error
            if (error instanceof Error)
                return callback(error);

            // Log all other errors
            this.log.error(error);
            return throwError(() => error)
        };
    }

    /**
     * Creates a promise error callback which invokes the given callback only
     * if the promise was rejected with a REST @link{Error} object. If the
     * promise is rejected without an @link{Error} object, such as when a
     * JavaScript error occurs within a callback earlier in the promise chain,
     * the rejection is logged without invoking the given callback.
     *
     * @param {Function} callback
     *     The callback to invoke if the promise is rejected with an
     *     @link{Error} object.
     *
     * @returns {Function}
     *     A function which can be provided as the error callback for a
     *     promise.
     */
    createPromiseErrorCallback(callback: Function): ((reason: any) => (PromiseLike<never>) | null | undefined) {
        return ((error: any) => {

            // Invoke given callback ONLY if due to a legitimate REST error
            if (error instanceof Error)
                return callback(error);

            // Log all other errors
            this.log.error(error);

        });
    }

    /**
     * Creates an observable error callback which emits the
     * given default value only if the @link{Error}  is a NOT_FOUND error.
     * All other errors are passed through and must be handled as yet more errors.
     *
     * @param value
     *     The default value to use to emit if the observable failed with a
     *     NOT_FOUND error.
     *
     * @returns
     *     A function which can be provided as the error callback for an
     *     observable.
     */
    defaultValue<T>(value: T): (error: any) => Observable<T> {
        return this.createErrorCallback((error: any) => {

            // Return default value only if not found
            if (error.type === Error.Type.NOT_FOUND)
                return of(value);

            // Reject promise with original error otherwise
            throw error;

        });
    }

    /**
     * Creates a promise error callback which resolves the promise with the
     * given default value only if the @link{Error} in the original rejection
     * is a NOT_FOUND error. All other errors are passed through and must be
     * handled as yet more rejections.
     *
     * @param value
     *     The default value to use to resolve the promise if the promise is
     *     rejected with a NOT_FOUND error.
     *
     * @returns
     *     A function which can be provided as the error callback for a
     *     promise.
     */
    defaultPromiseValue(value: any): Function {
        return this.createErrorCallback(function resolveIfNotFound(error: any) {

            // Return default value only if not found
            if (error.type === Error.Type.NOT_FOUND)
                return value;

            // Reject promise with original error otherwise
            throw error;

        });
    }

    /**
     * Promise error callback which ignores all rejections due to REST errors,
     * but logs all other rejections, such as those due to JavaScript errors.
     * This callback should be used in cases where
     * a REST response is being handled but REST errors should be ignored.
     *
     * @constant
     */
    readonly IGNORE: (error: any) => Observable<any> = this.createErrorCallback((error: any): Observable<any> => {
        return of(void (0));
    });

    /**
     * Promise error callback which logs all rejections due to REST errors as
     * warnings to the browser console, and logs all other rejections as
     * errors. This callback should be used in favor of
     * @link{IGNORE} if REST errors are simply not expected.
     *
     * @constant
     */
    readonly WARN: (error: any) => Observable<any> = this.createErrorCallback((error: any): Observable<any> => {
        this.log.warn(error.type, error.message || error.translatableMessage);
        return of(void (0));
    });

    /**
     * Promise error callback which replaces the content of the page with a
     * generic error message warning that the page could not be displayed. All
     * rejections are logged to the browser console as errors. This callback
     * should be used in favor of @link{WARN} if REST errors will result in the
     * page being unusable.
     *
     * @constant
     */
    readonly DIE: (error: any) => Observable<any> = this.createErrorCallback((error: any): Observable<any> => {
        this.guacEventService.broadcast('guacFatalPageError', error);
        this.log.error(error.type, error.message || error.translatableMessage);
        return of(void (0));
    });

    readonly PROMISE_DIE: ((reason: any) => (PromiseLike<never>) | null | undefined) = this.createPromiseErrorCallback((error: any) => {
        this.guacEventService.broadcast('guacFatalPageError', error);
        this.log.error(error.type, error.message || error.translatableMessage);
    });

    readonly PROMISE_WARN: ((reason: any) => (PromiseLike<never>) | null | undefined) = this.createPromiseErrorCallback((error: any) => {
        this.log.warn(error.type, error.message || error.translatableMessage);
    });

}
