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

var Guacamole = Guacamole || {};

/**
 * A Guacamole status. Each Guacamole status consists of a status code, defined
 * by the protocol, and an optional human-readable message, usually only
 * included for debugging convenience.
 *
 * @constructor
 * @param {Number} code
 *     The Guacamole status code, as defined by Guacamole.Status.Code.
 *
 * @param {String} [message]
 *     An optional human-readable message.
 */
Guacamole.Status = function(code, message) {

    /**
     * Reference to this Guacamole.Status.
     * @private
     */
    var guac_status = this;

    /**
     * The Guacamole status code.
     * @see Guacamole.Status.Code
     * @type {Number}
     */
    this.code = code;

    /**
     * An arbitrary human-readable message associated with this status, if any.
     * The human-readable message is not required, and is generally provided
     * for debugging purposes only. For user feedback, it is better to translate
     * the Guacamole status code into a message.
     * 
     * @type {String}
     */
    this.message = message;

    /**
     * Returns whether this status represents an error.
     * @returns {boolean} true if this status represents an error, false
     *                    otherwise.
     */
    this.isError = function() {
        return guac_status.code < 0 || guac_status.code > 0x00FF;
    };

};

/**
 * Enumeration of all Guacamole status codes.
 */
Guacamole.Status.Code = {

    /**
     * The operation succeeded.
     *
     * @type {Number}
     */
    "SUCCESS": 0x0000,

    /**
     * The requested operation is unsupported.
     *
     * @type {Number}
     */
    "UNSUPPORTED": 0x0100,

    /**
     * The operation could not be performed due to an internal failure.
     *
     * @type {Number}
     */
    "SERVER_ERROR": 0x0200,

    /**
     * The operation could not be performed as the server is busy.
     *
     * @type {Number}
     */
    "SERVER_BUSY": 0x0201,

    /**
     * The operation could not be performed because the upstream server is not
     * responding.
     *
     * @type {Number}
     */
    "UPSTREAM_TIMEOUT": 0x0202,

    /**
     * The operation was unsuccessful due to an error or otherwise unexpected
     * condition of the upstream server.
     *
     * @type {Number}
     */
    "UPSTREAM_ERROR": 0x0203,

    /**
     * The operation could not be performed as the requested resource does not
     * exist.
     *
     * @type {Number}
     */
    "RESOURCE_NOT_FOUND": 0x0204,

    /**
     * The operation could not be performed as the requested resource is
     * already in use.
     *
     * @type {Number}
     */
    "RESOURCE_CONFLICT": 0x0205,

    /**
     * The operation could not be performed as the requested resource is now
     * closed.
     *
     * @type {Number}
     */
    "RESOURCE_CLOSED": 0x0206,

    /**
     * The operation could not be performed because the upstream server does
     * not appear to exist.
     *
     * @type {Number}
     */
    "UPSTREAM_NOT_FOUND": 0x0207,

    /**
     * The operation could not be performed because the upstream server is not
     * available to service the request.
     *
     * @type {Number}
     */
    "UPSTREAM_UNAVAILABLE": 0x0208,

    /**
     * The session within the upstream server has ended because it conflicted
     * with another session.
     *
     * @type {Number}
     */
    "SESSION_CONFLICT": 0x0209,

    /**
     * The session within the upstream server has ended because it appeared to
     * be inactive.
     *
     * @type {Number}
     */
    "SESSION_TIMEOUT": 0x020A,

    /**
     * The session within the upstream server has been forcibly terminated.
     *
     * @type {Number}
     */
    "SESSION_CLOSED": 0x020B,

    /**
     * The operation could not be performed because bad parameters were given.
     *
     * @type {Number}
     */
    "CLIENT_BAD_REQUEST": 0x0300,

    /**
     * Permission was denied to perform the operation, as the user is not yet
     * authorized (not yet logged in, for example).
     *
     * @type {Number}
     */
    "CLIENT_UNAUTHORIZED": 0x0301,

    /**
     * Permission was denied to perform the operation, and this permission will
     * not be granted even if the user is authorized.
     *
     * @type {Number}
     */
    "CLIENT_FORBIDDEN": 0x0303,

    /**
     * The client took too long to respond.
     *
     * @type {Number}
     */
    "CLIENT_TIMEOUT": 0x0308,

    /**
     * The client sent too much data.
     *
     * @type {Number}
     */
    "CLIENT_OVERRUN": 0x030D,

    /**
     * The client sent data of an unsupported or unexpected type.
     *
     * @type {Number}
     */
    "CLIENT_BAD_TYPE": 0x030F,

    /**
     * The operation failed because the current client is already using too
     * many resources.
     *
     * @type {Number}
     */
    "CLIENT_TOO_MANY": 0x031D

};

/**
 * Returns the Guacamole protocol status code which most closely
 * represents the given HTTP status code.
 *
 * @param {Number} status
 *     The HTTP status code to translate into a Guacamole protocol status
 *     code.
 *
 * @returns {Number}
 *     The Guacamole protocol status code which most closely represents the
 *     given HTTP status code.
 */
Guacamole.Status.Code.fromHTTPCode = function fromHTTPCode(status) {

    // Translate status codes with known equivalents
    switch (status) {

        // HTTP 400 - Bad request
        case 400:
            return Guacamole.Status.Code.CLIENT_BAD_REQUEST;

        // HTTP 403 - Forbidden
        case 403:
            return Guacamole.Status.Code.CLIENT_FORBIDDEN;

        // HTTP 404 - Resource not found
        case 404:
            return Guacamole.Status.Code.RESOURCE_NOT_FOUND;

        // HTTP 429 - Too many requests
        case 429:
            return Guacamole.Status.Code.CLIENT_TOO_MANY;

        // HTTP 503 - Server unavailable
        case 503:
            return Guacamole.Status.Code.SERVER_BUSY;

    }

    // Default all other codes to generic internal error
    return Guacamole.Status.Code.SERVER_ERROR;

};

/**
 * Returns the Guacamole protocol status code which most closely
 * represents the given WebSocket status code.
 *
 * @param {Number} code
 *     The WebSocket status code to translate into a Guacamole protocol
 *     status code.
 *
 * @returns {Number}
 *     The Guacamole protocol status code which most closely represents the
 *     given WebSocket status code.
 */
Guacamole.Status.Code.fromWebSocketCode = function fromWebSocketCode(code) {

    // Translate status codes with known equivalents
    switch (code) {

        // Successful disconnect (no error)
        case 1000: // Normal Closure
            return Guacamole.Status.Code.SUCCESS;

        // Codes which indicate the server is not reachable
        case 1006: // Abnormal Closure (also signalled by JavaScript when the connection cannot be opened in the first place)
        case 1015: // TLS Handshake
            return Guacamole.Status.Code.UPSTREAM_NOT_FOUND;

        // Codes which indicate the server is reachable but busy/unavailable
        case 1001: // Going Away
        case 1012: // Service Restart
        case 1013: // Try Again Later
        case 1014: // Bad Gateway
            return Guacamole.Status.Code.UPSTREAM_UNAVAILABLE;

    }

    // Default all other codes to generic internal error
    return Guacamole.Status.Code.SERVER_ERROR;

};
