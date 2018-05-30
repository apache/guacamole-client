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

angular.module('guacWOL').controller('wolController', ['$scope', '$injector',
        function serverStatusController($scope, $injector) {

    var $log             = $injector.get('$log');
    var guacNotification = $injector.get('guacNotification');
    var wolService       = $injector.get('wolService');
    
    /**
     * The variable that stores the current state of the host as detected
     * by attempting to open a socket to the IP and port of the connection.
     * Default is false, meaning the host is down.
     */
    $scope.hostUp = false;
    
    /**
     * Controller method that triggers the host wake-up method and
     * processes the result, logging if successful and displaying an
     * error if the underlying service generates an error.
     * 
     * @param {String} identifier
     *     The identifier of the connection with the host being woken up.
     */
    $scope.sendWake = function sendWake(identifier) {
        
        wolService.wakeHost(identifier)
        .then(function sentWake(wakeStatus) {
            $log.debug('Successfully sent wake-up packet to ' + identifier);
        }, guacNotification.SHOW_REQUEST_ERROR);
        
    };
    
    /**
     * Set the currently-known status of the host in the specified
     * connection, either up if the host is up, otherwise down.
     * 
     * @param {String} identifier
     *     The identifier of the connection to query status for.
     */
    $scope.getStatus = function getStatus(dataSource, identifier) {
      
        wolService.checkStatus(dataSource, identifier)
        .then(function processStatus(hostStatus) {
            $scope.hostUp = hostStatus;
            
        }, function(error) {
            $log.error(error);
            $scope.hostUp = false;
        });

    };
            
}]);