/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Provides the ManagedClient class used by the guacClientManager service.
 */
angular.module('client').factory('ManagedClientState', [function defineManagedClientState() {

    /**
     * Object which represents the state of a Guacamole client and its tunnel,
     * including any error conditions.
     * 
     * @constructor
     * @param {ManagedClientState|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedClientState.
     */
    var ManagedClientState = function ManagedClientState(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The current connection state. Valid values are described by
         * ManagedClientState.ConnectionState.
         *
         * @type String
         * @default ManagedClientState.ConnectionState.IDLE
         */
        this.connectionState = template.connectionState || ManagedClientState.ConnectionState.IDLE;

        /**
         * The status code of the current error condition, if connectionState
         * is CLIENT_ERROR or TUNNEL_ERROR. For all other connectionState
         * values, this will be @link{Guacamole.Status.Code.SUCCESS}.
         *
         * @type Number
         * @default Guacamole.Status.Code.SUCCESS
         */
        this.statusCode = template.statusCode || Guacamole.Status.Code.SUCCESS;

    };

    /**
     * Valid connection state strings. Each state string is associated with a
     * specific state of a Guacamole connection.
     */
    ManagedClientState.ConnectionState = {

        /**
         * The Guacamole connection has not yet been attempted.
         * 
         * @type String
         */
        IDLE : "IDLE",

        /**
         * The Guacamole connection is being established.
         * 
         * @type String
         */
        CONNECTING : "CONNECTING",

        /**
         * The Guacamole connection has been successfully established, and the
         * client is now waiting for receipt of initial graphical data.
         * 
         * @type String
         */
        WAITING : "WAITING",

        /**
         * The Guacamole connection has been successfully established, and
         * initial graphical data has been received.
         * 
         * @type String
         */
        CONNECTED : "CONNECTED",

        /**
         * The Guacamole connection has terminated successfully. No errors are
         * indicated.
         * 
         * @type String
         */
        DISCONNECTED : "DISCONNECTED",

        /**
         * The Guacamole connection has terminated due to an error reported by
         * the client. The associated error code is stored in statusCode.
         * 
         * @type String
         */
        CLIENT_ERROR : "CLIENT_ERROR",

        /**
         * The Guacamole connection has terminated due to an error reported by
         * the tunnel. The associated error code is stored in statusCode.
         * 
         * @type String
         */
        TUNNEL_ERROR : "TUNNEL_ERROR"

    };

    /**
     * Sets the current client state and, if given, the associated status code.
     * If an error is already represented, this function has no effect.
     *
     * @param {ManagedClientState} clientState
     *     The ManagedClientState to update.
     *
     * @param {String} connectionState
     *     The connection state to assign to the given ManagedClientState, as
     *     listed within ManagedClientState.ConnectionState.
     * 
     * @param {Number} [statusCode]
     *     The status code to assign to the given ManagedClientState, if any,
     *     as listed within Guacamole.Status.Code. If no status code is
     *     specified, the status code of the ManagedClientState is not touched.
     */
    ManagedClientState.setConnectionState = function(clientState, connectionState, statusCode) {

        // Do not set state after an error is registered
        if (clientState.connectionState === ManagedClientState.ConnectionState.TUNNEL_ERROR
         || clientState.connectionState === ManagedClientState.ConnectionState.CLIENT_ERROR)
            return;

        // Update connection state
        clientState.connectionState = connectionState;

        // Set status code, if given
        if (statusCode)
            clientState.statusCode = statusCode;

    };

    return ManagedClientState;

}]);