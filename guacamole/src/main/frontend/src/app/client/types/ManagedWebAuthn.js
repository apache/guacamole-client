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
 * Provides the ManagedWebAuthn class used by ManagedClient to relay
 * WebAuthn ceremonies from a remote session to the user's local
 * authenticator. Ceremony requests arrive as inbound "auth-challenge"
 * streams whose mimetype identifies the ceremony kind; responses are
 * sent back as outbound "auth-response" streams referencing the same
 * challenge_id.
 */
angular.module('client').factory('ManagedWebAuthn', ['$injector', '$rootScope',
    function defineManagedWebAuthn($injector, $rootScope) {

    const webAuthnService = $injector.get('webAuthnService');

    /**
     * Mimetypes recognized by this relay, keyed by mimetype value to the
     * ceremony kind ("create" or "get") that they identify.
     *
     * @private
     * @constant
     * @type {!Object.<String, String>}
     */
    const MIMETYPE_KIND = {
        'application/x-webauthn-create+json': 'create',
        'application/x-webauthn-get+json':    'get'
    };

    /**
     * Runs the given mutation inside an Angular digest. handleChallenge
     * is called from Guacamole's tunnel.oninstruction dispatch which runs
     * outside Angular, so state changes the panel template watches need
     * an explicit $apply. Callbacks resolved through $q are already
     * inside a digest, so this is a no-op there.
     *
     * @private
     * @param {!Function} updateFn
     *     The mutation to apply.
     */
    function applyPanelStateChange(updateFn) {
        if ($rootScope.$$phase)
            updateFn();
        else
            $rootScope.$apply(updateFn);
    }

    /**
     * Object which tracks the state of WebAuthn passthrough for a single
     * ManagedClient.
     *
     * @constructor
     * @param {ManagedWebAuthn|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedWebAuthn.
     */
    const ManagedWebAuthn = function ManagedWebAuthn(template) {

        template = template || {};

        /**
         * The ManagedClient that owns this WebAuthn relay.
         *
         * @type ManagedClient
         */
        this.client = template.client;

        /**
         * The number of WebAuthn ceremonies currently in flight (awaiting
         * the user's authenticator).
         *
         * @type {!Number}
         */
        this.inFlight = template.inFlight || 0;

        /**
         * The status of the most recently-completed ceremony. One of
         * "completed", "failed", or null if no ceremony has run yet.
         *
         * @type {String}
         */
        this.lastStatus = template.lastStatus || null;

        /**
         * Whether the browser has refused a ceremony due to enterprise
         * policy. Once set, remains true for the lifetime of this client
         * since the policy is not expected to change mid-session.
         *
         * @type {!Boolean}
         */
        this.policyBlocked = !!template.policyBlocked;

        /**
         * Whether the browser has reported that WebAuthn is not supported
         * (NotSupportedError from navigator.credentials). Once set,
         * remains true for the lifetime of this client since browser
         * support is not expected to change mid-session.
         *
         * @type {!Boolean}
         */
        this.notSupported = !!template.notSupported;

        /**
         * AbortControllers for in-flight ceremonies, keyed by challenge
         * id. Used to dismiss the local authenticator UI when the
         * Guacamole connection drops.
         *
         * @private
         * @type {!Object.<String, AbortController>}
         */
        this._abortControllers = {};

    };

    /**
     * Creates a new ManagedWebAuthn instance bound to the given client.
     *
     * @param {ManagedClient} client
     *     The client that this ManagedWebAuthn should be associated with.
     *
     * @returns {ManagedWebAuthn}
     *     The newly-created ManagedWebAuthn.
     */
    ManagedWebAuthn.getInstance = function getInstance(client) {
        return new ManagedWebAuthn({ client });
    };

    /**
     * Returns whether the given auth-challenge mimetype is one this relay
     * handles.
     *
     * @param {!String} mimetype
     *     The mimetype to check.
     *
     * @returns {!Boolean}
     *     true if the mimetype is recognized, false otherwise.
     */
    ManagedWebAuthn.handlesMimetype = function handlesMimetype(mimetype) {
        return Object.prototype.hasOwnProperty.call(MIMETYPE_KIND, mimetype);
    };

    /**
     * Handles a single inbound WebAuthn ceremony challenge, dispatched
     * from the ManagedClient's auth-challenge handler. Reads the JSON
     * payload off the stream, dispatches the ceremony to the user's
     * local authenticator via webAuthnService, and sends the result (or
     * error) back as an auth-response stream with the matching
     * challenge_id.
     *
     * @param {!ManagedWebAuthn} managedWebAuthn
     *     The ManagedWebAuthn that should handle the challenge.
     *
     * @param {!Guacamole.InputStream} stream
     *     The input stream carrying the challenge body.
     *
     * @param {!String} mimetype
     *     The mimetype of the challenge body. Used to determine the
     *     ceremony kind.
     *
     * @param {!String} challengeId
     *     The challenge identifier; the matching auth-response will echo
     *     this value back.
     */
    ManagedWebAuthn.handleChallenge = function handleChallenge(
            managedWebAuthn, stream, mimetype, challengeId) {

        const kind = MIMETYPE_KIND[mimetype];
        if (!kind) {
            stream.sendAck('Unsupported auth-challenge mimetype',
                    Guacamole.Status.Code.UNSUPPORTED);
            return;
        }

        stream.sendAck('Ready', Guacamole.Status.Code.SUCCESS);

        let payload = '';
        const reader = new Guacamole.StringReader(stream);
        reader.ontext = function challengeChunk(text) {
            payload += text;
        };
        reader.onend = function challengeComplete() {
            dispatchCeremony(
                    managedWebAuthn, mimetype, challengeId, kind, payload);
        };

    };

    /**
     * Aborts every in-flight ceremony. Used when the Guacamole connection
     * drops, so the local authenticator UI does not sit on screen with
     * no live session behind it.
     *
     * @param {!ManagedWebAuthn} managedWebAuthn
     *     The ManagedWebAuthn whose ceremonies should be canceled.
     */
    ManagedWebAuthn.abortAll = function abortAll(managedWebAuthn) {
        const controllers = managedWebAuthn._abortControllers;
        for (const id in controllers) {
            if (Object.prototype.hasOwnProperty.call(controllers, id))
                controllers[id].abort();
        }
    };

    /**
     * Dispatches a fully-assembled ceremony challenge to the user's local
     * authenticator. Parses the JSON body, runs the WebAuthn ceremony
     * under an AbortController stored by challenge id, and sends the
     * response back via client.sendAuthResponse.
     *
     * @private
     * @param {!ManagedWebAuthn} managedWebAuthn
     *     The ManagedWebAuthn that owns the in-flight challenge.
     *
     * @param {!String} mimetype
     *     The mimetype of the originating auth-challenge, echoed onto
     *     the auth-response so the peer can route it.
     *
     * @param {!String} challengeId
     *     The challenge identifier parsed from the auth-challenge.
     *
     * @param {!String} kind
     *     The ceremony kind, "create" or "get", derived from the
     *     mimetype.
     *
     * @param {!String} payloadJson
     *     The JSON-encoded request body. Expected to deserialize to an
     *     object with originOverride, sameOriginWithAncestors, and
     *     publicKey fields.
     */
    function dispatchCeremony(managedWebAuthn, mimetype, challengeId, kind,
            payloadJson) {

        let request;
        try {
            request = JSON.parse(payloadJson);
        }
        catch (e) {
            console.warn('[WebAuthn] dropped malformed ceremony challenge id='
                    + challengeId + ' kind=' + kind, e);
            sendResponse(managedWebAuthn, mimetype, challengeId, 'error', {
                name: 'DataError',
                message: 'Malformed WebAuthn request payload'
            });
            return;
        }

        const controller = new AbortController();
        managedWebAuthn._abortControllers[challengeId] = controller;

        applyPanelStateChange(function trackInFlight() {
            managedWebAuthn.inFlight++;
        });

        webAuthnService.performCeremony({
            kind: kind,
            opts: request.publicKey,
            originOverride: request.originOverride,
            sameOriginWithAncestors: request.sameOriginWithAncestors
        }, controller.signal).then(function ceremonyResolved(credential) {
            sendResponse(managedWebAuthn, mimetype, challengeId, 'ok',
                    { credential: credential });
        }, function ceremonyRejected(error) {

            if (error && error.policyBlocked)
                managedWebAuthn.policyBlocked = true;

            if (error && error.name === 'NotSupportedError')
                managedWebAuthn.notSupported = true;

            console.warn('[WebAuthn] ceremony id=' + challengeId
                    + ' kind=' + kind
                    + (error && error.policyBlocked ? ' (policy-blocked)' : '')
                    + ' failed:', error);

            sendResponse(managedWebAuthn, mimetype, challengeId, 'error', {
                name: (error && error.name) || 'Error',
                message: (error && error.message) || String(error)
            });
        });

    }

    /**
     * Sends an auth-response settling the given challenge with the given
     * status and payload, and updates the status fields of the
     * ManagedWebAuthn accordingly. The response JSON carries the status
     * field alongside the payload so the peer can pull the status
     * without inspecting credential shape.
     *
     * @private
     * @param {!ManagedWebAuthn} managedWebAuthn
     *     The ManagedWebAuthn whose status should be updated.
     *
     * @param {!String} mimetype
     *     The mimetype of the originating auth-challenge, echoed back.
     *
     * @param {!String} challengeId
     *     The challenge identifier being settled.
     *
     * @param {!String} status
     *     The status of the ceremony: "ok" or "error".
     *
     * @param {!Object} payload
     *     The response payload object; will be JSON-encoded into the
     *     body alongside the status.
     */
    function sendResponse(managedWebAuthn, mimetype, challengeId, status,
            payload) {

        delete managedWebAuthn._abortControllers[challengeId];

        const client = managedWebAuthn.client && managedWebAuthn.client.client;
        if (client) {
            const stream = client.sendAuthResponse(mimetype, challengeId);
            if (stream) {
                const writer = new Guacamole.StringWriter(stream);
                writer.sendText(JSON.stringify(Object.assign(
                        { status: status }, payload)));
                writer.sendEnd();
            }
        }

        applyPanelStateChange(function recordCeremonySettled() {
            managedWebAuthn.inFlight =
                    Math.max(0, managedWebAuthn.inFlight - 1);
            managedWebAuthn.lastStatus =
                    (status === 'ok') ? 'completed' : 'failed';
        });

    }

    return ManagedWebAuthn;

}]);
