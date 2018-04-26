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
 * Service for operating on connections via the REST API.
 */
angular.module('rest').factory('connectionService', ['$injector',
        function connectionService($injector) {

    // Required services
    var requestService        = $injector.get('requestService');
    var authenticationService = $injector.get('authenticationService');
    var cacheService          = $injector.get('cacheService');
    
    var service = {};
    
    /**
     * Makes a request to the REST API to get a single connection, returning a
     * promise that provides the corresponding @link{Connection} if successful.
     * 
     * @param {String} id The ID of the connection.
     * 
     * @returns {Promise.<Connection>}
     *     A promise which will resolve with a @link{Connection} upon success.
     * 
     * @example
     * 
     * connectionService.getConnection('myConnection').then(function(connection) {
     *     // Do something with the connection
     * });
     */
    service.getConnection = function getConnection(dataSource, id) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve connection
        return requestService({
            cache   : cacheService.connections,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(id),
            params  : httpParameters
        });

    };

    /**
     * Makes a request to the REST API to get the usage history of a single
     * connection, returning a promise that provides the corresponding
     * array of @link{ConnectionHistoryEntry} objects if successful.
     * 
     * @param {String} id
     *     The identifier of the connection.
     * 
     * @returns {Promise.<ConnectionHistoryEntry[]>}
     *     A promise which will resolve with an array of
     *     @link{ConnectionHistoryEntry} objects upon success.
     */
    service.getConnectionHistory = function getConnectionHistory(dataSource, id) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve connection history
        return requestService({
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(id) + '/history',
            params  : httpParameters
        });
 
    };

    /**
     * Makes a request to the REST API to get the parameters of a single
     * connection, returning a promise that provides the corresponding
     * map of parameter name/value pairs if successful.
     * 
     * @param {String} id
     *     The identifier of the connection.
     * 
     * @returns {Promise.<Object.<String, String>>}
     *     A promise which will resolve with an map of parameter name/value
     *     pairs upon success.
     */
    service.getConnectionParameters = function getConnectionParameters(dataSource, id) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Retrieve connection parameters
        return requestService({
            cache   : cacheService.connections,
            method  : 'GET',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(id) + '/parameters',
            params  : httpParameters
        });
 
    };

    /**
     * Makes a request to the REST API to save a connection, returning a
     * promise that can be used for processing the results of the call. If the
     * connection is new, and thus does not yet have an associated identifier,
     * the identifier will be automatically set in the provided connection
     * upon success.
     * 
     * @param {Connection} connection The connection to update.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     save operation is successful.
     */
    service.saveConnection = function saveConnection(dataSource, connection) {
        
        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // If connection is new, add it and set the identifier automatically
        if (!connection.identifier) {
            return requestService({
                method  : 'POST',
                url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/connections',
                params  : httpParameters,
                data    : connection
            })

            // Set the identifier on the new connection and clear the cache
            .then(function connectionCreated(newConnection){
                connection.identifier = newConnection.identifier;
                cacheService.connections.removeAll();

                // Clear users cache to force reload of permissions for this
                // newly created connection
                cacheService.users.removeAll();
            });
        }

        // Otherwise, update the existing connection
        else {
            return requestService({
                method  : 'PUT',
                url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(connection.identifier),
                params  : httpParameters,
                data    : connection
            })
            
            // Clear the cache
            .then(function connectionUpdated(){
                cacheService.connections.removeAll();

                // Clear users cache to force reload of permissions for this
                // newly updated connection
                cacheService.users.removeAll();
            });
        }

    };
    
    /**
     * Makes a request to the REST API to delete a connection,
     * returning a promise that can be used for processing the results of the call.
     * 
     * @param {Connection} connection The connection to delete.
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     delete operation is successful.
     */
    service.deleteConnection = function deleteConnection(dataSource, connection) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Delete connection
        return requestService({
            method  : 'DELETE',
            url     : 'api/session/data/' + encodeURIComponent(dataSource) + '/connections/' + encodeURIComponent(connection.identifier),
            params  : httpParameters
        })

        // Clear the cache
        .then(function connectionDeleted(){
            cacheService.connections.removeAll();
        });

    };
    
    return service;
}]);
