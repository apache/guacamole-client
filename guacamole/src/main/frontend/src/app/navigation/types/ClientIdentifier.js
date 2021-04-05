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
 * Provides the ClientIdentifier class definition.
 */
angular.module('client').factory('ClientIdentifier', ['$injector',
    function defineClientIdentifier($injector) {

    // Required services
    var authenticationService = $injector.get('authenticationService');
    var $window               = $injector.get('$window');

    /**
     * Object which uniquely identifies a particular connection or connection
     * group within Guacamole. This object can be converted to/from a string to
     * generate a guaranteed-unique, deterministic identifier for client URLs.
     * 
     * @constructor
     * @param {ClientIdentifier|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ClientIdentifier.
     */
    var ClientIdentifier = function ClientIdentifier(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The identifier of the data source associated with the object to
         * which the client will connect. This identifier will be the
         * identifier of an AuthenticationProvider within the Guacamole web
         * application.
         *
         * @type String
         */
        this.dataSource = template.dataSource;

        /**
         * The type of object to which the client will connect. Possible values
         * are defined within ClientIdentifier.Types.
         *
         * @type String
         */
        this.type = template.type;

        /**
         * The unique identifier of the object to which the client will
         * connect.
         *
         * @type String
         */
        this.id = template.id;

    };

    /**
     * All possible ClientIdentifier types.
     *
     * @type Object.<String, String>
     */
    ClientIdentifier.Types = {

        /**
         * The type string for a Guacamole connection.
         *
         * @type String
         */
        CONNECTION : 'c',

        /**
         * The type string for a Guacamole connection group.
         *
         * @type String
         */
        CONNECTION_GROUP : 'g',

        /**
         * The type string for an active Guacamole connection.
         *
         * @type String
         */
        ACTIVE_CONNECTION : 'a'

    };

    /**
     * Converts the given ClientIdentifier or ClientIdentifier-like object to
     * a String representation. Any object having the same properties as
     * ClientIdentifier may be used, but only those properties will be taken
     * into account when producing the resulting String.
     *
     * @param {ClientIdentifier|Object} id
     *     The ClientIdentifier or ClientIdentifier-like object to convert to
     *     a String representation.
     *
     * @returns {String}
     *     A deterministic String representation of the given ClientIdentifier
     *     or ClientIdentifier-like object.
     */
    ClientIdentifier.toString = function toString(id) {
        return $window.btoa([
            id.id,
            id.type,
            id.dataSource
        ].join('\0'));
    };

    /**
     * Converts the given String into the corresponding ClientIdentifier. If
     * the provided String is not a valid identifier, it will be interpreted
     * as the identifier of a connection within the data source that
     * authenticated the current user.
     *
     * @param {String} str
     *     The String to convert to a ClientIdentifier.
     *
     * @returns {ClientIdentifier}
     *     The ClientIdentifier represented by the given String.
     */
    ClientIdentifier.fromString = function fromString(str) {

        try {
            var values = $window.atob(str).split('\0');
            return new ClientIdentifier({
                id         : values[0],
                type       : values[1],
                dataSource : values[2]
            });
        }

        // If the provided string is invalid, transform into a reasonable guess
        catch (e) {
            return new ClientIdentifier({
                id         : str,
                type       : ClientIdentifier.Types.CONNECTION,
                dataSource : authenticationService.getDataSource() || 'default'
            });
        }

    };

    return ClientIdentifier;

}]);
