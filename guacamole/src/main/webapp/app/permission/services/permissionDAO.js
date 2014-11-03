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
 * The DAO for permission operations agains the REST API.
 */
angular.module('permission').factory('permissionDAO', ['$http', 'localStorageUtility',
        function permissionDAO($http, localStorageUtility) {
            
    var service = {};
    
    /**
     * Makes a request to the REST API to get the list of permissions for a given user,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {string} userID The ID of the user to retrieve the permissions for.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.getPermissions = function getPermissions(userID) {
        return $http.get("api/permission/" + userID + "/?token=" + localStorageUtility.get('authToken'));
    };
    
    /**
     * Makes a request to the REST API to add a permission for a given user,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {string} userID The ID of the user to add the permission for.
     * @param {object} permission The permission to add.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.addPermission = function addPermission(userID, permission) {
        return $http.post("api/permission/" + userID + "/?token=" + localStorageUtility.get('authToken'), permission);
    };
    
    
    
    /**
     * Makes a request to the REST API to remove a permission for a given user,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {string} userID The ID of the user to remove the permission for.
     * @param {object} permission The permission to remove.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.removePermission = function removePermission(userID, permission) {
        return $http.post("api/permission/remove/" + userID + "/?token=" + localStorageUtility.get('authToken'), permission);
    };
    
    
    /**
     * Makes a request to the REST API to modify the permissions for a given user,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {string} userID The ID of the user to remove the permission for.
     * @param {array} permissionsToAdd The permissions to add.
     * @param {array} permissionsToRemove The permissions to remove.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.patchPermissions = function patchPermissions(userID, permissionsToAdd, permissionsToRemove) {
        var permissionPatch = [];
        
        // Add all the add operations to the patch
        for(var i = 0; i < permissionsToAdd.length; i++ ) {
            permissionPatch.push({
                op      : "add",
                path    : userID, 
                value   : permissionsToAdd[i]
            });
        }
        
        // Add all the remove operations to the patch
        for(var i = 0; i < permissionsToRemove.length; i++ ) {
            permissionPatch.push({
                op      : "remove",
                path    : userID, 
                value   : permissionsToRemove[i]
            });
        }
        
        // Make the HTTP call
        return $http({
            method  : 'PATCH', 
            url     : "api/permission/?token=" + localStorageUtility.get('authToken'),
            data    : permissionPatch
        });
    }
    
    
    
    
    return service;
}]);
