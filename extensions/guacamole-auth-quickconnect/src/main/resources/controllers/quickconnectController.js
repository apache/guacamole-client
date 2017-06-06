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
 * The controller for making ad-hoc (quick) connections
 */
angular.module('guacQuickConnect').controller('quickconnectController', ['$scope', '$injector', '$log',
        function manageConnectionController($scope, $injector, $log) {

    $log.debug('In quickconnectController...');

    // Required types
    var ClientIdentifier    = $injector.get('ClientIdentifier');
    var Connection          = $injector.get('Connection');

    // Required services
    var $location                = $injector.get('$location');
    var $routeParams             = $injector.get('$routeParams');
    var guacNotification         = $injector.get('guacNotification');
    var connectionService        = $injector.get('connectionService');
    var schemaService            = $injector.get('schemaService');

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_CONNECTION.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            guacNotification.showStatus(false);
        }
    };

    $scope.uri = null;

    $scope.selectedDataSource = 'quickconnect';

    /**
     * Saves the connection, creating a new connection or updating the existing
     * connection.
     */
    $scope.quickConnect = function quickConnect() {

        $log.debug('Got saveConnection() call.');

        // Construct parameters from URI...
        /**
         * Parse URL into the following components:
         * [0] - Full URL
         * [3] - Protocol
         * [5] - Username
         * [7] - Password
         * [8] - Hostname
         * [10] - Port
         * [11] - Path
         * [13] - Document
         * [15] - Parameters
         * [17] - JS Route
         */
        var regexURL = /^(((rdp|ssh|telnet|vnc)?):\/)?\/?((.*?)(:(.*?)|)@)?([^:\/\s]+)(:([^\/]*))?((\/\w+\/)*)([-\w.\/]+[^#?\s]*)?(\?([^#]*))?(#(.*))?$/g;
        $log.debug(regexURL);
        var urlArray = regexURL.exec($scope.uri);
        $log.debug($scope.uri);
        $log.debug(urlArray);

        var gettingProtocols = schemaService.getProtocols('quickconnect');
        gettingProtocols.success(function checkProtocol(supportedProtocols) {
            $log.debug(supportedProtocols);
            if (!(urlArray[3] in supportedProtocols)) {
                guacNotification.showStatus({
                    'className' : 'error',
                    'title'     : 'Unsupported Protocol',
                    'text'      : 'The ' + urlArray[3] + ' protocol is not supported by Guacamole.',
                    'actions'   : [ ACKNOWLEDGE_ACTION ]
               });
              return;
            }
            var port = 0;
            var urlParams = Array();
            switch(urlArray[3]) {
                case 'rdp':
                    port = 3389;
                    break;
                case 'ssh':
                    port = 22;
                    break;
                case 'telnet':
                    port = 23;
                    break;
                case 'vnc':
                    port = 5900;
                    break;
                default:
                    port = 0;
            }

            if (!isNaN(urlArray[10]))
                port = parseInt(urlArray[10]);

            if (!(typeof urlArray[15] === 'undefined'))
                urlParams = JSON.parse('{"' + decodeURI(urlArray[15]).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g,'":"') + '"}');

            console.log(urlParams);

            var connParams = {};
            if (!(typeof urlArray[8] === 'undefined'))
                connParams['hostname'] = urlArray[8];
            else
                connParams['hostname'] = 'localhost';
            if (!(typeof urlArray[5] === 'undefined'))
                connParams['username'] = urlArray[5];
            if (!(typeof urlArray[7] === 'undefined'))
                connParams['password'] = urlArray[7];
            connParams['port'] = port;
            connParams['read-only'] = 'read-only' in urlParams ? urlParams['read-only'] : '';
            connParams['swap-red-blue'] = 'swap-red-blue' in urlParams ? urlParams['swap-red-blue'] : '';
            connParams['cursor'] = 'cursor' in urlParams ? urlParams['cursor'] : '';
            connParams['color-depth'] = 'color-depth' in urlParams ? parseInt(urlParams['color-depth']) : '';
            connParams['clipboard-encoding'] = 'clipboard-encoding' in urlParams ? urlParams['clipboard-encoding'] : '';
            connParams['dest-port'] = 'dest-port' in urlParams ? parseInt(urlParams['dest-port']) : '';
            connParams['create-recording-path'] = 'create-recording-path' in urlParams ? urlParams['create-recording-path'] : '';
            connParams['enable-sftp'] = 'enable-sftp' in urlParams ? urlParams['enable-sftp'] : false;
            connParams['sftp-port'] = 'sftp-port' in urlParams ? parseInt(urlParams['sftp-port']) : 22;
            connParams['enable-audio'] = 'enable-audio' in urlParams ? urlParams['enable-audio'] : false;
            connParams['color-scheme'] = 'color-scheme' in urlParams ? urlParams['color-scheme'] : '';
            connParams['font-size'] = 'font-size' in urlParams ? parseInt(urlParams['font-size']) : '';
            connParams['create-typescript-path'] = 'create-typescript-path' in urlParams ? urlParams['create-typescript-path'] : '';
            connParams['private-key'] = 'private-key' in urlParams ? urlParams['private-key'] : '';
            connParams['passphrase'] = 'passphrase' in urlParams ? urlParams['passphrase'] : '';

            var connName = urlArray[3] + '://';
            if(!(typeof connParams['username'] === 'undefined'))
                connName += connParams['username'] + '@';
            connName += connParams['hostname'] + ':' + connParams['port'];
            
            $scope.connection = new Connection({
                name : connName,
                protocol : urlArray[3],
                parameters: connParams
            });
            console.log($scope.connection);

            connectionService.saveConnection($scope.selectedDataSource, $scope.connection)
            .success(function runConnection(newConnection) {
                $log.debug('Connection saved - we should run it, now: ' + newConnection.identifier);
                $location.url('/client/' + ClientIdentifier.toString({
                            dataSource : $scope.selectedDataSource,
                            type       : ClientIdentifier.Types.CONNECTION,
                            id         : newConnection.identifier
                }));
            })
            .error(function saveFailed(error) {
                guacNotification.showStatus({
                    'className'  : 'error',
                    'title'      : 'MANAGE_CONNECTION.DIALOG_HEADER_ERROR',
                    'text'       : error.translatableMessage,
                    'actions'    : [ ACKNOWLEDGE_ACTION ]
                });
            });

        });

        

        return;

        /*
        $scope.connection.parameters = $scope.parameters;

        // Save the connection
        connectionService.saveConnection($scope.selectedDataSource, $scope.connection)
        .success(function savedConnection(newConnection) {
            $log.debug('Connection saved successfully: ' + newConnection);
            // $location.url('/settings/' + encodeURIComponent($scope.selectedDataSource) + '/connections');
        })

        // Notify of any errors
        .error(function connectionSaveFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_CONNECTION.DIALOG_HEADER_ERROR',
                'text'       : error.translatableMessage,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });
        */

    };

}]);
