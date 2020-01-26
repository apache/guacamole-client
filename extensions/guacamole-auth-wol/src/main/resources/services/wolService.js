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
 * Services to support the Wake-on-LAN extensions, including triggering
 * the wake-up packet and checking host status.
 */
angular.module('guacWOL').factory('wolService', ['$injector',
        function wolService($injector) {
    
    // Import required supporting services
    var $log                  = $injector.get('$log');
    var authenticationService = $injector.get('authenticationService');
    var connectionService     = $injector.get('connectionService');
    var requestService        = $injector.get('requestService');
    
    // This service.
    var service = {};
    
    /**
     * Wake up the host associated with the specified connection identifier.
     * 
     * @param {type} identifier
     *     The identifier of the connection in the directory of the host
     *     to be sent the Wake-on-LAN packet.
     *     
     * @return {Promise}
     *     A promise which, when resolved, will mirror the identifier
     *     passed in for the wake-on-LAN request and the status of the request.
     */
    service.wakeHost = function wakeHost(identifier) {
        
        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };

        // Send the WOL request
        return requestService({
            method  : 'POST',
            url     : 'api/session/ext/wol/wake',
            params  : httpParameters,
            data    : $.param({connectionIdentifier: identifier}),
            headers : {'Content-Type': 'application/x-www-form-urlencoded'}
        })
        .then(function wokeHost(wokeData) {
            return wokeData['identifier'];
        });
    };
    
    /**
     * Retrieve host information about the host associated with the given
     * connection data source and identifier.
     * 
     * @param {type} dataSource
     *     The data source where the connection exists.
     *     
     * @param {type} identifier
     *     The identifier of the connection.
     *     
     * @returns {Promise}.Object
     *     A promise, which, when resolved, will contain the hostname and
     *     port associated with the connection.
     */
    var getHostInfo = function getHostInfo(dataSource, identifier) {
        
        return connectionService.getConnectionParameters(dataSource, identifier)
        .then(function gotParameters(paramData) {
            
                $log.debug(paramData);
            
                return {
                   'hostname' : paramData['hostname'],
                   'port'     : paramData['port']
               };
                            
        }, function(error) {
            $log.error(error);
            return null;
        });
    };
    
    /**
     * Check the status of the host associated with the given datasource
     * and connection identifier, returning a promise which, when resolved,
     * will contain true if the host is up, or false if the host is down or an
     * error occurs.
     * 
     * @param {type} dataSource
     *     The datasource in Guacamole where the connection exists.
     *     
     * @param {type} identifier
     *     The identifier of the connection.
     *     
     * @return {Promise}.Boolean
     *     A promse which, when resolved, will contain a Boolean value for the
     *     status of the host - True if the host is up, false otherwise or if
     *     an error exists.
     */
    service.checkStatus = function checkStatus(dataSource, identifier) {

        // Build HTTP parameters set
        var httpParameters = {
            token : authenticationService.getCurrentToken()
        };
        
        // Retrieve the information for the host (hostname and port).
        var hostInfo = getHostInfo(dataSource, identifier);
        
        // Retrieve a promise to get the status of the host.
        var status = hostInfo.then(function hostInfoRetrieved(hostData) {
        
            // Send the WOL request
            return requestService({
                method  : 'GET',
                url     : 'api/session/ext/wol/check/' + hostData['hostname'] + '/' + hostData['port'],
                params  : httpParameters
            })
            .then(function hostStatusRequested(statusData) {
                return statusData;
            }, function hostStatusRequestError(error) {
                $log.error(error);
                return false;
            });
        }, function hostInfoError(error) {
            $log.error(error);
            return false;
        });
        
        // Return the status, once resolved.
        return status.then(function hostStatusRetrieved(statusData) {
            return statusData;
        }, function hostStatusError(error) {
            $log.error(error);
            return false;
        });

    };
    
    return service;
    
}]);