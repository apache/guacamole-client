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
 * Service for operating on active connections via the REST API.
 */
angular.module('rest').factory('activeConnectionService', ['$injector',
        function activeConnectionService($injector) {

    // Required services
    var $http                 = $injector.get('$http');
    var $q                    = $injector.get('$q');
    var authenticationService = $injector.get('authenticationService');

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
    service.getActiveConnections = function getActiveConnections(dataSource, permissionTypes) {

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
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/activeConnections',
            params  : httpParameters
        });

    };

    /**
     * Returns a promise which resolves with all active connections accessible
     * by the current user, as a map of @link{ActiveConnection} maps, as would
     * be returned by getActiveConnections(), grouped by the identifier of
     * their corresponding data source. All given data sources are queried. If
     * an error occurs while retrieving any ActiveConnection map, the promise
     * will be rejected.
     *
     * @param {String[]} dataSources
     *     The unique identifier of the data sources containing the active
     *     connections to be retrieved. These identifiers correspond to
     *     AuthenticationProviders within the Guacamole web application.
     *
     * @param {String[]} [permissionTypes]
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for an active connection to appear in the
     *     result.  If null, no filtering will be performed. Valid values are
     *     listed within PermissionSet.ObjectType.
     *
     * @returns {Promise.<Object.<String, Object.<String, ActiveConnection>>>}
     *     A promise which resolves with all active connections available to
     *     the current user, as a map of ActiveConnection maps, as would be
     *     returned by getActiveConnections(), grouped by the identifier of
     *     their corresponding data source.
     */
    service.getAllActiveConnections = function getAllActiveConnections(dataSources, permissionTypes) {

        var deferred = $q.defer();

        var activeConnectionRequests = [];
        var activeConnectionMaps = {};

        // Retrieve all active connections from all data sources
        angular.forEach(dataSources, function retrieveActiveConnections(dataSource) {
            activeConnectionRequests.push(
                service.getActiveConnections(dataSource, permissionTypes)
                .success(function activeConnectionsRetrieved(activeConnections) {
                    activeConnectionMaps[dataSource] = activeConnections;
                })
            );
        });

        // Resolve when all requests are completed
        $q.all(activeConnectionRequests)
        .then(

            // All requests completed successfully
            function allActiveConnectionsRetrieved() {
                deferred.resolve(userArrays);
            },

            // At least one request failed
            function activeConnectionRetrievalFailed(e) {
                deferred.reject(e);
            }

        );

        return deferred.promise;

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
    service.deleteActiveConnections = function deleteActiveConnections(dataSource, identifiers) {

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
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/activeConnections',
            params  : httpParameters,
            data    : activeConnectionPatch
        });
        
    };

    /**
     * Makes a request to the REST API to generate credentials which have
     * access strictly to the given active connection, using the restrictions
     * defined by the given sharing profile, returning a promise that provides
     * the resulting @link{UserCredentials} object if successful.
     *
     * @param {String} id
     *     The identifier of the active connection being shared.
     *
     * @param {String} sharingProfile
     *     The identifier of the sharing profile dictating the
     *     semantics/restrictions which apply to the shared session.
     *
     * @returns {Promise.<UserCredentials>}
     *     A promise which will resolve with a @link{UserCredentials} object
     *     upon success.
     */
    service.getSharingCredentials = function getSharingCredentials(dataSource, id, sharingProfile) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Generate sharing credentials
        return $http({
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource)
                        + '/activeConnections/' + encodeURIComponent(id)
                        + '/sharingCredentials/' + encodeURIComponent(sharingProfile),
            params  : httpParameters
        });

    };

    return service;

}]);
