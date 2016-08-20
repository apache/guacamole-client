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
 * Service for operating on user permissions via the REST API.
 */
angular.module('rest').factory('permissionService', ['$injector',
        function permissionService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var $q                    = $injector.get('$q');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');
    
    // Required types
    var PermissionPatch = $injector.get('PermissionPatch');

    var service = {};

    /**
     * Returns the URL for the REST resource most appropriate for accessing
     * the permissions of the user having the given username.
     * 
     * It is important to note that a particular data source can authenticate
     * and provide permissions for a user, even if that user does not exist
     * within that data source (and thus cannot be found beneath
     * "api/session/data/{dataSource}/users")
     *
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user whose
     *     permissions should be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String} username
     *     The username of the user for which the URL of the proper REST
     *     resource should be derived.
     *
     * @returns {String}
     *     The URL for the REST resource representing the user having the given
     *     username.
     */
    var getPermissionsResourceURL = function getPermissionsResourceURL(dataSource, username) {

        // Create base URL for data source
        var base = 'api/session/data/' + encodeURIComponent(dataSource);

        // If the username is that of the current user, do not rely on the
        // user actually existing (they may not). Access their permissions via
        // "self" rather than the collection of defined users.
        if (username === authenticationService.getCurrentUsername())
            return base + '/self/permissions';

        // Otherwise, the user must exist for their permissions to be
        // accessible. Use the collection of defined users.
        return base + '/users/' + encodeURIComponent(username) + '/permissions';

    };

    /**
     * Makes a request to the REST API to get the list of permissions for a
     * given user, returning a promise that provides an array of
     * @link{Permission} objects if successful.
     * 
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user whose
     *     permissions should be retrieved. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String} userID
     *     The ID of the user to retrieve the permissions for.
     *                          
     * @returns {Promise.<PermissionSet>}
     *     A promise which will resolve with a @link{PermissionSet} upon
     *     success.
     */
    service.getPermissions = function getPermissions(dataSource, userID) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve user permissions
        return $http({
            cache   : cacheService.users,
            method  : 'GET',
            url     : getPermissionsResourceURL(dataSource, userID),
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to add permissions for a given user,
     * returning a promise that can be used for processing the results of the
     * call.
     * 
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user whose
     *     permissions should be modified. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String} userID
     *     The ID of the user to modify the permissions of.
     *                          
     * @param {PermissionSet} permissions
     *     The set of permissions to add.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     add operation is successful.
     */
    service.addPermissions = function addPermissions(dataSource, userID, permissions) {
        return service.patchPermissions(dataSource, userID, permissions, null);
    };
    
    /**
     * Makes a request to the REST API to remove permissions for a given user,
     * returning a promise that can be used for processing the results of the
     * call.
     * 
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user whose
     *     permissions should be modified. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String} userID
     *     The ID of the user to modify the permissions of.
     *                          
     * @param {PermissionSet} permissions
     *     The set of permissions to remove.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     remove operation is successful.
     */
    service.removePermissions = function removePermissions(dataSource, userID, permissions) {
        return service.patchPermissions(dataSource, userID, null, permissions);
    };

    /**
     * Adds patches for modifying the permissions associated with specific
     * objects to the given array of patches.
     *
     * @param {PermissionPatch[]} patch
     *     The array of patches to add new patches to.
     *
     * @param {String} operation
     *     The operation to specify within each of the patches. Valid values
     *     for this are defined within PermissionPatch.Operation.
     *     
     * @param {String} path
     *     The path of the permissions being patched. The path is a JSON path
     *     describing the position of the permissions within a PermissionSet.
     *
     * @param {Object.<String, String[]>} permissions
     *     A map of object identifiers to arrays of permission type strings,
     *     where each type string is a value from
     *     PermissionSet.ObjectPermissionType.
     */
    var addObjectPatchOperations = function addObjectPatchOperations(patch, operation, path, permissions) {

        // Add object permission operations to patch
        for (var identifier in permissions) {
            permissions[identifier].forEach(function addObjectPatch(type) {
                patch.push({
                    op    : operation,
                    path  : path + "/" + identifier,
                    value : type
                });
            });
        }

    };

    /**
     * Adds patches for modifying any permission that can be stored within a
     * @link{PermissionSet}.
     * 
     * @param {PermissionPatch[]} patch
     *     The array of patches to add new patches to.
     *
     * @param {String} operation
     *     The operation to specify within each of the patches. Valid values
     *     for this are defined within PermissionPatch.Operation.
     *
     * @param {PermissionSet} permissions
     *     The set of permissions for which patches should be added.
     */
    var addPatchOperations = function addPatchOperations(patch, operation, permissions) {

        // Add connection permission operations to patch
        addObjectPatchOperations(patch, operation, "/connectionPermissions",
            permissions.connectionPermissions);

        // Add connection group permission operations to patch
        addObjectPatchOperations(patch, operation, "/connectionGroupPermissions",
            permissions.connectionGroupPermissions);

        // Add sharing profile permission operations to patch
        addObjectPatchOperations(patch, operation, "/sharingProfilePermissions",
            permissions.sharingProfilePermissions);

        // Add active connection permission operations to patch
        addObjectPatchOperations(patch, operation, "/activeConnectionPermissions",
            permissions.activeConnectionPermissions);

        // Add user permission operations to patch
        addObjectPatchOperations(patch, operation, "/userPermissions",
            permissions.userPermissions);

        // Add system operations to patch
        permissions.systemPermissions.forEach(function addSystemPatch(type) {
            patch.push({
                op    : operation,
                path  : "/systemPermissions",
                value : type
            });
        });

    };
            
    /**
     * Makes a request to the REST API to modify the permissions for a given
     * user, returning a promise that can be used for processing the results of
     * the call.
     * 
     * @param {String} dataSource
     *     The unique identifier of the data source containing the user whose
     *     permissions should be modified. This identifier corresponds to an
     *     AuthenticationProvider within the Guacamole web application.
     *
     * @param {String} userID
     *     The ID of the user to modify the permissions of.
     *                          
     * @param {PermissionSet} [permissionsToAdd]
     *     The set of permissions to add, if any.
     *
     * @param {PermissionSet} [permissionsToRemove]
     *     The set of permissions to remove, if any.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    service.patchPermissions = function patchPermissions(dataSource, userID, permissionsToAdd, permissionsToRemove) {

        var permissionPatch = [];
        
        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Add all the add operations to the patch
        addPatchOperations(permissionPatch, PermissionPatch.Operation.ADD, permissionsToAdd);

        // Add all the remove operations to the patch
        addPatchOperations(permissionPatch, PermissionPatch.Operation.REMOVE, permissionsToRemove);

        // Patch user permissions
        return $http({
            method  : 'PATCH', 
            url     : getPermissionsResourceURL(dataSource, userID),
            params  : httpParameters,
            data    : permissionPatch
        })
        
        // Clear the cache
        .success(function permissionsPatched(){
            cacheService.users.removeAll();
        });
    };
    
    return service;

}]);
