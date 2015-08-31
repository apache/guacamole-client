/*
 * Copyright (C) 2015 Glyptodon LLC
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
    var $q                    = $injector.get('$q');
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
     * @param {String} dataSource
     *     The unique identifier of the data source containing the users to be
     *     retrieved. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
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
    service.getUsers = function getUsers(dataSource, permissionTypes) {

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
            url     : 'api/data/' + encodeURIComponent(dataSource) + '/users',
            params  : httpParameters
        });

    };

    /**
     * Returns a promise which resolves with all users accessible by the
     * current user, as a map of @link{User} arrays by the identifier of their
     * corresponding data source. All given data sources are queried. If an
     * error occurs while retrieving any user, the promise will be rejected.
     *
     * @param {String[]} dataSources
     *     The unique identifier of the data sources containing the user to be
     *     retrieved. These identifiers correspond to AuthenticationProviders
     *     within the Guacamole web application.
     *
     * @param {String[]} [permissionTypes]
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for a user to appear in the result.
     *     If null, no filtering will be performed. Valid values are listed
     *     within PermissionSet.ObjectType.
     *
     * @returns {Promise.<Object.<String, User[]>>}
     *     A promise which resolves with all user objects available to the
     *     current user, as a map of @link{User} arrays grouped by the
     *     identifier of their corresponding data source.
     */
    service.getAllUsers = function getAllUsers(dataSources, permissionTypes) {

        var deferred = $q.defer();

        var userRequests = [];
        var userArrays = {};

        // Retrieve all users from all data sources
        angular.forEach(dataSources, function retrieveUsers(dataSource) {
            userRequests.push(
                service.getUsers(dataSource, permissionTypes)
                .success(function usersRetrieved(users) {
                    userArrays[dataSource] = users;
                })
            );
        });

        // Resolve when all requests are completed
        $q.all(userRequests)
        .then(

            // All requests completed successfully
            function allUsersRetrieved() {
                deferred.resolve(userArrays);
            },

            // At least one request failed
            function userRetrievalFailed(e) {
                deferred.reject(e);
            }

        );

        return deferred.promise;

    };

    /**
     * Makes a request to the REST API to get the user having the given
     * username, returning a promise that provides the corresponding
     * @link{User} if successful.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user to be
     *     retrieved. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param {String} username
     *     The username of the user to retrieve.
     * 
     * @returns {Promise.<User>}
     *     A promise which will resolve with a @link{User} upon success.
     */
    service.getUser = function getUser(dataSource, username) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve user
        return $http({
            cache   : cacheService.users,
            method  : 'GET',
            url     : 'api/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(username),
            params  : httpParameters
        });

    };
    
    /**
     * Makes a request to the REST API to delete a user, returning a promise
     * that can be used for processing the results of the call.
     * 
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user to be
     *     deleted. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param {User} user
     *     The user to delete.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteUser = function deleteUser(dataSource, user) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Delete user
        return $http({
            method  : 'DELETE',
            url     : 'api/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(user.username),
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
     * @param {String} dataSource
     *     The unique identifier of the data source in which the user should be
     *     created. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param {User} user
     *     The user to create.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     create operation is successful.
     */
    service.createUser = function createUser(dataSource, user) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Create user
        return $http({
            method  : 'POST',
            url     : 'api/data/' + encodeURIComponent(dataSource) + '/users',
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
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user to be
     *     updated. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param {User} user
     *     The user to update.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    service.saveUser = function saveUser(dataSource, user) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Update user
        return $http({
            method  : 'PUT',
            url     : 'api/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(user.username),
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
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user to be
     *     updated. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
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
    service.updateUserPassword = function updateUserPassword(dataSource, username,
            oldPassword, newPassword) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Update user password
        return $http({
            method  : 'PUT',
            url     : 'api/data/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(username) + '/password',
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
