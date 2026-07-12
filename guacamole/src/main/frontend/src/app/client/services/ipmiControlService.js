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
 * A service which maintains the out-of-band chassis state (power, SOL health,
 * System Event Log, last command result) for IPMI connections, communicating
 * with the guacd IPMI module over the dedicated "ipmi-control" pipe stream.
 *
 * State is held per ManagedClient and persists independently of any directive
 * lifecycle, so both the always-visible status badge and the (menu-scoped,
 * repeatedly destroyed) control panel can be thin views over a single source
 * of truth.
 */
angular.module('client').factory('ipmiControlService', ['$rootScope', '$injector',
        function ipmiControlService($rootScope, $injector) {

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

    const service = {};

    /**
     * Per-ManagedClient chassis state, keyed by ManagedClient id.
     *
     * @type Object.<String, Object>
     */
    const states = {};

    /**
     * The outbound control writer for each ManagedClient, keyed by id and
     * created lazily on first command.
     *
     * @type Object.<String, Guacamole.StringWriter>
     */
    const writers = {};

    /**
     * A monotonically increasing counter used to generate unique command
     * identifiers so results can be correlated with their requests.
     *
     * @type Number
     */
    let nextId = 0;

    /**
     * Applies the given inbound control message to the state of the given
     * client, within an Angular digest.
     *
     * @param {Object} state
     *     The chassis state object to update.
     *
     * @param {Object} msg
     *     The parsed inbound JSON message.
     */
    const handleMessage = function handleMessage(state, msg) {
        $rootScope.$evalAsync(function applyMessage() {
            switch (msg.type) {

                case 'state':
                    if (msg.power)  state.power  = msg.power;
                    if (msg.health) state.health = msg.health;
                    break;

                case 'result':
                    state.busy = false;
                    state.message = msg.message;
                    state.messageError = (msg.ok === false);
                    break;

                case 'sel':
                    state.sel = msg.error ? ('[' + msg.error + ']') : (msg.text || '');
                    break;

            }
        });
    };

    /**
     * Returns the chassis state object for the given ManagedClient, creating
     * and initializing it (registering the inbound pipe handler and requesting
     * an authoritative status) on first access.
     *
     * @param {ManagedClient} client
     *     The IPMI ManagedClient whose chassis state is desired.
     *
     * @returns {Object}
     *     The chassis state object for the given client.
     */
    service.getState = function getState(client) {

        const id = client.id;

        if (!states[id]) {

            const state = {
                power        : 'unknown',
                health       : 'sol-disconnected',
                sel          : null,
                message      : null,
                messageError : false,
                busy         : false
            };
            states[id] = state;

            // Register the inbound control handler and request an initial
            // authoritative state as soon as the underlying client exists
            if (client.client) {
                ManagedClient.registerDeferredPipeHandler(client, PIPE_NAME,
                        function pipeHandler(stream) {
                    const reader = new Guacamole.StringReader(stream);
                    let received = '';
                    reader.ontext = function ontext(text) { received += text; };
                    reader.onend = function onend() {
                        try { handleMessage(state, JSON.parse(received)); }
                        catch (ignore) { /* ignore malformed messages */ }
                    };
                });
                service.send(client, 'refresh-status');
            }

        }

        return states[id];

    };

    /**
     * Sends the given command to the given client over the control pipe,
     * generating a unique correlation id and marking the client busy.
     *
     * @param {ManagedClient} client
     *     The IPMI ManagedClient to command.
     *
     * @param {String} command
     *     The command to send (e.g. "power-on", "identify", "read-sel").
     */
    service.send = function send(client, command) {

        const guacClient = client && client.client;
        if (!guacClient)
            return;

        const id = client.id;

        // Lazily open the outbound control pipe for this client
        if (!writers[id]) {
            const stream = guacClient.createPipeStream('application/json', PIPE_NAME);
            writers[id] = new Guacamole.StringWriter(stream);
        }

        writers[id].sendText(JSON.stringify({
            type    : 'command',
            command : command,
            id      : 'c' + (nextId++)
        }) + '\n');

        const state = states[id];
        if (state) {
            state.busy = true;
            state.message = null;
        }

    };

    return service;

}]);
