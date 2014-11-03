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
 * A service for performing useful user related functionaltiy.
 */
angular.module('user').factory('userService', ['$injector', function userService($injector) {
            
    var permissionCheckService          = $injector.get('permissionCheckService');
            
    var service = {};
    
    /**
     * Filters the list of users using the provided permissions.
     * 
     * @param {array} users The user list.
     * 
     * @param {object} permissionList The list of permissions to use 
     *                                when filtering.
     * 
     * @param {object} permissionCriteria The required permission for each user.
     *                          
     * @return {array} The filtered list.
     */
    service.filterUsersByPermission = function filterUsersByPermission(users, permissionList, permissionCriteria) {
        for(var i = 0; i < users.length; i++) {
            if(!permissionCheckService.checkPermission(permissionList, 
                    "USER", user.username, permissionCriteria)) {
                items.splice(i, 1);
                continue;
            } 
        }
        
        return users;
    };
    
    return service;
}]);
