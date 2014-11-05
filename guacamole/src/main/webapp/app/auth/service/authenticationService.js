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
angular.module('auth').factory('authenticationService', ['$http', '$injector',
        function authenticationService($http, $injector) {

    var localStorageUtility = $injector.get("localStorageUtility");
    var service = {};
    
    /**
     * Makes a request to authenticate a user using the token REST API endpoint, 
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {String} username The username to log in with.
     * @param {String} password The password to log in with.
     * @returns {Promise} A promise for the HTTP call.
     */
    service.login = function login(username, password) {
        return $http({
            method: 'POST',
            url: 'api/token',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            data: $.param({
                username: username,
                password: password
            })
        }).success(function success(data, status, headers, config) {
            localStorageUtility.set('authToken', data.authToken);
            localStorageUtility.set('userID', data.userID);
        });
    };

    /**
     * Makes a request to logout a user using the login REST API endpoint, 
     * returning a promise that can be used for processing the results of the call.
     * 
     * @returns {Promise} A promise for the HTTP call.
     */
    service.logout = function logout() {
        return $http({
            method: 'DELETE',
            url: 'api/token/' + encodeURIComponent(service.getCurrentToken())
        });
    };

    /**
     * Returns the user ID of the current user.
     *
     * @returns {String} The user ID of the current user.
     */
    service.getCurrentUserID = function getCurrentUserID() {
        return localStorageUtility.get('userID');
    };

    /**
     * Returns the auth token associated with the current user. If the current
     * user is not logged in, this token may not be valid.
     *
     * @returns {String} The auth token associated with the current user.
     */
    service.getCurrentToken = function getCurrentToken() {
        return localStorageUtility.get('authToken');
    };
    
    return service;
}]);
