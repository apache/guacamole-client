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

import { HttpClient, HttpContext, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import { catchError, from, map, Observable, of, switchMap, throwError } from 'rxjs';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { RequestService } from '../../rest/service/request.service';
import { Error } from '../../rest/types/Error';
import { LocalStorageService } from '../../storage/local-storage.service';
import { AUTHENTICATION_TOKEN } from '../../util/interceptor.service';
import { AuthenticationResult } from '../types/AuthenticationResult';

/**
 * The unique identifier of the local storage key which stores the latest
 * authentication token.
 */
const AUTH_TOKEN_STORAGE_KEY = 'GUAC_AUTH_TOKEN';

/**
 * A service for authenticating a user against the REST API. Invoking the
 * authenticate() or login() functions of this service will automatically
 * affect the login dialog, if visible.
 *
 * This service broadcasts events on $rootScope depending on the status and
 * result of authentication operations:
 *
 *  "guacLoginPending"
 *      An authentication request is being submitted and we are awaiting the
 *      result. The request may not yet have been submitted if the parameters
 *      for that request are not ready. This event receives a promise that
 *      resolves with the HTTP parameters that were ultimately submitted as its
 *      sole parameter.
 *
 *  "guacLogin"
 *      Authentication was successful and a new token was created. This event
 *      receives the authentication token as its sole parameter.
 *
 *  "guacLogout"
 *      An existing token is being destroyed. This event receives the
 *      authentication token as its sole parameter. If the existing token for
 *      the current session is being replaced without destroying that session,
 *      this event is not fired.
 *
 *  "guacLoginFailed"
 *      An authentication request has failed for any reason. This event is
 *      broadcast before any other events that are specific to the nature of
 *      the failure, and may be used to detect login failures in lieu of those
 *      events. This event receives two parameters: the HTTP parameters
 *      submitted and the Error object received from the REST endpoint.
 *
 *  "guacInsufficientCredentials"
 *      An authentication request failed because additional credentials are
 *      needed before the request can be processed. This event receives two
 *      parameters: the HTTP parameters submitted and the Error object received
 *      from the REST endpoint.
 *
 *  "guacInvalidCredentials"
 *      An authentication request failed because the credentials provided are
 *      invalid. This event receives two parameters: the HTTP parameters
 *      submitted and the Error object received from the REST endpoint.
 */
@Injectable({
    providedIn: 'root'
})
export class AuthenticationService {

    /**
     * The most recent authentication result, or null if no authentication
     * result is cached.
     */
    cachedResult: AuthenticationResult | null = null;

    constructor(private localStorageService: LocalStorageService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private requestService: RequestService,
                private http: HttpClient) {
    }

    /**
     * Retrieves the authentication result cached in memory. If the user has not
     * yet authenticated, the user has logged out, or the last authentication
     * attempt failed, null is returned.
     *
     * NOTE: setAuthenticationResult() will be called upon page load, so the
     * cache should always be populated after the page has successfully loaded.
     *
     * @returns
     *     The last successful authentication result, or null if the user is not
     *     currently authenticated.
     */
    getAuthenticationResult(): AuthenticationResult | null {

        // Use cached result, if any
        if (this.cachedResult)
            return this.cachedResult;

        // Return explicit undefined if no auth data is currently stored
        return null;

    }

    /**
     * Stores the given authentication result for future retrieval. The given
     * result MUST be the result of the most recent authentication attempt.
     *
     * @param data
     *     The last successful authentication result, or null if the last
     *     authentication attempt failed.
     */
    setAuthenticationResult(data: AuthenticationResult | null): void {

        // Clear the currently-stored result and auth token if the last
        // attempt failed
        if (!data) {
            this.cachedResult = null;
            this.localStorageService.removeItem(AUTH_TOKEN_STORAGE_KEY);
        }

            // Otherwise, store the authentication attempt directly.
            // Note that only the auth token is stored in persistent local storage.
            // To re-obtain an authentication result upon a fresh page load,
            // reauthenticate with the persistent token, which can be obtained by
        // calling getCurrentToken().
        else {

            // Always store in cache
            this.cachedResult = data;

            // Persist only the auth token past tab/window closure, and only
            // if not anonymous
            if (data.username !== AuthenticationResult.ANONYMOUS_USERNAME)
                this.localStorageService.setItem(
                    AUTH_TOKEN_STORAGE_KEY, data.authToken);

        }

    }

    /**
     * Clears the stored authentication result, if any. If no authentication
     * result is currently stored, this function has no effect.
     */
    clearAuthenticationResult() {
        this.setAuthenticationResult(null);
    }

    /**
     * Makes a request to authenticate a user using the token REST API endpoint
     * and given arbitrary parameters, returning a promise that succeeds only
     * if the authentication operation was successful. The resulting
     * authentication data can be retrieved later via getCurrentToken() or
     * getCurrentUsername(). Invoking this function will affect the UI,
     * including the login screen if visible.
     *
     * The provided parameters can be virtually any object, as each property
     * will be sent as an HTTP parameter in the authentication request.
     * Standard parameters include "username" for the user's username,
     * "password" for the user's associated password, and "token" for the
     * auth token to check/update.
     *
     * If a token is provided, it will be reused if possible.
     *
     * @param parameters
     *     Arbitrary parameters to authenticate with. If a Promise is provided,
     *     that Promise must resolve with the parameters to be submitted when
     *     those parameters are available, and any error will be handled as if
     *     from the authentication endpoint of the REST API itself.
     *
     * @returns
     *     An Observable which emits the authentication result only if the login operation was successful.
     */
    authenticate(parameters: object | Promise<any>): Observable<AuthenticationResult> {

        // Coerce received parameters object into a Promise, if it isn't
        // already a Promise
        const parametersPromise = Promise.resolve(parameters);

        // Notify that a fresh authentication request is underway
        this.guacEventService.broadcast('guacLoginPending', {parameters: parametersPromise});

        // Attempt authentication after auth parameters are available ...
        return from(parametersPromise).pipe(
            switchMap((requestParams: any) => {
                    return this.http.post<AuthenticationResult>(
                        'api/tokens',
                        this.toHttpParams(requestParams),
                        {
                            headers: new HttpHeaders({'Content-Type': 'application/x-www-form-urlencoded'}),
                        }
                    )
                }
            ),

            // ... if authentication succeeds, handle received auth data ...
            map((data: AuthenticationResult) => {
                const currentToken = this.getCurrentToken();

                // If a new token was received, ensure the old token is invalidated,
                // if any, and notify listeners of the new token
                if (data.authToken !== currentToken) {

                    // If an old token existed, request that the token be revoked
                    if (currentToken) {
                        this.revokeToken(currentToken).subscribe();
                    }

                    // Notify of login and new token
                    this.setAuthenticationResult(new AuthenticationResult(data));
                    this.guacEventService.broadcast('guacLogin', {authToken: data.authToken});

                }
                    // Update cached authentication result, even if the token remains
                // the same
                else
                    this.setAuthenticationResult(new AuthenticationResult(data));

                // Authentication was successful
                return data;
            }),

            // ... if authentication fails, propagate failure to returned promise
            catchError(this.requestService.createErrorCallback((error: Error): Observable<never> => {

                        // Notify of generic login failure, for any event consumers that
                        // wish to handle all types of failures at once
                        this.guacEventService.broadcast('guacLoginFailed', {parameters: parametersPromise, error});

                        // Request credentials if provided credentials were invalid
                        if (error.type === Error.Type.INVALID_CREDENTIALS) {
                            this.guacEventService.broadcast('guacInvalidCredentials', {
                                parameters: parametersPromise,
                                error
                            });
                            this.clearAuthenticationResult();
                        }

                        // Request more credentials if provided credentials were not enough
                        else if (error.type === Error.Type.INSUFFICIENT_CREDENTIALS) {
                            this.guacEventService.broadcast('guacInsufficientCredentials', {
                                parameters: parametersPromise,
                                error
                            });
                            this.clearAuthenticationResult();
                        }

                        // Abort rendering of page if an internal error occurs
                        else if (error.type === Error.Type.INTERNAL_ERROR)
                            this.guacEventService.broadcast('guacFatalPageError', {error});

                        // Authentication failed
                        return throwError(() => error);

                    }
                )
            )
        );

    }

    /**
     * Converts the given object into an equivalent HttpParams object by adding
     * a parameter for each property of the given object.
     *
     * @param object
     *     The object to convert into HttpParams.
     *
     * @returns
     *     An HttpParams object containing a parameter for each property of the
     *     given object.
     */
    private toHttpParams(object: any): HttpParams {
        return Object.getOwnPropertyNames(object)
            .reduce((p, key) => p.set(key, object[key]), new HttpParams());
    }

    /**
     * Makes a request to update the current auth token, if any, using the
     * token REST API endpoint. If the optional parameters object is provided,
     * its properties will be included as parameters in the update request.
     * This function returns a promise that succeeds only if the authentication
     * operation was successful. The resulting authentication data can be
     * retrieved later via getCurrentToken() or getCurrentUsername().
     *
     * If there is no current auth token, this function behaves identically to
     * authenticate(), and makes a general authentication request.
     *
     * @param parameters
     *     Arbitrary parameters to authenticate with, if any.
     *
     * @returns
     *     A promise which succeeds only if the login operation was successful.
     */
    updateCurrentToken(parameters: object): Observable<AuthenticationResult> {

        // HTTP parameters for the authentication request
        let httpParameters: any = {};

        // Add token parameter if current token is known
        const token = this.getCurrentToken();
        if (token)
            httpParameters.token = this.getCurrentToken();

        // Add any additional parameters
        if (parameters)
            httpParameters = {...httpParameters, ...parameters};

        // Make the request
        return this.authenticate(httpParameters);

    }

    /**
     * Determines whether the session associated with a particular token is
     * still valid, without performing an operation that would result in that
     * session being marked as active. If no token is provided, the session of
     * the current user is checked.
     *
     * @param token
     *     The authentication token to pass with the "Guacamole-Token" header.
     *     If omitted, and the user is logged in, the user's current
     *     authentication token will be used.
     *
     * @returns
     *     A promise that resolves with the boolean value "true" if the session
     *     is valid, and resolves with the boolean value "false" otherwise,
     *     including if an error prevents session validity from being
     *     determined. The promise is never rejected.
     */
    getValidity(token?: string): Observable<boolean> {

        // NOTE: Because this is a HEAD request, we will not receive a JSON
        // response body. We will only have a simple yes/no regarding whether
        // the auth token can be expected to be usable.
        return this.http.head(
            'api/session',
            {
                context: new HttpContext().set(AUTHENTICATION_TOKEN, token)
            })
            .pipe(
                map(() => true),
                catchError(() => of(false))
            );
    }

    /**
     * Makes a request to revoke an authentication token using the token REST
     * API endpoint, returning a promise that succeeds only if the token was
     * successfully revoked.
     *
     * @param token
     *     The authentication token to revoke.
     *
     * @returns
     *     A promise which succeeds only if the token was successfully revoked.
     */
    revokeToken(token: string | null): Observable<void> {
        return this.http.delete<void>(
            'api/session',
            {
                context: new HttpContext().set(AUTHENTICATION_TOKEN, token)
            }
        );
    }

    /**
     * Makes a request to authenticate a user using the token REST API endpoint
     * with a username and password, ignoring any currently-stored token,
     * returning a promise that succeeds only if the login operation was
     * successful. The resulting authentication data can be retrieved later
     * via getCurrentToken() or getCurrentUsername(). Invoking this function
     * will affect the UI, including the login screen if visible.
     *
     * @param username
     *     The username to log in with.
     *
     * @param password
     *     The password to log in with.
     *
     * @returns
     *     A promise which succeeds only if the login operation was successful.
     */
    login(username: string, password: string): Observable<AuthenticationResult> {
        return this.authenticate({
            username: username,
            password: password
        });
    }

    /**
     * Makes a request to logout a user using the token REST API endpoint,
     * returning a promise that succeeds only if the logout operation was
     * successful. Invoking this function will affect the UI, causing the
     * visible components of the application to be replaced with a status
     * message noting that the user has been logged out.
     *
     * @returns
     *     A promise which succeeds only if the logout operation was
     *     successful.
     */
    logout(): Observable<void> {

        // Clear authentication data
        const token = this.getCurrentToken();
        this.clearAuthenticationResult();

        // Notify listeners that a token is being destroyed
        this.guacEventService.broadcast('guacLogout', {token});

        // Delete old token
        return this.revokeToken(token);

    }

    /**
     * Returns whether the current user has authenticated anonymously. An
     * anonymous user is denoted by the identifier reserved by the Guacamole
     * extension API for anonymous users (the empty string).
     *
     * @returns {Boolean}
     *     true if the current user has authenticated anonymously, false
     *     otherwise.
     */
    isAnonymous(): boolean {
        return this.getCurrentUsername() === '';
    }

    /**
     * Returns the username of the current user. If the current user is not
     * logged in, this value may not be valid.
     *
     * @returns {String}
     *     The username of the current user, or null if no authentication data
     *     is present.
     */
    getCurrentUsername(): string | null {

        // Return username, if available
        const authData = this.getAuthenticationResult();
        if (authData)
            return authData.username;

        // No auth data present
        return null;

    }

    /**
     * Returns the auth token associated with the current user. If the current
     * user is not logged in, this token may not be valid.
     *
     * @returns
     *     The auth token associated with the current user, or null if no
     *     authentication data is present.
     */
    getCurrentToken(): string | null {

        // Return cached auth token, if available
        const authData = this.getAuthenticationResult();
        if (authData)
            return authData.authToken;

        // Fall back to the value from local storage if not found in cache
        return this.localStorageService.getItem(AUTH_TOKEN_STORAGE_KEY) as string | null;

    }

    /**
     * Returns the identifier of the data source that authenticated the current
     * user. If the current user is not logged in, this value may not be valid.
     *
     * @returns
     *     The identifier of the data source that authenticated the current
     *     user, or null if no authentication data is present.
     */
    getDataSource(): string | null {

        // Return data source, if available
        const authData = this.getAuthenticationResult();
        if (authData)
            return authData.dataSource;

        // No auth data present
        return null;

    }

    /**
     * Returns the identifiers of all data sources available to the current
     * user. If the current user is not logged in, this value may not be valid.
     *
     * @returns
     *     The identifiers of all data sources available to the current user,
     *     or an empty array if no authentication data is present.
     */
    getAvailableDataSources(): string[] {

        // Return data sources, if available
        const authData = this.getAuthenticationResult();
        if (authData)
            return authData.availableDataSources || []

        // No auth data present
        return [];

    }

}
