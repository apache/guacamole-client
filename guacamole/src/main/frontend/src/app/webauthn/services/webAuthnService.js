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
 * Service for performing WebAuthn ceremonies against the user's local
 * authenticator on behalf of a remote session, using the
 * remoteDesktopClientOverride extension to override the origin embedded in
 * the resulting signed clientDataJSON.
 *
 * Requires that the user's browser is configured (typically via the
 * Chromium WebAuthenticationRemoteDesktopAllowedOrigins enterprise policy)
 * to permit this extension from this application's origin.
 */
angular.module('webauthn').factory('webAuthnService', ['$injector',
    function webAuthnService($injector) {

        const $q = $injector.get('$q');
        const service = {};

        /**
         * Whether WebAuthn passthrough is supported by the current browser.
         * Requires both the Web Authentication API and a Chromium-based
         * browser, since the origin-override mechanism this service relies
         * on (the remoteDesktopClientOverride extension, gated by the
         * WebAuthenticationRemoteDesktopAllowedOrigins policy) is currently
         * only implemented in Chromium.
         *
         * @type {Boolean}
         */
        service.isSupported = (function detectSupport() {

            if (!window.PublicKeyCredential)
                return false;

            // Modern: userAgentData.brands enumerates the engine brands
            const brands = navigator.userAgentData && navigator.userAgentData.brands;
            if (Array.isArray(brands)) {
                return brands.some(function (b) {
                    return /Chromium|Google Chrome|Microsoft Edge/i.test(b.brand);
                });
            }

            // Fallback: legacy userAgent sniff (Firefox advertises "Gecko"
            // but no Chrome/Chromium token; Safari has "Safari" but not
            // "Chrome" outside the Chrome marketing token used by Chromium)
            const ua = navigator.userAgent || '';
            return /Chrome|Chromium|Edg\//.test(ua) && !/Firefox|FxiOS/.test(ua);

        })();

        /**
         * Encodes the given ArrayBuffer (or ArrayBufferView) as a standard,
         * unpadded base64 string suitable for inclusion in JSON.
         *
         * @param {!ArrayBuffer|!ArrayBufferView} buf
         *     The buffer to encode.
         *
         * @returns {!String}
         *     The base64 encoding of the given buffer.
         */
        function base64Encode(buf) {
            const bytes = buf instanceof ArrayBuffer
                    ? new Uint8Array(buf)
                    : new Uint8Array(buf.buffer, buf.byteOffset, buf.byteLength);
            let str = '';
            for (let i = 0; i < bytes.length; i++)
                str += String.fromCharCode(bytes[i]);
            return btoa(str);
        }

        /**
         * Decodes the given base64 string into a new ArrayBuffer.
         *
         * @param {!String} encoded
         *     The base64-encoded string to decode.
         *
         * @returns {!ArrayBuffer}
         *     A new ArrayBuffer containing the decoded bytes.
         */
        function base64Decode(encoded) {
            const bin = atob(encoded);
            const bytes = new Uint8Array(bin.length);
            for (let i = 0; i < bin.length; i++)
                bytes[i] = bin.charCodeAt(i);
            return bytes.buffer;
        }

        /**
         * Walks the given value, replacing any object of the form
         * { "__ab": "<base64>" } with a real ArrayBuffer containing the
         * decoded bytes. Used to rehydrate BufferSource fields that were
         * serialized for transport over the Guacamole protocol.
         *
         * @param {*} value
         *     The value to rehydrate. May be any JSON-compatible value.
         *
         * @returns {*}
         *     The rehydrated value. Returned as-is if no rehydration is
         *     required.
         */
        function rehydrateBuffers(value) {

            if (value === null || typeof value !== 'object')
                return value;

            // The marker shape, as produced by the corresponding serializer
            // on the remote end
            if (typeof value.__ab === 'string'
                    && Object.keys(value).length === 1)
                return base64Decode(value.__ab);

            if (Array.isArray(value))
                return value.map(rehydrateBuffers);

            const result = {};
            for (const key in value)
                if (Object.prototype.hasOwnProperty.call(value, key))
                    result[key] = rehydrateBuffers(value[key]);
            return result;

        }

        /**
         * Converts a PublicKeyCredential returned from the Web Authentication
         * API into a plain JSON-compatible object suitable for transport over
         * the Guacamole protocol. All ArrayBuffer fields are base64-encoded.
         *
         * @param {!PublicKeyCredential} credential
         *     The credential to serialize.
         *
         * @returns {!Object}
         *     A plain object describing the credential.
         */
        function serializeCredential(credential) {

            const response = credential.response;
            const out = {
                id: credential.id,
                rawId: base64Encode(credential.rawId),
                type: credential.type,
                authenticatorAttachment: credential.authenticatorAttachment || null,
                clientExtensionResults: credential.getClientExtensionResults
                        ? credential.getClientExtensionResults() : {},
                response: {
                    clientDataJSON: base64Encode(response.clientDataJSON)
                }
            };

            // AuthenticatorAttestationResponse (create)
            if (response.attestationObject)
                out.response.attestationObject = base64Encode(response.attestationObject);

            // AuthenticatorAssertionResponse (get)
            if (response.authenticatorData)
                out.response.authenticatorData = base64Encode(response.authenticatorData);
            if (response.signature)
                out.response.signature = base64Encode(response.signature);
            if (response.userHandle)
                out.response.userHandle = base64Encode(response.userHandle);

            return out;

        }

        /**
         * Performs a WebAuthn ceremony of the given kind against the user's
         * local authenticator, using the remoteDesktopClientOverride
         * extension to embed the given origin in the resulting clientDataJSON.
         *
         * @param {!Object} request
         *     The ceremony request.
         *
         * @param {!String} request.kind
         *     Either "create" or "get".
         *
         * @param {!Object} request.opts
         *     The PublicKeyCredentialCreationOptions or
         *     PublicKeyCredentialRequestOptions for the ceremony, with any
         *     BufferSource fields serialized as { "__ab": "<base64>" }
         *     markers.
         *
         * @param {!String} request.originOverride
         *     The origin to embed in the resulting clientDataJSON. This is
         *     the origin of the remote page that initiated the ceremony, not
         *     this application's origin.
         *
         * @param {Boolean} [request.sameOriginWithAncestors=true]
         *     Whether the remote page that initiated the ceremony is
         *     same-origin with all its ancestor frames.
         *
         * @param {AbortSignal} [signal]
         *     Optional AbortSignal. Aborting it cancels the in-flight
         *     ceremony, dismisses the local authenticator UI, and
         *     rejects the returned promise with an AbortError.
         *
         * @returns {!Promise.<!Object>}
         *     A promise that resolves with the serialized credential, or
         *     rejects with an Error decorated with a "policyBlocked" property
         *     set to true if the browser refused the override due to policy.
         */
        service.performCeremony = function performCeremony(request, signal) {

            const deferred = $q.defer();

            if (!service.isSupported) {
                const err = new Error('WebAuthn is not available in this browser.');
                err.name = 'NotSupportedError';
                deferred.reject(err);
                return deferred.promise;
            }

            if (request.kind !== 'create' && request.kind !== 'get') {
                deferred.reject(new Error('Unknown ceremony kind: ' + request.kind));
                return deferred.promise;
            }

            const publicKey = rehydrateBuffers(request.opts || {});

            // Layer our origin override on top of any extensions provided by
            // the remote page
            publicKey.extensions = Object.assign({}, publicKey.extensions || {}, {
                remoteDesktopClientOverride: {
                    origin: request.originOverride,
                    sameOriginWithAncestors:
                            request.sameOriginWithAncestors !== false
                }
            });

            const options = { publicKey: publicKey };
            if (signal)
                options.signal = signal;

            const op = request.kind === 'create'
                    ? navigator.credentials.create(options)
                    : navigator.credentials.get(options);

            op.then(function ceremonyResolved(credential) {

                // The Credential Management API resolves null when
                // mediation is "silent" or "conditional" and no credential
                // is available. Treat that as a refusal so
                // serializeCredential never sees null.
                if (credential === null) {
                    const err = new Error(
                            'Authenticator returned no credential.');
                    err.name = 'NotAllowedError';
                    deferred.reject(err);
                    return;
                }

                deferred.resolve(serializeCredential(credential));

            }, function ceremonyRejected(error) {

                // Surface the policy-blocked case distinctly so the UI can
                // explain the WebAuthenticationRemoteDesktopAllowedOrigins
                // requirement. Two failure shapes both indicate the policy
                // is not honoring the override: NotAllowedError mentioning
                // the extension by name (policy present but origin not
                // allowlisted), and SecurityError from the RP-ID check
                // (policy missing entirely, so the extension is silently
                // ignored and Chrome falls through to standard validation
                // against the calling page's origin).
                if (error && typeof error.message === 'string') {
                    const msg = error.message;
                    if (error.name === 'NotAllowedError'
                            && msg.indexOf('remoteDesktopClientOverride') !== -1) {
                        error.policyBlocked = true;
                    }
                    else if (error.name === 'SecurityError'
                            && msg.indexOf('relying party ID') !== -1) {
                        error.policyBlocked = true;
                    }
                }

                deferred.reject(error);
            });

            return deferred.promise;

        };

        return service;

    }
]);
