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
 * Service which defines the ConnectionGroup class.
 */
angular.module('rest').factory('ConnectionGroup', [function defineConnectionGroup() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with a connection group.
     * 
     * @constructor
     * @param {ConnectionGroup|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ConnectionGroup.
     */
    var ConnectionGroup = function ConnectionGroup(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The unique identifier associated with this connection group.
         *
         * @type String
         */
        this.identifier = template.identifier;

        /**
         * The unique identifier of the connection group that contains this
         * connection group.
         * 
         * @type String
         * @default ConnectionGroup.ROOT_IDENTIFIER
         */
        this.parentIdentifier = template.parentIdentifier || ConnectionGroup.ROOT_IDENTIFIER;

        /**
         * The human-readable name of this connection group, which is not
         * necessarily unique.
         * 
         * @type String
         */
        this.name = template.name;

        /**
         * The type of this connection group, which may be either
         * ConnectionGroup.Type.ORGANIZATIONAL or
         * ConnectionGroup.Type.BALANCING.
         * 
         * @type String
         * @default ConnectionGroup.Type.ORGANIZATIONAL
         */
        this.type = template.type || ConnectionGroup.Type.ORGANIZATIONAL;

        /**
         * An array of all child connections, if known. This property may be
         * null or undefined if children have not been queried, and thus the
         * child connections are unknown.
         *
         * @type Connection[]
         */
        this.childConnections = template.childConnections;

        /**
         * An array of all child connection groups, if known. This property may
         * be null or undefined if children have not been queried, and thus the
         * child connection groups are unknown.
         *
         * @type ConnectionGroup[]
         */
        this.childConnectionGroups = template.childConnectionGroups;

        /**
         * The count of currently active connections using this connection
         * group. This field will be returned from the REST API during a get
         * operation, but manually setting this field will have no effect.
         * 
         * @type Number
         */
        this.activeConnections = template.activeConnections;

    };

    /**
     * The reserved identifier which always represents the root connection
     * group.
     * 
     * @type String
     */
    ConnectionGroup.ROOT_IDENTIFIER = "ROOT";

    /**
     * All valid connection group types.
     */
    ConnectionGroup.Type = {

        /**
         * The type string associated with balancing connection groups.
         *
         * @type String
         */
        BALANCING : "BALANCING",

        /**
         * The type string associated with organizational connection groups.
         *
         * @type String
         */
        ORGANIZATIONAL : "ORGANIZATIONAL"

    };

    return ConnectionGroup;

}]);