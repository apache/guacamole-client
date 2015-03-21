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
 * Service for operating on active connections via the REST API.
 */
angular.module('rest').factory('activeConnectionService', ['$http', 'authenticationService',
        function activeConnectionService($http, authenticationService) {

    var service = {};

    /**
     * Makes a request to the REST API to get the list of active tunnels,
     * returning a promise that provides a map of @link{ActiveConnection}
     * objects if successful.
     *
     * @param {String[]} [permissionTypes]
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for an active connection to appear in the
     *     result.  If null, no filtering will be performed. Valid values are
     *     listed within PermissionSet.ObjectType.
     *                          

     * @returns {Promise.<Object.<String, ActiveConnection>>}
     *     A promise which will resolve with a map of @link{ActiveConnection}
     *     objects, where each key is the identifier of the corresponding
     *     active connection.
     */
    service.getActiveConnections = function getActiveConnections(permissionTypes) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Add permission filter if specified
        if (permissionTypes)
            httpParameters.permission = permissionTypes;

        // Retrieve tunnels
        return $http({
            method  : 'GET',
            url     : 'api/activeConnections',
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to delete the active connections having
     * the given identifiers, effectively disconnecting them, returning a
     * promise that can be used for processing the results of the call.
     *
     * @param {String[]} identifiers
     *     The identifiers of the active connections to delete.
     *
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteActiveConnections = function deleteActiveConnections(identifiers) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Convert provided array of identifiers to a patch
        var activeConnectionPatch = [];
        identifiers.forEach(function addActiveConnectionPatch(identifier) {
            activeConnectionPatch.push({
                op   : 'remove',
                path : '/' + identifier 
            });
        });

        // Perform active connection deletion via PATCH
        return $http({
            method  : 'PATCH',
            url     : 'api/activeConnections',
            params  : httpParameters,
            data    : activeConnectionPatch
        });
        
    };

    return service;

}]);
