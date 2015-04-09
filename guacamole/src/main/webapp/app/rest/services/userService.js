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
angular.module('rest').factory('userService', ['$injector',
        function userService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');

    // Get required types
    var UserPasswordUpdate = $injector.get("UserPasswordUpdate");
            
    var service = {};
    
    /**
     * Makes a request to the REST API to get the list of users,
     * returning a promise that provides an array of @link{User} objects if
     * successful.
     * 
     * @param {String[]} [permissionTypes]
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for a user to appear in the result. 
     *     If null, no filtering will be performed. Valid values are listed
     *     within PermissionSet.ObjectType.
     *                          
     * @returns {Promise.<User[]>}
     *     A promise which will resolve with an array of @link{User} objects
     *     upon success.
     */
    service.getUsers = function getUsers(permissionTypes) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Add permission filter if specified
        if (permissionTypes)
            httpParameters.permission = permissionTypes;

        // Retrieve users
        return $http({
            cache   : cacheService.users,
            method  : 'GET',
            url     : 'api/users',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the user having the given
     * username, returning a promise that provides the corresponding
     * @link{User} if successful.
     * 
     * @param {String} username
     *     The username of the user to retrieve.
     * 
     * @returns {Promise.<User>}
     *     A promise which will resolve with a @link{User} upon success.
     */
    service.getUser = function getUser(username) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve user
        return $http({
            cache   : cacheService.users,
            method  : 'GET',
            url     : 'api/users/' + encodeURIComponent(username),
            params  : httpParameters
        });

    };
    
    /**
     * Makes a request to the REST API to delete a user, returning a promise
     * that can be used for processing the results of the call.
     * 
     * @param {User} user
     *     The user to delete.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteUser = function deleteUser(user) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Delete user
        return $http({
            method  : 'DELETE',
            url     : 'api/users/' + encodeURIComponent(user.username),
            params  : httpParameters
        })

        // Clear the cache
        .success(function userDeleted(){
            cacheService.users.removeAll();
        });


    };
    
    /**
     * Makes a request to the REST API to create a user, returning a promise
     * that can be used for processing the results of the call.
     * 
     * @param {User} user
     *     The user to create.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     create operation is successful.
     */
    service.createUser = function createUser(user) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Create user
        return $http({
            method  : 'POST',
            url     : 'api/users',
            params  : httpParameters,
            data    : user
        })

        // Clear the cache
        .success(function userCreated(){
            cacheService.users.removeAll();
        });

    };
    
    /**
     * Makes a request to the REST API to save a user, returning a promise that
     * can be used for processing the results of the call.
     * 
     * @param {User} user
     *     The user to update.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    service.saveUser = function saveUser(user) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Update user
        return $http({
            method  : 'PUT',
            url     : 'api/users/' + encodeURIComponent(user.username),
            params  : httpParameters,
            data    : user
        })

        // Clear the cache
        .success(function userUpdated(){
            cacheService.users.removeAll();
        });

    };
    
    /**
     * Makes a request to the REST API to update the password for a user, 
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {String} username
     *     The username of the user to update.
     *     
     * @param {String} oldPassword
     *     The exiting password of the user to update.
     *     
     * @param {String} newPassword
     *     The new password of the user to update.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     password update operation is successful.
     */
    service.updateUserPassword = function updateUserPassword(username, 
            oldPassword, newPassword) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Update user password
        return $http({
            method  : 'PUT',
            url     : 'api/users/' + encodeURIComponent(username) + '/password',
            params  : httpParameters,
            data    : new UserPasswordUpdate({
                oldPassword : oldPassword,
                newPassword : newPassword
            })
        })

        // Clear the cache
        .success(function passwordChanged(){
            cacheService.users.removeAll();
        });

    };
    
    return service;

}]);
