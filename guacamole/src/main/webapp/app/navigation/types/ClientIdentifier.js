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
        CONNECTION_GROUP : 'g'

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
