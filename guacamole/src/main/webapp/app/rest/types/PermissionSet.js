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
 * Service which defines the PermissionSet class.
 */
angular.module('rest').factory('PermissionSet', [function definePermissionSet() {
            
    /**
     * The object returned by REST API calls when representing the permissions
     * granted to a specific user.
     * 
     * @constructor
     * @param {PermissionSet|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     PermissionSet.
     */
    var PermissionSet = function Permission(template) {

        // Use empty object by default
        template = template || {};

        /**
         * Map of connection identifiers to the corresponding array of granted
         * permissions. Each permission is represented by a string listed
         * within PermissionSet.ObjectPermissionType.
         *
         * @type Object.<String, String[]>
         */
        this.connectionPermissions = template.connectionPermissions || {};

        /**
         * Map of connection group identifiers to the corresponding array of
         * granted permissions. Each permission is represented by a string
         * listed within PermissionSet.ObjectPermissionType.
         *
         * @type Object.<String, String[]>
         */
        this.connectionGroupPermissions = template.connectionGroupPermissions || {};
        
        /**
         * Map of user identifiers to the corresponding array of granted
         * permissions. Each permission is represented by a string listed
         * within PermissionSet.ObjectPermissionType.
         *
         * @type Object.<String, String[]>
         */
        this.userPermissions = template.userPermissions || {};

        /**
         * Array of granted system permissions. Each permission is represented
         * by a string listed within PermissionSet.SystemPermissionType.
         *
         * @type String[]
         */
        this.systemPermissions = template.systemPermissions || [];

    };

    /**
     * Valid object permission type strings.
     */
    PermissionSet.ObjectPermissionType = {

        /**
         * Permission to read from the specified object.
         */
        READ : "READ",

        /**
         * Permission to update the specified object.
         */
        UPDATE : "UPDATE",

        /**
         * Permission to delete the specified object.
         */
        DELETE : "DELETE",

        /**
         * Permission to administer the specified object
         */
        ADMINISTER : "ADMINISTER"

    };

    /**
     * Valid system permission type strings.
     */
    PermissionSet.SystemPermissionType = {

        /**
         * Permission to administer the entire system.
         */
        ADMINISTER : "ADMINISTER",

        /**
         * Permission to create new users.
         */
        CREATE_USER : "CREATE_USER",

        /**
         * Permission to create new connections.
         */
        CREATE_CONNECTION : "CREATE_CONNECTION",

        /**
         * Permission to create new connection groups.
         */
        CREATE_CONNECTION_GROUP : "CREATE_CONNECTION_GROUP"

    };

    /**
     * Returns whether the given permission is granted for at least one
     * arbitrary object, regardless of ID.
     *
     * @param {Object.<String, String[]>} permMap
     *     The permission map to check, where each entry maps an object
     *     identifer to the array of granted permissions.
     *
     * @param {String} type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *     
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    var containsPermission = function containsPermission(permMap, type) {

        // Search all identifiers for given permission
        for (var identifier in permMap) {

            // If permission is granted, then no further searching is necessary
            if (permMap[identifier].indexOf(type) !== -1)
                return true;

        }

        // No such permission exists
        return false;

    };

    /**
     * Returns whether the given permission is granted for the arbitrary
     * object having the given ID. If no ID is given, this function determines
     * whether the permission is granted at all for any such arbitrary object.
     *
     * @param {Object.<String, String[]>} permMap
     *     The permission map to check, where each entry maps an object
     *     identifer to the array of granted permissions.
     *
     * @param {String} type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *     
     * @param {String} [identifier]
     *     The identifier of the object to which the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    var hasPermission = function hasPermission(permMap, type, identifier) {

        // If no identifier given, search ignoring the identifier
        if (!identifier)
            return containsPermission(permMap, type);

        // If identifier not present at all, there are no such permissions
        if (!(identifier in permMap))
            return false;

        return permMap[identifier].indexOf(type) !== -1;

    };

    /**
     * Returns whether the given permission is granted for the connection
     * having the given ID.
     *
     * @param {PermissionSet|Object} permSet
     *     The permission set to check.
     *
     * @param {String} type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *     
     * @param {String} identifier
     *     The identifier of the connection to which the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    PermissionSet.hasConnectionPermission = function hasConnectionPermission(permSet, type, identifier) {
        return hasPermission(permSet.connectionPermissions, type, identifier);
    };

    /**
     * Returns whether the given permission is granted for the connection group
     * having the given ID.
     *
     * @param {PermissionSet|Object} permSet
     *     The permission set to check.
     *
     * @param {String} type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *     
     * @param {String} identifier
     *     The identifier of the connection group to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    PermissionSet.hasConnectionGroupPermission = function hasConnectionGroupPermission(permSet, type, identifier) {
        return hasPermission(permSet.connectionGroupPermissions, type, identifier);
    };

    /**
     * Returns whether the given permission is granted for the user having the 
     * given ID.
     *
     * @param {PermissionSet|Object} permSet
     *     The permission set to check.
     *
     * @param {String} type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *     
     * @param {String} identifier
     *     The identifier of the user to which the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    PermissionSet.hasUserPermission = function hasUserPermission(permSet, type, identifier) {
        return hasPermission(permSet.userPermissions, type, identifier);
    };

    /**
     * Returns whether the given permission is granted at the system level.
     *
     * @param {PermissionSet|Object} permSet
     *     The permission set to check.
     *
     * @param {String} type
     *     The permission to search for, as defined by
     *     PermissionSet.SystemPermissionType.
     *
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    PermissionSet.hasSystemPermission = function hasSystemPermission(permSet, type) {
        return permSet.systemPermissions.indexOf(type) !== -1;
    };

    return PermissionSet;

}]);