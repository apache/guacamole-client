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
    var PermissionSet = function PermissionSet(template) {

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
         * Map of active connection identifiers to the corresponding array of
         * granted permissions. Each permission is represented by a string
         * listed within PermissionSet.ObjectPermissionType.
         *
         * @type Object.<String, String[]>
         */
        this.activeConnectionPermissions = template.activeConnectionPermissions || {};
        
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
     * Returns whether the given permission is granted for the active
     * connection having the given ID.
     *
     * @param {PermissionSet|Object} permSet
     *     The permission set to check.
     *
     * @param {String} type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *     
     * @param {String} identifier
     *     The identifier of the active connection to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    PermissionSet.hasActiveConnectionPermission = function hasActiveConnectionPermission(permSet, type, identifier) {
        return hasPermission(permSet.activeConnectionPermissions, type, identifier);
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

    /**
     * Adds the given system permission to the given permission set, if not
     * already present. If the permission is already present, this function has
     * no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to add, as defined by
     *     PermissionSet.SystemPermissionType.
     *
     * @returns {Boolean}
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    PermissionSet.addSystemPermission = function addSystemPermission(permSet, type) {

        // Add permission, if it doesn't already exist
        if (permSet.systemPermissions.indexOf(type) === -1) {
            permSet.systemPermissions.push(type);
            return true;
        }

        // Permission already present
        return false;

    };

    /**
     * Removes the given system permission from the given permission set, if
     * present. If the permission is not present, this function has no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to remove, as defined by
     *     PermissionSet.SystemPermissionType.
     *
     * @returns {Boolean}
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    PermissionSet.removeSystemPermission = function removeSystemPermission(permSet, type) {

        // Remove permission, if it exists
        var permLocation = permSet.systemPermissions.indexOf(type);
        if (permLocation !== -1) {
            permSet.systemPermissions.splice(permLocation, 1);
            return true;
        }

        // Permission not present
        return false;

    };

    /**
     * Adds the given permission applying to the arbitrary object with the 
     * given ID to the given permission set, if not already present. If the
     * permission is already present, this function has no effect.
     *
     * @param {Object.<String, String[]>} permMap
     *     The permission map to modify, where each entry maps an object
     *     identifer to the array of granted permissions.
     *
     * @param {String} type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the arbitrary object to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    var addObjectPermission = function addObjectPermission(permMap, type, identifier) {

        // Pull array of permissions, creating it if necessary
        var permArray = permMap[identifier] = permMap[identifier] || [];

        // Add permission, if it doesn't already exist
        if (permArray.indexOf(type) === -1) {
            permArray.push(type);
            return true;
        }

        // Permission already present
        return false;

    };

    /**
     * Removes the given permission applying to the arbitrary object with the 
     * given ID from the given permission set, if present. If the permission is
     * not present, this function has no effect.
     *
     * @param {Object.<String, String[]>} permMap
     *     The permission map to modify, where each entry maps an object
     *     identifer to the array of granted permissions.
     *
     * @param {String} type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the arbitrary object to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    var removeObjectPermission = function removeObjectPermission(permMap, type, identifier) {

        // Pull array of permissions
        var permArray = permMap[identifier];

        // If no permissions present at all, nothing to remove
        if (!(identifier in permMap))
            return false;

        // Remove permission, if it exists
        var permLocation = permArray.indexOf(type);
        if (permLocation !== -1) {
            permArray.splice(permLocation, 1);
            return true;
        }

        // Permission not present
        return false;

    };

    /**
     * Adds the given connection permission applying to the connection with
     * the given ID to the given permission set, if not already present. If the
     * permission is already present, this function has no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the connection to which the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    PermissionSet.addConnectionPermission = function addConnectionPermission(permSet, type, identifier) {
        return addObjectPermission(permSet.connectionPermissions, type, identifier);
    };

    /**
     * Removes the given connection permission applying to the connection with
     * the given ID from the given permission set, if present. If the
     * permission is not present, this function has no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the connection to which the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    PermissionSet.removeConnectionPermission = function removeConnectionPermission(permSet, type, identifier) {
        return removeObjectPermission(permSet.connectionPermissions, type, identifier);
    };

    /**
     * Adds the given connection group permission applying to the connection
     * group with the given ID to the given permission set, if not already
     * present. If the permission is already present, this function has no
     * effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the connection group to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    PermissionSet.addConnectionGroupPermission = function addConnectionGroupPermission(permSet, type, identifier) {
        return addObjectPermission(permSet.connectionGroupPermissions, type, identifier);
    };

    /**
     * Removes the given connection group permission applying to the connection
     * group with the given ID from the given permission set, if present. If
     * the permission is not present, this function has no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the connection group to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    PermissionSet.removeConnectionGroupPermission = function removeConnectionGroupPermission(permSet, type, identifier) {
        return removeObjectPermission(permSet.connectionGroupPermissions, type, identifier);
    };

    /**
     * Adds the given active connection permission applying to the connection
     * group with the given ID to the given permission set, if not already
     * present. If the permission is already present, this function has no
     * effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the active connection to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    PermissionSet.addActiveConnectionPermission = function addActiveConnectionPermission(permSet, type, identifier) {
        return addObjectPermission(permSet.activeConnectionPermissions, type, identifier);
    };

    /**
     * Removes the given active connection permission applying to the
     * connection group with the given ID from the given permission set, if
     * present. If the permission is not present, this function has no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the active connection to which the permission
     *     applies.
     *
     * @returns {Boolean}
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    PermissionSet.removeActiveConnectionPermission = function removeActiveConnectionPermission(permSet, type, identifier) {
        return removeObjectPermission(permSet.activeConnectionPermissions, type, identifier);
    };

    /**
     * Adds the given user permission applying to the user with the given ID to
     * the given permission set, if not already present. If the permission is
     * already present, this function has no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the user to which the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    PermissionSet.addUserPermission = function addUserPermission(permSet, type, identifier) {
        return addObjectPermission(permSet.userPermissions, type, identifier);
    };

    /**
     * Removes the given user permission applying to the user with the given ID
     * from the given permission set, if present. If the permission is not
     * present, this function has no effect.
     *
     * @param {PermissionSet} permSet
     *     The permission set to modify.
     *
     * @param {String} type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the user to whom the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    PermissionSet.removeUserPermission = function removeUserPermission(permSet, type, identifier) {
        return removeObjectPermission(permSet.userPermissions, type, identifier);
    };

    return PermissionSet;

}]);