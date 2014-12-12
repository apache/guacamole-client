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
 * Service which defines the Permission class.
 */
angular.module('rest').factory('Permission', [function definePermission() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with a supported remote desktop protocol.
     * 
     * @constructor
     * @param {Permission|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     Permission.
     */
    var Permission = function Permission(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The type of object associated with this permission.
         *
         * @type String
         */
        this.objectType = template.objectType;

        /**
         * The identifier of the specific object associated with this
         * permission. If the objectType is Permission.ObjectType.SYSTEM, this
         * property is not applicable.
         * 
         * @type String
         */
        this.objectIdentifier = template.objectIdentifier;

        /**
         * The type of this permission, representing the actions granted if
         * this permission is present, such as the ability to read or update
         * specific objects. Legal values are specified within
         * Permission.Type and depend on this permission's objectType.
         *
         * @type String
         */
        this.permissionType = template.permissionType;

    };

    /**
     * Valid object type strings.
     */
    Permission.ObjectType = {

        /**
         * The permission refers to a specific connection, identified by the
         * value of objectIdentifier.
         */
        CONNECTION : "CONNECTION",

        /**
         * The permission refers to a specific connection group, identified by
         * the value of objectIdentifier.
         */
        CONNECTION_GROUP : "CONNECTION_GROUP",

        /**
         * The permission refers to a specific user, identified by the value of
         * objectIdentifier.
         */
        USER : "USER",

        /**
         * The permission refers to the system as a whole, and the
         * objectIdentifier propery is not applicable.
         */
        SYSTEM : "SYSTEM"

    };

    /**
     * Valid permission type strings.
     */
    Permission.Type = {

        /**
         * Permission to read from the specified object. This permission type
         * does not apply to SYSTEM permissions.
         */
        READ : "READ",

        /**
         * Permission to update the specified object. This permission type does
         * not apply to SYSTEM permissions.
         */
        UPDATE : "UPDATE",

        /**
         * Permission to delete the specified object. This permission type does
         * not apply to SYSTEM permissions.
         */
        DELETE : "DELETE",

        /**
         * Permission to administer the specified object or, if the permission
         * refers to the system as a whole, permission to administer the entire
         * system.
         */
        ADMINISTER : "ADMINISTER",

        /**
         * Permission to create new users. This permission type may only be
         * applied to the system as a whole.
         */
        CREATE_USER : "CREATE_USER",

        /**
         * Permission to create new connections. This permission type may only
         * be applied to the system as a whole.
         */
        CREATE_CONNECTION : "CREATE_CONNECTION",

        /**
         * Permission to create new connection groups. This permission type may
         * only be applied to the system as a whole.
         */
        CREATE_CONNECTION_GROUP : "CREATE_CONNECTION_GROUP"

    };

    return Permission;

}]);