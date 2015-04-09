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
 * Service for operating on connection groups via the REST API.
 */
angular.module('rest').factory('connectionGroupService', ['$injector',
        function connectionGroupService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');
    
    // Required types
    var ConnectionGroup = $injector.get('ConnectionGroup');

    var service = {};
    
    /**
     * Makes a request to the REST API to get an individual connection group
     * and all descendants, returning a promise that provides the corresponding
     * @link{ConnectionGroup} if successful. Descendant groups and connections
     * will be stored as children of that connection group. If a permission
     * type is specified, the result will be filtering by that permission.
     * 
     * @param {String} [connectionGroupID=ConnectionGroup.ROOT_IDENTIFIER]
     *     The ID of the connection group to retrieve. If not provided, the
     *     root connection group will be retrieved by default.
     *     
     * @param {String[]} [permissionTypes]
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for a connection to appear in the result. 
     *     If null, no filtering will be performed. Valid values are listed
     *     within PermissionSet.ObjectType.
     *
     * @returns {Promise.ConnectionGroup}
     *     A promise which will resolve with a @link{ConnectionGroup} upon
     *     success.
     */
    service.getConnectionGroupTree = function getConnectionGroupTree(connectionGroupID, permissionTypes) {
        
        // Use the root connection group ID if no ID is passed in
        connectionGroupID = connectionGroupID || ConnectionGroup.ROOT_IDENTIFIER;

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Add permission filter if specified
        if (permissionTypes)
            httpParameters.permission = permissionTypes;

        // Retrieve connection group 
        return $http({
            cache   : cacheService.connections,
            method  : 'GET',
            url     : 'api/connectionGroups/' + encodeURIComponent(connectionGroupID) + '/tree',
            params  : httpParameters
        });
       
    };
    
    /**
     * Makes a request to the REST API to get an individual connection group,
     * returning a promise that provides the corresponding
     * @link{ConnectionGroup} if successful.
     *
     * @param {String} [connectionGroupID=ConnectionGroup.ROOT_IDENTIFIER]
     *     The ID of the connection group to retrieve. If not provided, the
     *     root connection group will be retrieved by default.
     *     
     * @returns {Promise.<ConnectionGroup>} A promise for the HTTP call.
     *     A promise which will resolve with a @link{ConnectionGroup} upon
     *     success.
     */
    service.getConnectionGroup = function getConnectionGroup(connectionGroupID) {
        
        // Use the root connection group ID if no ID is passed in
        connectionGroupID = connectionGroupID || ConnectionGroup.ROOT_IDENTIFIER;
        
        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve connection group
        return $http({
            cache   : cacheService.connections,
            method  : 'GET',
            url     : 'api/connectionGroups/' + encodeURIComponent(connectionGroupID),
            params  : httpParameters
        });

    };
    
    /**
     * Makes a request to the REST API to save a connection group, returning a
     * promise that can be used for processing the results of the call. If the
     * connection group is new, and thus does not yet have an associated
     * identifier, the identifier will be automatically set in the provided
     * connection group upon success.
     * 
     * @param {ConnectionGroup} connectionGroup The connection group to update.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    service.saveConnectionGroup = function saveConnectionGroup(connectionGroup) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // If connection group is new, add it and set the identifier automatically
        if (!connectionGroup.identifier) {
            return $http({
                method  : 'POST',
                url     : 'api/connectionGroups',
                params  : httpParameters,
                data    : connectionGroup
            })

            // Set the identifier on the new connection group and clear the cache
            .success(function connectionGroupCreated(identifier){
                connectionGroup.identifier = identifier;
                cacheService.connections.removeAll();
            });
        }

        // Otherwise, update the existing connection group
        else {
            return $http({
                method  : 'PUT',
                url     : 'api/connectionGroups/' + encodeURIComponent(connectionGroup.identifier),
                params  : httpParameters,
                data    : connectionGroup
            })

            // Clear the cache
            .success(function connectionGroupUpdated(){
                cacheService.connections.removeAll();
            });
        }

    };
    
    /**
     * Makes a request to the REST API to delete a connection group, returning
     * a promise that can be used for processing the results of the call.
     * 
     * @param {ConnectionGroup} connectionGroup The connection group to delete.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteConnectionGroup = function deleteConnectionGroup(connectionGroup) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Delete connection group
        return $http({
            method  : 'DELETE',
            url     : 'api/connectionGroups/' + encodeURIComponent(connectionGroup.identifier),
            params  : httpParameters
        })

        // Clear the cache
        .success(function connectionGroupDeleted(){
            cacheService.connections.removeAll();
        });

    };
    
    return service;
}]);
