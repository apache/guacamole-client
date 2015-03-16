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
 * Service which defines the ActiveTunnel class.
 */
angular.module('rest').factory('ActiveTunnel', [function defineActiveTunnel() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with an active tunnel. Each tunnel denotes an active
     * connection, uniquely identified by the tunnel UUID.
     * 
     * @constructor
     * @param {ActiveTunnel|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ActiveTunnel.
     */
    var ActiveTunnel = function ActiveTunnel(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The identifier of the connection associated with this tunnel.
         *
         * @type String
         */
        this.identifier = template.identifier;

        /**
         * The time that the tunnel began, in seconds since
         * 1970-01-01 00:00:00 UTC.
         *
         * @type Number 
         */
        this.startDate = template.startDate;

        /**
         * The remote host that initiated the tunnel, if known.
         *
         * @type String
         */
        this.remoteHost = template.remoteHost;

        /**
         * The username of the user associated with the tunnel.
         * 
         * @type String
         */
        this.username = template.username;

        /**
         * The UUID which uniquely identifies the tunnel.
         * 
         * @type String
         */
        this.uuid = template.uuid;

    };

    return ActiveTunnel;

}]);