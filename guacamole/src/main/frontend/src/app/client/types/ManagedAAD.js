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

import qrcode from 'qrcode-generator';

/**
 * Provides the ManagedAAD class used by ManagedClient to display an Azure AD
 * device code sign-in prompt for an RDP connection. The server obtains a device
 * code from Azure and opens an inbound pipe stream carrying the user code and
 * verification URI; the browser shows these as a QR code the user scans to sign
 * in on a separate device (a phone). The browser sends nothing back: the server
 * polls Azure for completion and closes the stream when the flow finishes,
 * which dismisses the prompt.
 */
angular.module('client').factory('ManagedAAD', ['$rootScope',
    function defineManagedAAD($rootScope) {


    /**
     * Runs the given mutation inside an Angular digest. Prompt handling runs
     * outside Angular (via Guacamole's tunnel dispatch), so state the sign-in
     * prompt watches needs an explicit $apply.
     *
     * @private
     * @param {!Function} updateFn
     *     The mutation to apply.
     */
    function applyStateChange(updateFn) {
        if ($rootScope.$$phase)
            updateFn();
        else
            $rootScope.$apply(updateFn);
    }

    /**
     * Renders the given text as a QR code, returning a data URI suitable for the
     * src of an <img>.
     *
     * @private
     * @param {!String} text
     *     The text to encode.
     *
     * @returns {!String}
     *     A data URI of the rendered QR code.
     */
    function renderQRCode(text) {
        const qr = qrcode(0, 'M');
        qr.addData(text);
        qr.make();
        return qr.createDataURL(6, 8);
    }

    /**
     * Object which tracks Azure AD device code sign-in for a single
     * ManagedClient.
     *
     * @constructor
     * @param {ManagedAAD|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedAAD.
     */
    const ManagedAAD = function ManagedAAD(template) {

        template = template || {};

        /**
         * The ManagedClient that owns this sign-in relay.
         *
         * @type ManagedClient
         */
        this.client = template.client;

        /**
         * The sign-in prompt currently being displayed, or null if none. When
         * set, the QR overlay is shown. Contains the user code, the
         * verification URI (with and without the code embedded), and the QR
         * code data URI.
         *
         * @type {Object}
         */
        this.prompt = template.prompt || null;

    };

    /**
     * Creates a new ManagedAAD instance bound to the given client.
     *
     * @param {ManagedClient} client
     *     The client this ManagedAAD should be associated with.
     *
     * @returns {ManagedAAD}
     *     The newly-created ManagedAAD.
     */
    ManagedAAD.getInstance = function getInstance(client) {
        return new ManagedAAD({ client });
    };

    /**
     * The name of the pipe stream that carries the device code prompt. Matches
     * the name used by the server (guacd) when opening the stream.
     *
     * @constant
     * @type {!String}
     */
    ManagedAAD.PIPE_NAME = 'aad-device-code';

    /**
     * Handles a single inbound Azure AD device code prompt. Reads the JSON
     * prompt off the pipe stream and displays it as a QR code. The stream is
     * left open by the server for the duration of the sign-in; when the server
     * closes it (on success, failure, or expiry) the prompt is dismissed.
     *
     * @param {!ManagedAAD} managedAAD
     *     The ManagedAAD that should display the prompt.
     *
     * @param {!Guacamole.InputStream} stream
     *     The pipe stream carrying the prompt body.
     *
     * @param {!String} mimetype
     *     The mimetype of the prompt body.
     *
     * @param {!String} name
     *     The name of the pipe stream.
     */
    ManagedAAD.handlePrompt = function handlePrompt(managedAAD, stream,
            mimetype, name) {

        let payload = '';
        let shown = false;

        const reader = new Guacamole.StringReader(stream);
        reader.ontext = function promptChunk(text) {

            payload += text;
            if (shown)
                return;

            // The prompt arrives as a single JSON object; wait for enough blobs
            // to form valid JSON before displaying it
            let prompt;
            try {
                prompt = JSON.parse(payload);
            }
            catch (e) {
                return;
            }

            shown = true;
            applyStateChange(function showPrompt() {
                const link = prompt.verification_uri_complete
                        || prompt.verification_uri;
                managedAAD.prompt = {
                    userCode                : prompt.user_code,
                    verificationUri         : prompt.verification_uri,
                    verificationUriComplete : link,
                    qrCode                  : renderQRCode(link)
                };
            });

        };

        // The server closes the stream once the device code flow completes,
        // whatever the outcome; dismiss the prompt
        reader.onend = function signInComplete() {
            ManagedAAD.clear(managedAAD);
        };

    };

    /**
     * Dismisses the currently-displayed sign-in prompt, if any. Called when the
     * server closes the prompt stream and when the connection drops.
     *
     * @param {!ManagedAAD} managedAAD
     *     The ManagedAAD whose prompt should be cleared.
     */
    ManagedAAD.clear = function clear(managedAAD) {
        applyStateChange(function clearPrompt() {
            managedAAD.prompt = null;
        });
    };

    return ManagedAAD;

}]);
