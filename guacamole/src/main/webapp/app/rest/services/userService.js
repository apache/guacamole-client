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
 * Service for operating on users via the REST API.
 */
angular.module('rest').factory('userService', ['$http', 'authenticationService',
        function userService($http, authenticationService) {
            
    var service = {};
    
    /**
     * Makes a request to the REST API to get the list of users,
     * returning a promise that provides an array of @link{User} objects if
     * successful.
     * 
     * @param {String} [permissionType]
     *     The permission type string of the permission that the current user
     *     must have for a given user to appear within the list. Valid values
     *     are listed within PermissionSet.ObjectType.
     *                          
     * @returns {Promise.<User[]>}
     *     A promise which will resolve with an array of @link{User} objects
     *     upon success.
     */
    service.getUsers = function getUsers(permissionType) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Add permission filter if specified
        if (permissionType)
            httpParameters.permission = permissionType;

        // Retrieve users
        return $http({
            method  : 'GET',
            url     : 'api/user',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the user having the given ID,
     * returning a promise that provides the corresponding @link{User} if
     * successful.
     * 
     * @param {String} userID The ID of the user to retrieve.
     * 
     * @returns {Promise.<User>}
     *     A promise which will resolve with a @link{User} upon success.
     */
    service.getUser = function getUser(userID) {
        return $http.get("api/user/" + userID + "/?token=" + authenticationService.getCurrentToken());
    };
    
    /**
     * Makes a request to the REST API to delete a user, returning a promise
     * that can be used for processing the results of the call.
     * 
     * @param {User} user The user to delete.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteUser = function deleteUser(user) {
        return $http['delete'](
            "api/user/" + user.username + 
            "?token=" + authenticationService.getCurrentToken());
    };
    
    
    /**
     * Makes a request to the REST API to create a user, returning a promise
     * that can be used for processing the results of the call.
     * 
     * @param {User} user The user to create.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     create operation is successful.
     */
    service.createUser = function createUser(user) {
        return $http.post(
            "api/user/" 
            + "?token=" + authenticationService.getCurrentToken(), 
            user
        );
    };
    
    /**
     * Makes a request to the REST API to save a user, returning a promise that
     * can be used for processing the results of the call.
     * 
     * @param {User} user The user to update.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    service.saveUser = function saveUser(user) {
        return $http.post(
            "api/user/" + user.username + 
            "?token=" + authenticationService.getCurrentToken(), 
        user);
    };
    
    return service;

}]);
