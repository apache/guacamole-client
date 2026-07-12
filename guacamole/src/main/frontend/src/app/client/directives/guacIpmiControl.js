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
 * A directive which provides out-of-band chassis management (power control,
 * chassis identify, System Event Log, serial break) for connections using the
 * IPMI protocol. Intended to live within the client menu. Console I/O flows
 * over the normal terminal; all chassis state and commands are mediated by
 * ipmiControlService over the dedicated "ipmi-control" pipe stream.
 */
angular.module('client').directive('guacIpmiControl', [function guacIpmiControl() {

    const directive = {
        restrict: 'E',
        replace: true,
        templateUrl: 'app/client/templates/guacIpmiControl.html'
    };

    directive.scope = {

        /**
         * The ManagedClient of the IPMI connection to control.
         *
         * @type ManagedClient
         */
        client : '='

    };

    directive.controller = ['$scope', '$injector',
            function guacIpmiControlController($scope, $injector) {

        // Required services
        const ipmiControlService = $injector.get('ipmiControlService');

        /**
         * The set of power actions which are potentially disruptive and thus
         * require explicit confirmation before being sent to the BMC.
         *
         * @type Object.<String, Boolean>
         */
        const DESTRUCTIVE = {
            'power-off'            : true,
            'power-cycle'         : true,
            'hard-reset'          : true,
            'diagnostic-interrupt' : true
        };

        /**
         * The shared chassis state for the current client, or null if no
         * client is available yet.
         *
         * @type Object
         */
        $scope.state = null;

        /**
         * The power action awaiting confirmation, or null if none is pending.
         *
         * @type String
         */
        $scope.pending = null;

        /**
         * The translation-key suffix (uppercase, underscore-separated) of the
         * pending destructive action, or null if none is pending.
         *
         * @type String
         */
        $scope.pendingKey = null;

        /**
         * Requests a fresh, authoritative power/health state from the BMC.
         */
        $scope.refresh = function refresh() {
            ipmiControlService.send($scope.client, 'refresh-status');
        };

        /**
         * Requests the current System Event Log.
         */
        $scope.readSel = function readSel() {
            if ($scope.state)
                $scope.state.sel = '';
            ipmiControlService.send($scope.client, 'read-sel');
        };

        /**
         * Activates the chassis identify LED.
         */
        $scope.identify = function identify() {
            ipmiControlService.send($scope.client, 'identify');
        };

        /**
         * Sends a serial break over the active SOL session.
         */
        $scope.sendBreak = function sendBreak() {
            ipmiControlService.send($scope.client, 'send-break');
        };

        /**
         * Invokes the given power action, first prompting for confirmation if
         * the action is potentially disruptive.
         *
         * @param {String} action
         *     The power action to invoke (e.g. "power-on", "hard-reset").
         */
        $scope.power_action = function power_action(action) {
            if (DESTRUCTIVE[action]) {
                $scope.pending = action;
                $scope.pendingKey = action.toUpperCase().replace(/-/g, '_');
            }
            else
                ipmiControlService.send($scope.client, action);
        };

        /**
         * Confirms and sends the currently pending destructive action.
         */
        $scope.confirm = function confirm() {
            const action = $scope.pending;
            $scope.pending = null;
            $scope.pendingKey = null;
            if (action)
                ipmiControlService.send($scope.client, action);
        };

        /**
         * Cancels the currently pending destructive action.
         */
        $scope.cancel = function cancel() {
            $scope.pending = null;
            $scope.pendingKey = null;
        };

        // Bind to the shared chassis state as soon as a client is available
        $scope.$watch('client', function clientChanged(client) {
            if (client && client.client)
                $scope.state = ipmiControlService.getState(client);
        });

    }];

    return directive;

}]);
