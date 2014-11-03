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
 * The DAO for connection operations agains the REST API.
 */
angular.module('user').factory('userDAO', ['$http', 'localStorageUtility',
        function userDAO($http, localStorageUtility) {
            
    var service = {};
    
    /**
     * Makes a request to the REST API to get the list of users,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @returns {promise} A promise for the HTTP call.
     */
    service.getUsers = function getUsers() {
        return $http.get("api/user?token=" + localStorageUtility.get('authToken'));
    };
    
    /**
     * Makes a request to the REST API to get the list of users,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {string} userID The ID of the user to retrieve.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.getUser = function getUser(userID) {
        return $http.get("api/user/" + userID + "/?token=" + localStorageUtility.get('authToken'));
    };
    
    /**
     * Makes a request to the REST API to delete a user,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {object} user The user to delete.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.deleteUser = function deleteUser(user) {
        return $http['delete'](
            "api/user/" + user.username + 
            "?token=" + localStorageUtility.get('authToken'));
    };
    
    
    /**
     * Makes a request to the REST API to create a user,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {object} user The user to create.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.createUser = function createUser(user) {
        return $http.post(
            "api/user/" 
            + "?token=" + localStorageUtility.get('authToken'), 
            user
        );
    }
    
    /**
     * Makes a request to the REST API to save a user,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {object} user The user to update.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.saveUser = function saveUser(user) {
        return $http.post(
            "api/user/" + user.username + 
            "?token=" + localStorageUtility.get('authToken'), 
        user);
    };
    
    return service;
}]);
