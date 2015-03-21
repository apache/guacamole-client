/*
 * Copyright (C) 2015 Glyptodon LLC
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
 * Service which defines the ActiveConnection class.
 */
angular.module('rest').factory('ActiveConnection', [function defineActiveConnection() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with an active connection. Each active connection is
     * effectively a pairing of a connection and the user currently using it,
     * along with other information.
     * 
     * @constructor
     * @param {ActiveConnection|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ActiveConnection.
     */
    var ActiveConnection = function ActiveConnection(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The identifier which uniquely identifies this specific active
         * connection.
         * 
         * @type String
         */
        this.identifier = template.identifier;

        /**
         * The identifier of the connection associated with this active
         * connection.
         *
         * @type String
         */
        this.connectionIdentifier = template.connectionIdentifier;

        /**
         * The time that the connection began, in seconds since
         * 1970-01-01 00:00:00 UTC.
         *
         * @type Number 
         */
        this.startDate = template.startDate;

        /**
         * The remote host that initiated the connection, if known.
         *
         * @type String
         */
        this.remoteHost = template.remoteHost;

        /**
         * The username of the user associated with the connection.
         * 
         * @type String
         */
        this.username = template.username;

    };

    return ActiveConnection;

}]);