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

/**
 * A service for authenticating a user against the REST API.
 *
 * This service broadcasts two events on $rootScope depending on the result of
 * authentication operations: 'guacLogin' if authentication was successful and
 * a new token was created, and 'guacLogout' if an existing token is being
 * destroyed or replaced. Both events will be passed the related token as their
 * sole parameter.
 *
 * If a login attempt results in an existing token being replaced, 'guacLogout'
 * will be broadcast first for the token being replaced, followed by
 * 'guacLogin' for the new token.
 * 
 * Failed logins may also result in guacInsufficientCredentials or
 * guacInvalidCredentials events, if the provided credentials were rejected for
 * being insufficient or invalid respectively. Both events will be provided
 * the set of parameters originally given to authenticate() and the error that
 * rejected the credentials. The Error object provided will contain set of
 * expected credentials returned by the REST endpoint. This set of credentials
 * will be in the form of a Field array.
 */
angular.module('auth').factory('authenticationService', ['$injector',
        function authenticationService($injector) {

    // Required types
    var AuthenticationResult = $injector.get('AuthenticationResult');
    var Error                = $injector.get('Error');

    // Required services
    var $rootScope          = $injector.get('$rootScope');
    var localStorageService = $injector.get('localStorageService');
    var requestService      = $injector.get('requestService');

    var service = {};

    /**
     * The most recent authentication result, or null if no authentication
     * result is cached.
     *
     * @type AuthenticationResult
     */
    var cachedResult = null;

    /**
     * The unique identifier of the local storage key which stores the result
     * of the last authentication attempt.
     *
     * @type String
     */
    var AUTH_STORAGE_KEY = 'GUAC_AUTH';

    /**
     * Retrieves the last successful authentication result. If the user has not
     * yet authenticated, the user has logged out, or the last authentication
     * attempt failed, null is returned.
     *
     * @returns {AuthenticationResult}
     *     The last successful authentication result, or null if the user is not
     *     currently authenticated.
     */
    var getAuthenticationResult = function getAuthenticationResult() {

        // Use cached result, if any
        if (cachedResult)
            return cachedResult;

        // Return explicit null if no auth data is currently stored
        var data = localStorageService.getItem(AUTH_STORAGE_KEY);
        if (!data)
            return null;

        // Update cache and return retrieved auth result
        return (cachedResult = new AuthenticationResult(data));

    };

    /**
     * Stores the given authentication result for future retrieval. The given
     * result MUST be the result of the most recent authentication attempt.
     *
     * @param {AuthenticationResult} data
     *     The last successful authentication result, or null if the last
     *     authentication attempt failed.
     */
    var setAuthenticationResult = function setAuthenticationResult(data) {

        // Clear the currently-stored result if the last attempt failed
        if (!data) {
            cachedResult = null;
            localStorageService.removeItem(AUTH_STORAGE_KEY);
        }

        // Otherwise store the authentication attempt directly
        else {

            // Always store in cache
            cachedResult = data;

            // Persist result past tab/window closure ONLY if not anonymous
            if (data.username !== AuthenticationResult.ANONYMOUS_USERNAME)
                localStorageService.setItem(AUTH_STORAGE_KEY, data);

        }

    };

    /**
     * Clears the stored authentication result, if any. If no authentication
     * result is currently stored, this function has no effect.
     */
    var clearAuthenticationResult = function clearAuthenticationResult() {
        setAuthenticationResult(null);
    };

    /**
     * Makes a request to authenticate a user using the token REST API endpoint
     * and given arbitrary parameters, returning a promise that succeeds only
     * if the authentication operation was successful. The resulting
     * authentication data can be retrieved later via getCurrentToken() or
     * getCurrentUsername().
     * 
     * The provided parameters can be virtually any object, as each property
     * will be sent as an HTTP parameter in the authentication request.
     * Standard parameters include "username" for the user's username,
     * "password" for the user's associated password, and "token" for the
     * auth token to check/update.
     * 
     * If a token is provided, it will be reused if possible.
     * 
     * @param {Object} parameters 
     *     Arbitrary parameters to authenticate with.
     *
     * @returns {Promise}
     *     A promise which succeeds only if the login operation was successful.
     */
    service.authenticate = function authenticate(parameters) {

        // Attempt authentication
        return requestService({
            method: 'POST',
            url: 'api/tokens',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            data: $.param(parameters)
        })

        // If authentication succeeds, handle received auth data
        .then(function authenticationSuccessful(data) {

            var currentToken = service.getCurrentToken();

            // If a new token was received, ensure the old token is invalidated,
            // if any, and notify listeners of the new token
            if (data.authToken !== currentToken) {

                // If an old token existed, request that the token be revoked
                if (currentToken) {
                    service.revokeToken(currentToken).catch(angular.noop);
                }

                // Notify of login and new token
                setAuthenticationResult(new AuthenticationResult(data));
                $rootScope.$broadcast('guacLogin', data.authToken);

            }

            // Update cached authentication result, even if the token remains
            // the same
            else
                setAuthenticationResult(new AuthenticationResult(data));

            // Authentication was successful
            return data;

        })

        // If authentication fails, propogate failure to returned promise
        ['catch'](requestService.createErrorCallback(function authenticationFailed(error) {

            // Request credentials if provided credentials were invalid
            if (error.type === Error.Type.INVALID_CREDENTIALS)
                $rootScope.$broadcast('guacInvalidCredentials', parameters, error);

            // Request more credentials if provided credentials were not enough 
            else if (error.type === Error.Type.INSUFFICIENT_CREDENTIALS)
                $rootScope.$broadcast('guacInsufficientCredentials', parameters, error);

            // Abort rendering of page if an internal error occurs
            else if (error.type === Error.Type.INTERNAL_ERROR)
                $rootScope.$broadcast('guacFatalPageError', error);

            // Authentication failed
            throw error;

        }));

    };

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
     * @param {Object} [parameters]
     *     Arbitrary parameters to authenticate with, if any.
     *
     * @returns {Promise}
     *     A promise which succeeds only if the login operation was successful.
     */
    service.updateCurrentToken = function updateCurrentToken(parameters) {

        // HTTP parameters for the authentication request
        var httpParameters = {};

        // Add token parameter if current token is known
        var token = service.getCurrentToken();
        if (token)
            httpParameters.token = service.getCurrentToken();

        // Add any additional parameters
        if (parameters)
            angular.extend(httpParameters, parameters);

        // Make the request
        return service.authenticate(httpParameters);

    };

    /**
     * Makes a request to revoke an authentication token using the token REST
     * API endpoint, returning a promise that succeeds only if the token was
     * successfully revoked.
     *
     * @param {string} token
     *     The authentication token to revoke.
     *
     * @returns {Promise}
     *     A promise which succeeds only if the token was successfully revoked.
     */
    service.revokeToken = function revokeToken(token) {
        return requestService({
            method: 'DELETE',
            url: 'api/tokens/' + token
        });
    };

    /**
     * Makes a request to authenticate a user using the token REST API endpoint
     * with a username and password, ignoring any currently-stored token, 
     * returning a promise that succeeds only if the login operation was
     * successful. The resulting authentication data can be retrieved later
     * via getCurrentToken() or getCurrentUsername().
     * 
     * @param {String} username
     *     The username to log in with.
     *
     * @param {String} password
     *     The password to log in with.
     *
     * @returns {Promise}
     *     A promise which succeeds only if the login operation was successful.
     */
    service.login = function login(username, password) {
        return service.authenticate({
            username: username,
            password: password
        });
    };

    /**
     * Makes a request to logout a user using the token REST API endpoint,
     * returning a promise that succeeds only if the logout operation was
     * successful.
     * 
     * @returns {Promise}
     *     A promise which succeeds only if the logout operation was
     *     successful.
     */
    service.logout = function logout() {
        
        // Clear authentication data
        var token = service.getCurrentToken();
        clearAuthenticationResult();

        // Notify listeners that a token is being destroyed
        $rootScope.$broadcast('guacLogout', token);

        // Delete old token
        return service.revokeToken(token);

    };

    /**
     * Returns whether the current user has authenticated anonymously. An
     * anonymous user is denoted by the identifier reserved by the Guacamole
     * extension API for anonymous users (the empty string).
     *
     * @returns {Boolean}
     *     true if the current user has authenticated anonymously, false
     *     otherwise.
     */
    service.isAnonymous = function isAnonymous() {
        return service.getCurrentUsername() === '';
    };

    /**
     * Returns the username of the current user. If the current user is not
     * logged in, this value may not be valid.
     *
     * @returns {String}
     *     The username of the current user, or null if no authentication data
     *     is present.
     */
    service.getCurrentUsername = function getCurrentUsername() {

        // Return username, if available
        var authData = getAuthenticationResult();
        if (authData)
            return authData.username;

        // No auth data present
        return null;

    };

    /**
     * Returns the auth token associated with the current user. If the current
     * user is not logged in, this token may not be valid.
     *
     * @returns {String}
     *     The auth token associated with the current user, or null if no
     *     authentication data is present.
     */
    service.getCurrentToken = function getCurrentToken() {

        // Return auth token, if available
        var authData = getAuthenticationResult();
        if (authData)
            return authData.authToken;

        // No auth data present
        return null;

    };

    /**
     * Returns the identifier of the data source that authenticated the current
     * user. If the current user is not logged in, this value may not be valid.
     *
     * @returns {String}
     *     The identifier of the data source that authenticated the current
     *     user, or null if no authentication data is present.
     */
    service.getDataSource = function getDataSource() {

        // Return data source, if available
        var authData = getAuthenticationResult();
        if (authData)
            return authData.dataSource;

        // No auth data present
        return null;

    };

    /**
     * Returns the identifiers of all data sources available to the current
     * user. If the current user is not logged in, this value may not be valid.
     *
     * @returns {String[]}
     *     The identifiers of all data sources availble to the current user,
     *     or an empty array if no authentication data is present.
     */
    service.getAvailableDataSources = function getAvailableDataSources() {

        // Return data sources, if available
        var authData = getAuthenticationResult();
        if (authData)
            return authData.availableDataSources;

        // No auth data present
        return [];

    };

    return service;
}]);
