/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * A service for authenticating a user against the REST API.
 */
angular.module('auth').factory('authenticationService', ['$injector',
        function authenticationService($injector) {

    // Required services
    var $cookieStore = $injector.get('$cookieStore');
    var $http        = $injector.get('$http');
    var $q           = $injector.get('$q');

    var service = {};

    /**
     * The unique identifier of the local cookie which stores the user's
     * current authentication token and user ID.
     *
     * @type String
     */
    var AUTH_COOKIE_ID = "GUAC_AUTH";

    /**
     * Makes a request to authenticate a user using the token REST API endpoint
     * and given arbitrary parameters, returning a promise that succeeds only
     * if the authentication operation was successful. The resulting
     * authentication data can be retrieved later via getCurrentToken() or
     * getCurrentUserID().
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

        var authenticationProcess = $q.defer();

        /**
         * Stores the given authentication data within the browser and marks
         * the authentication process as completed.
         *
         * @param {Object} data
         *     The authentication data returned by the token REST endpoint.
         */
        var completeAuthentication = function completeAuthentication(data) {

            // Store auth data
            $cookieStore.put(AUTH_COOKIE_ID, {
                authToken : data.authToken,
                userID    : data.userID
            });

            // Process is complete
            authenticationProcess.resolve();

        };

        // Attempt authentication
        $http({
            method: 'POST',
            url: 'api/tokens',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            data: $.param(parameters),
        })

        // If authentication succeeds, handle received auth data
        .success(function authenticationSuccessful(data) {

            var currentToken = service.getCurrentToken();

            // If a new token was received, ensure the old token is invalidated
            if (currentToken && data.authToken !== currentToken) {
                service.logout()
                ['finally'](function logoutComplete() {
                    completeAuthentication(data);
                });
            }

            // Otherwise, just finish the auth process
            else
                completeAuthentication(data);

        })

        // If authentication fails, propogate failure to returned promise
        .error(function authenticationFailed() {
            authenticationProcess.reject();
        });

        return authenticationProcess.promise;

    };

    /**
     * Makes a request to update the current auth token, if any, using the
     * token REST API endpoint. If the optional parameters object is provided,
     * its properties will be included as parameters in the update request.
     * This function returns a promise that succeeds only if the authentication
     * operation was successful. The resulting authentication data can be
     * retrieved later via getCurrentToken() or getCurrentUserID().
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
     * Makes a request to authenticate a user using the token REST API endpoint
     * with a username and password, ignoring any currently-stored token, 
     * returning a promise that succeeds only if the login operation was
     * successful. The resulting authentication data can be retrieved later
     * via getCurrentToken() or getCurrentUserID().
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
     * Makes a request to logout a user using the login REST API endpoint, 
     * returning a promise succeeds only if the logout operation was
     * successful.
     * 
     * @returns {Promise}
     *     A promise which succeeds only if the logout operation was
     *     successful.
     */
    service.logout = function logout() {

        // Clear authentication data
        var token = service.getCurrentToken();
        $cookieStore.remove(AUTH_COOKIE_ID);

        // Delete old token
        return $http({
            method: 'DELETE',
            url: 'api/tokens/' + token
        });

    };

    /**
     * Returns the user ID of the current user. If the current user is not
     * logged in, this ID may not be valid.
     *
     * @returns {String}
     *     The user ID of the current user, or null if no authentication data
     *     is present.
     */
    service.getCurrentUserID = function getCurrentUserID() {

        // Return user ID, if available
        var authData = $cookieStore.get(AUTH_COOKIE_ID);
        if (authData)
            return authData.userID;

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
        var authData = $cookieStore.get(AUTH_COOKIE_ID);
        if (authData)
            return authData.authToken;

        // No auth data present
        return null;

    };
    
    return service;
}]);
