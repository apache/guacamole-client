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
 * Service for operating on user permissions via the REST API.
 */
angular.module('rest').factory('permissionService', ['$http', 'authenticationService',
        function permissionService($http, authenticationService) {
            
    var service = {};
    
    /**
     * Makes a request to the REST API to get the list of permissions for a
     * given user, returning a promise that provides an array of
     * @link{Permission} objects if successful.

     * 
     * @param {String} userID
     *     The ID of the user to retrieve the permissions for.
     *                          
     * @returns {Promise.<PermissionSet>}
     *     A promise which will resolve with a @link{PermissionSet} upon
     *     success.
     */
    service.getPermissions = function getPermissions(userID) {
        return $http.get("api/user/" + userID + "/permissions?token=" + authenticationService.getCurrentToken());
    };
    
    /**
     * Makes a request to the REST API to add permissions for a given user,
     * returning a promise that can be used for processing the results of the
     * call.
     * 
     * @param {String} userID The ID of the user to add the permission for.
     * @param {PermissionSet} permissions The permissions to add.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     add operation is successful.
     */
    service.addPermissions = function addPermissions(userID, permissions) {
        return service.patchPermissions(userID, permissions, null);
    };
    
    /**
     * Makes a request to the REST API to remove permissions for a given user,
     * returning a promise that can be used for processing the results of the
     * call.
     * 
     * @param {String} userID The ID of the user to remove the permission for.
     * @param {PermissionSet} permissions The permissions to remove.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     remove operation is successful.
     */
    service.removePermissions = function removePermissions(userID, permissions) {
        return service.patchPermissions(userID, null, permissions);
    };
    
    /**
     * Makes a request to the REST API to modify the permissions for a given
     * user, returning a promise that can be used for processing the results of
     * the call.
     * 
     * @param {String} userID The ID of the user to remove the permission for.
     * @param {PermissionSet} [permissionsToAdd] The permissions to add.
     * @param {PermissionSet} [permissionsToRemove] The permissions to remove.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     patch operation is successful.
     */
    service.patchPermissions = function patchPermissions(userID, permissionsToAdd, permissionsToRemove) {

        // FIXME: This will NOT work, now that PermissionSet is used

        var i;
        var permissionPatch = [];
        
        // Add all the add operations to the patch
        for (i = 0; i < permissionsToAdd.length; i++ ) {
            permissionPatch.push({
                op      : "add",
                path    : userID, 
                value   : permissionsToAdd[i]
            });
        }
        
        // Add all the remove operations to the patch
        for (i = 0; i < permissionsToRemove.length; i++ ) {
            permissionPatch.push({
                op      : "remove",
                path    : userID, 
                value   : permissionsToRemove[i]
            });
        }
        
        // Make the HTTP call
        return $http({
            method  : 'PATCH', 
            url     : "api/permission/?token=" + authenticationService.getCurrentToken(),
            data    : permissionPatch
        });

    };
    
    return service;

}]);
