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
 * Service for operating on connections via the REST API.
 */
angular.module('rest').factory('connectionService', ['$http', 'authenticationService',
        function connectionService($http, authenticationService) {
            
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
     * connectionService.getConnection('myConnection').success(function(connection) {
     *     // Do something with the connection
     * });
     */
    service.getConnection = function getConnection(id) {
        return $http.get("api/connection/" + id + "?token=" + authenticationService.getCurrentToken());
    };

    /**
     * Makes a request to the REST API to get the list of connections,
     * returning a promise that provides an array of
     * @link{Connection} objects if successful.
     * 
     * @param {String} [parentID=ConnectionGroup.ROOT_IDENTIFIER]
     *     The ID of the connection group whose child connections should be
     *     returned. If not provided, the root connection group will be used
     *     by default.
     *                          
     * @returns {Promise.<Connection[]>}
     *     A promise which will resolve with an array of @link{Connection}
     *     objects upon success.
     */
    service.getConnections = function getConnections(parentID) {
        
        var parentIDParam = "";
        if (parentID)
            parentIDParam = "&parentID=" + parentID;
        
        return $http.get("api/connection?token=" + authenticationService.getCurrentToken() + parentIDParam);
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
    service.saveConnection = function saveConnection(connection) {
        
        /*
         * FIXME: This should not be necessary. Perhaps the need for this is a
         * sign that history should be queried separately, and not reside as
         * part of the connection object?
         */
        // Do not try to save the connection history records
        var connectionToSave = angular.copy(connection);
        delete connectionToSave.history;
        
        // If connection is new, add it and set the identifier automatically
        if (!connectionToSave.identifier) {
            return $http.post("api/connection/?token=" + authenticationService.getCurrentToken(), connectionToSave).success(

                // Set the identifier on the new connection
                function setConnectionID(connectionID){
                    connection.identifier = connectionID;
                }

            );
        }
        
        // Otherwise, update the existing connection
        else {
            return $http.post(
                "api/connection/" + connectionToSave.identifier + 
                "?token=" + authenticationService.getCurrentToken(), 
            connectionToSave);
        }

    };
    
    /**
     * FIXME: Why is this different from save?
     * 
     * Makes a request to the REST API to move a connection to a different
     * group, returning a promise that can be used for processing the results
     * of the call.
     * 
     * @param {Connection} connection The connection to move. 
     *                          
     * @returns {Promise}
     *     A promise for the HTTP call which will succeed if and only if the
     *     move operation is successful.
     */
    service.moveConnection = function moveConnection(connection) {
        
        return $http.put(
            "api/connection/" + connection.identifier + 
            "?token=" + authenticationService.getCurrentToken() + 
            "&parentID=" + connection.parentIdentifier, 
        connection);
        
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
    service.deleteConnection = function deleteConnection(connection) {
        return $http['delete'](
            "api/connection/" + connection.identifier + 
            "?token=" + authenticationService.getCurrentToken());
    };
    
    return service;
}]);
