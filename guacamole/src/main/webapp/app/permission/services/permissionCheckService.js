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
 * A service for checking if a specific permission exists 
 * in a given list of permissions.
 */
angular.module('permission').factory('permissionCheckService', [
        function permissionCheckService() {
            
    var service = {};
    
    /**
     * A service for checking if the given permission list contains the given
     * permission, defined by the objectType, objectID, and permissionType.
     * If the objectType or objectID are not passed, they will not be checked.
     * 
     * For example, checkPermission(list, "CONNECTION", undefined, "READ") would
     * check if the permission list contains permission to read any connection.
     * 
     * @param {array} permissions The array of permissions to check.
     * @param {string} objectType The object type for the permission.
     *                            If not passed, this will not be checked.
     * @param {string} objectID The ID of the object the permission is for. 
     *                          If not passed, this will not be checked.
     * @param {string} permissionType The actual permission type to check for.
     * @returns {boolean} True if the given permissions contain the requested permission, false otherwise.
     */
    service.checkPermission = function checkPermission(permissions, objectType, objectID, permissionType) {
        
        // Loop through all the permissions and check if any of them match the given parameters
        for(var i = 0; i < permissions.length; i++) {
            var permission = permissions[i];
            
            if(objectType === "SYSTEM") {
                // System permissions have no object ID, we only need to check the type.
                if(permission.permissionType === permissionType)
                    return true;
            }
            else {
                // Object permissions need to match the object ID and type if given.
                if(permission.permissionType === permissionType && 
                        (!objectType || permission.objectType === objectType) && 
                        (!objectID || permission.objectID === objectID))
                    return true;
            }
        }
        
        // Didn't find any that matched
        return false;
    }
    
    return service;
}]);
