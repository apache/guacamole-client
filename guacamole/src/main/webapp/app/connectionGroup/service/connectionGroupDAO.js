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
 * The DAO for connection group operations agains the REST API.
 */
angular.module('connectionGroup').factory('connectionGroupDAO', ['$http', 'localStorageUtility',
        function connectionGrouDAO($http, localStorageUtility) {
            
    /**
     * The ID of the root connection group.
     */
    var ROOT_CONNECTION_GROUP_ID = "ROOT";
            
    var service = {};
    
    /**
     * Makes a request to the REST API to get the list of connection groups,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {string} parentID The parent ID for the connection group.
     *                          If not passed in, it will query a list of the 
     *                          connection groups in the root group.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.getConnectionGroups = function getConnectionGroups(parentID) {
        
        var parentIDParam = "";
        if(parentID !== undefined)
            parentIDParam = "&parentID=" + parentID;
        
        return $http.get("api/connectionGroup?token=" + localStorageUtility.get('authToken') + parentIDParam);
    };
    
    /**
     * Makes a request to the REST API to get an individual connection group,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {string} connectionGroupID The ID for the connection group.
     *                                   If not passed in, it will query the
     *                                   root connection group.
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.getConnectionGroup = function getConnectionGroup(connectionGroupID) {
        
        // Use the root connection group ID if no ID is passed in
        connectionGroupID = connectionGroupID || ROOT_CONNECTION_GROUP_ID;
        
        return $http.get("api/connectionGroup/" + connectionGroupID + "?token=" + localStorageUtility.get('authToken'));
    };
    
    /**
     * Makes a request to the REST API to save a connection group,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {object} connectionGroup The connection group to update
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.saveConnectionGroup = function saveConnectionGroup(connectionGroup) {
        // This is a new connection group
        if(!connectionGroup.identifier) {
            return $http.post("api/connectionGroup/?token=" + localStorageUtility.get('authToken'), connectionGroup).success(
                function setConnectionGroupID(connectionGroupID){
                    // Set the identifier on the new connection
                    connectionGroup.identifier = connectionGroupID;
                    return connectionGroupID;
                });
        } else {
            return $http.post(
                "api/connectionGroup/" + connectionGroup.identifier + 
                "?token=" + localStorageUtility.get('authToken'), 
            connectionGroup);
        }
    };
    
    /**
     * Makes a request to the REST API to move a connection group to a different group,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {object} connectionGroup The connection group to move. 
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.moveConnectionGroup = function moveConnectionGroup(connectionGroup) {
        
        return $http.put(
            "api/connectionGroup/" + connectionGroup.identifier + 
            "?token=" + localStorageUtility.get('authToken') + 
            "&parentID=" + connectionGroup.parentIdentifier, 
        connectionGroup);
    };
    
    /**
     * Makes a request to the REST API to delete a connection group,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {object} connectionGroup The connection group to delete
     *                          
     * @returns {promise} A promise for the HTTP call.
     */
    service.deleteConnectionGroup = function deleteConnectionGroup(connectionGroup) {
        return $http['delete'](
            "api/connectionGroup/" + connectionGroup.identifier + 
            "?token=" + localStorageUtility.get('authToken'));
    };
    
    return service;
}]);
