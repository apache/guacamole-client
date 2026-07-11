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
 * IPMI protocol. Console I/O flows over the normal terminal; this directive
 * communicates exclusively over the dedicated "ipmi-control" pipe stream,
 * exchanging newline-delimited JSON with the guacd IPMI module.
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

    directive.controller = ['$scope', '$injector', '$element',
            function guacIpmiControlController($scope, $injector, $element) {

        // Required services
        const ManagedClient = $injector.get('ManagedClient');

        /**
         * The name of the bidirectional control pipe stream, matching
         * GUAC_IPMI_CONTROL_PIPE_NAME on the server.
         *
         * @constant
         * @type String
         */
        const PIPE_NAME = 'ipmi-control';

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
         * The most recently reported chassis power state ("on", "off", or
         * "unknown").
         *
         * @type String
         */
        $scope.power = 'unknown';

        /**
         * The most recently reported SOL session health ("sol-connected" or
         * "sol-disconnected").
         *
         * @type String
         */
        $scope.health = 'sol-disconnected';

        /**
         * The rendered text of the System Event Log, if it has been read, or
         * null if it has not yet been requested.
         *
         * @type String
         */
        $scope.sel = null;

        /**
         * The human-readable result of the most recent command, or null if no
         * command has completed yet.
         *
         * @type String
         */
        $scope.message = null;

        /**
         * Whether the most recent command result indicated failure.
         *
         * @type Boolean
         */
        $scope.messageError = false;

        /**
         * The command awaiting confirmation, or null if no destructive command
         * is currently pending confirmation.
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
         * Whether a command has been sent and no corresponding result has yet
         * been received.
         *
         * @type Boolean
         */
        $scope.busy = false;

        /**
         * The outbound pipe stream / string writer used to send commands to
         * the server, lazily created on first use.
         *
         * @type Guacamole.StringWriter
         */
        let writer = null;

        /**
         * A monotonically increasing counter used to generate unique command
         * identifiers so results can be correlated with their requests.
         *
         * @type Number
         */
        let nextId = 0;

        /**
         * Handles a single complete inbound control message from the server.
         *
         * @param {Object} msg
         *     The parsed JSON message.
         */
        const handleMessage = function handleMessage(msg) {
            $scope.$evalAsync(function applyMessage() {

                switch (msg.type) {

                    case 'state':
                        if (msg.power)  $scope.power  = msg.power;
                        if (msg.health) $scope.health = msg.health;
                        break;

                    case 'result':
                        $scope.busy = false;
                        $scope.message = msg.message;
                        $scope.messageError = (msg.ok === false);
                        break;

                    case 'sel':
                        $scope.sel = msg.error
                            ? ('[' + msg.error + ']')
                            : (msg.text || '');
                        break;

                }

            });
        };

        /**
         * Deferred pipe stream handler for the "ipmi-control" stream. Each
         * server message arrives as its own pipe stream carrying a single JSON
         * object; this reassembles and parses that object.
         */
        const pipeHandler = function pipeHandler(stream, mimetype) {

            const reader = new Guacamole.StringReader(stream);
            let received = '';

            reader.ontext = function ontext(text) {
                received += text;
            };

            reader.onend = function onend() {
                try {
                    handleMessage(JSON.parse(received));
                }
                catch (ignore) {
                    // Ignore malformed messages
                }
            };

        };

        /**
         * Sends the given command over the control pipe, generating a unique
         * correlation id.
         *
         * @param {String} command
         *     The command to send (e.g. "power-on", "identify", "read-sel").
         */
        const send = function send(command) {

            const guacClient = $scope.client && $scope.client.client;
            if (!guacClient)
                return;

            // Lazily open the outbound control pipe
            if (!writer) {
                const stream = guacClient.createPipeStream('application/json', PIPE_NAME);
                writer = new Guacamole.StringWriter(stream);
            }

            const id = 'c' + (nextId++);
            writer.sendText(JSON.stringify({
                type    : 'command',
                command : command,
                id      : id
            }) + '\n');

            $scope.busy = true;
            $scope.message = null;

        };

        /**
         * Requests a fresh, authoritative power/health state from the BMC.
         */
        $scope.refresh = function refresh() {
            send('refresh-status');
        };

        /**
         * Requests the current System Event Log.
         */
        $scope.readSel = function readSel() {
            $scope.sel = '';
            send('read-sel');
        };

        /**
         * Activates the chassis identify LED.
         */
        $scope.identify = function identify() {
            send('identify');
        };

        /**
         * Sends a serial break over the active SOL session.
         */
        $scope.sendBreak = function sendBreak() {
            send('send-break');
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
                send(action);
        };

        /**
         * Confirms and sends the currently pending destructive action.
         */
        $scope.confirm = function confirm() {
            const action = $scope.pending;
            $scope.pending = null;
            $scope.pendingKey = null;
            if (action)
                send(action);
        };

        /**
         * Cancels the currently pending destructive action.
         */
        $scope.cancel = function cancel() {
            $scope.pending = null;
            $scope.pendingKey = null;
        };

        // Register the control pipe handler as soon as a client is available,
        // then request an authoritative initial state
        $scope.$watch('client', function clientChanged(client) {
            if (client && client.client) {
                ManagedClient.registerDeferredPipeHandler(client, PIPE_NAME, pipeHandler);
                $scope.refresh();
            }
        });

    }];

    return directive;

}]);
