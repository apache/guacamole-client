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
 * A service for defining the PermissionFlagSet class.
 */
angular.module('rest').factory('PermissionFlagSet', ['PermissionSet',
    function definePermissionFlagSet(PermissionSet) {

    /**
     * Alternative view of a @link{PermissionSet} which allows manipulation of
     * each permission through the setting (or retrieval) of boolean property
     * values.
     * 
     * @constructor
     * @param {PermissionFlagSet|Object} template 
     *     The object whose properties should be copied within the new
     *     PermissionFlagSet.
     */
    var PermissionFlagSet = function PermissionFlagSet(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The granted state of each system permission, as a map of system
         * permission type string to boolean value. A particular permission is
         * granted if its corresponding boolean value is set to true. Valid
         * permission type strings are defined within
         * PermissionSet.SystemPermissionType. Permissions which are not
         * granted may be set to false, but this is not required.
         * 
         * @type Object.<String, Boolean>
         */
        this.systemPermissions = template.systemPermissions || {};

        /**
         * The granted state of each permission for each connection, as a map
         * of object permission type string to permission map. The permission
         * map is, in turn, a map of connection identifier to boolean value. A
         * particular permission is granted if its corresponding boolean value
         * is set to true. Valid permission type strings are defined within
         * PermissionSet.ObjectPermissionType. Permissions which are not
         * granted may be set to false, but this is not required.
         * 
         * @type Object.<String, Object.<String, Boolean>>
         */
        this.connectionPermissions = template.connectionPermissions || {
            'READ'       : {},
            'UPDATE'     : {},
            'DELETE'     : {},
            'ADMINISTER' : {}
        };

        /**
         * The granted state of each permission for each connection group, as a
         * map of object permission type string to permission map. The
         * permission map is, in turn, a map of connection group identifier to
         * boolean value. A particular permission is granted if its
         * corresponding boolean value is set to true. Valid permission type
         * strings are defined within PermissionSet.ObjectPermissionType.
         * Permissions which are not granted may be set to false, but this is
         * not required.
         * 
         * @type Object.<String, Object.<String, Boolean>>
         */
        this.connectionGroupPermissions = template.connectionGroupPermissions || {
            'READ'       : {},
            'UPDATE'     : {},
            'DELETE'     : {},
            'ADMINISTER' : {}
        };

        /**
         * The granted state of each permission for each active connection, as
         * a map of object permission type string to permission map. The
         * permission map is, in turn, a map of active connection identifier to
         * boolean value. A particular permission is granted if its
         * corresponding boolean value is set to true. Valid permission type
         * strings are defined within PermissionSet.ObjectPermissionType.
         * Permissions which are not granted may be set to false, but this is
         * not required.
         * 
         * @type Object.<String, Object.<String, Boolean>>
         */
        this.activeConnectionPermissions = template.activeConnectionPermissions || {
            'READ'       : {},
            'UPDATE'     : {},
            'DELETE'     : {},
            'ADMINISTER' : {}
        };

        /**
         * The granted state of each permission for each user, as a map of
         * object permission type string to permission map. The permission map
         * is, in turn, a map of username to boolean value. A particular
         * permission is granted if its corresponding boolean value is set to
         * true. Valid permission type strings are defined within
         * PermissionSet.ObjectPermissionType. Permissions which are not
         * granted may be set to false, but this is not required.
         * 
         * @type Object.<String, Object.<String, Boolean>>
         */
        this.userPermissions = template.userPermissions || {
            'READ'       : {},
            'UPDATE'     : {},
            'DELETE'     : {},
            'ADMINISTER' : {}
        };

    };

    /**
     * Iterates through all permissions in the given permission map, setting
     * the corresponding permission flags in the given permission flag map.
     *
     * @param {Object.<String, String[]>} permMap
     *     Map of object identifiers to the set of granted permissions. Each
     *     permission is represented by a string listed within
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {Object.<String, Object.<String, Boolean>>} flagMap
     *     Map of permission type strings to identifier/flag pairs representing
     *     whether the permission of that type is granted for the object having
     *     having the associated identifier.
     */
    var addObjectPermissions = function addObjectPermissions(permMap, flagMap) {

        // For each defined identifier in the permission map
        for (var identifier in permMap) {

            // Pull the permission array and loop through each permission
            var permissions = permMap[identifier];
            permissions.forEach(function addObjectPermission(type) {

                // Get identifier/flag mapping, creating first if necessary
                var objectFlags = flagMap[type] = flagMap[type] || {};

                // Set flag for current permission
                objectFlags[identifier] = true;

            });

        }

    };

    /**
     * Creates a new PermissionFlagSet, populating it with all the permissions
     * indicated as granted within the given PermissionSet.
     *
     * @param {PermissionSet} permissionSet
     *     The PermissionSet containing the permissions to be copied into a new
     *     PermissionFlagSet.
     *
     * @returns {PermissionFlagSet}
     *     A new PermissionFlagSet containing flags representing all granted
     *     permissions from the given PermissionSet.
     */
    PermissionFlagSet.fromPermissionSet = function fromPermissionSet(permissionSet) {

        var permissionFlagSet = new PermissionFlagSet();

        // Add all granted system permissions
        permissionSet.systemPermissions.forEach(function addSystemPermission(type) {
            permissionFlagSet.systemPermissions[type] = true;
        });

        // Add all granted connection permissions
        addObjectPermissions(permissionSet.connectionPermissions, permissionFlagSet.connectionPermissions);

        // Add all granted connection group permissions
        addObjectPermissions(permissionSet.connectionGroupPermissions, permissionFlagSet.connectionGroupPermissions);

        // Add all granted active connection permissions
        addObjectPermissions(permissionSet.activeConnectionPermissions, permissionFlagSet.activeConnectionPermissions);

        // Add all granted user permissions
        addObjectPermissions(permissionSet.userPermissions, permissionFlagSet.userPermissions);

        return permissionFlagSet;

    };

    return PermissionFlagSet;

}]);