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
 * Service for operating on user groups via the REST API.
 */
angular.module('rest').factory('userGroupService', ['$injector',
        function userGroupService($injector) {

    // Required services
    var requestService        = $injector.get('requestService');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');

    var service = {};

    /**
     * Makes a request to the REST API to get the list of user groups,
     * returning a promise that provides an array of @link{UserGroup} objects if
     * successful.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user groups
     *     to be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String[]} [permissionTypes]
     *     The set of permissions to filter with. A user group must have one or
     *     more of these permissions for a user group to appear in the result.
     *     If null, no filtering will be performed. Valid values are listed
     *     within PermissionSet.ObjectType.
     *
     * @returns {Promise.<Object.<String, UserGroup>>}
     *     A promise which will resolve with a map of @link{UserGroup} objects
     *     where each key is the identifier of the corresponding user group.
     */
    service.getUserGroups = function getUserGroups(dataSource, permissionTypes) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Add permission filter if specified
        if (permissionTypes)
            httpParameters.permission = permissionTypes;

        // Retrieve user groups
        return requestService({
            cache   : cacheService.users,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the user group having the given
     * identifier, returning a promise that provides the corresponding
     * @link{UserGroup} if successful.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user group to
     *     be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String} identifier
     *     The identifier of the user group to retrieve.
     *
     * @returns {Promise.<UserGroup>}
     *     A promise which will resolve with a @link{UserGroup} upon success.
     */
    service.getUserGroup = function getUserGroup(dataSource, identifier) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve user group
        return requestService({
            cache   : cacheService.users,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(identifier),
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to delete a user group, returning a
     * promise that can be used for processing the results of the call.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user group to
     *     be deleted. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param {UserGroup} userGroup
     *     The user group to delete.
     *
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteUserGroup = function deleteUserGroup(dataSource, userGroup) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Delete user group
        return requestService({
            method  : 'DELETE',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(userGroup.identifier),
            params  : httpParameters
        })

        // Clear the cache
        .then(function userGroupDeleted(){
            cacheService.users.removeAll();
        });


    };

    /**
     * Makes a request to the REST API to create a user group, returning a promise
     * that can be used for processing the results of the call.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source in which the user group
     *     should be created. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {UserGroup} userGroup
     *     The user group to create.
     *
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     create operation is successful.
     */
    service.createUserGroup = function createUserGroup(dataSource, userGroup) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Create user group
        return requestService({
            method  : 'POST',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups',
            params  : httpParameters,
            data    : userGroup
        })

        // Clear the cache
        .then(function userGroupCreated(){
            cacheService.users.removeAll();
        });

    };

    /**
     * Makes a request to the REST API to save a user group, returning a
     * promise that can be used for processing the results of the call.
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user group to
     *     be updated. This identifier corresponds to an AuthenticationProvider
     *     within the Guacamole web application.
     *
     * @param {UserGroup} userGroup
     *     The user group to update.
     *
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    service.saveUserGroup = function saveUserGroup(dataSource, userGroup) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Update user group
        return requestService({
            method  : 'PUT',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/userGroups/' + encodeURIComponent(userGroup.identifier),
            params  : httpParameters,
            data    : userGroup
        })

        // Clear the cache
        .then(function userGroupUpdated(){
            cacheService.users.removeAll();
        });

    };

    return service;

}]);
