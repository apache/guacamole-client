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

import { PermissionSet } from './PermissionSet';

/**
 * Alternative view of a @link{PermissionSet} which allows manipulation of
 * each permission through the setting (or retrieval) of boolean property
 * values.
 */
export class PermissionFlagSet {

    /**
     * The granted state of each system permission, as a map of system
     * permission type string to boolean value. A particular permission is
     * granted if its corresponding boolean value is set to true. Valid
     * permission type strings are defined within
     * PermissionSet.SystemPermissionType. Permissions which are not
     * granted may be set to false, but this is not required.
     */
    systemPermissions: Record<string, boolean>;

    /**
     * The granted state of each permission for each connection, as a map
     * of object permission type string to permission map. The permission
     * map is, in turn, a map of connection identifier to boolean value. A
     * particular permission is granted if its corresponding boolean value
     * is set to true. Valid permission type strings are defined within
     * PermissionSet.ObjectPermissionType. Permissions which are not
     * granted may be set to false, but this is not required.
     */
    connectionPermissions: Record<string, Record<string, boolean>>;

    /**
     * The granted state of each permission for each connection group, as a
     * map of object permission type string to permission map. The
     * permission map is, in turn, a map of connection group identifier to
     * boolean value. A particular permission is granted if its
     * corresponding boolean value is set to true. Valid permission type
     * strings are defined within PermissionSet.ObjectPermissionType.
     * Permissions which are not granted may be set to false, but this is
     * not required.
     */
    connectionGroupPermissions: Record<string, Record<string, boolean>>;

    /**
     * The granted state of each permission for each sharing profile, as a
     * map of object permission type string to permission map. The
     * permission map is, in turn, a map of sharing profile identifier to
     * boolean value. A particular permission is granted if its
     * corresponding boolean value is set to true. Valid permission type
     * strings are defined within PermissionSet.ObjectPermissionType.
     * Permissions which are not granted may be set to false, but this is
     * not required.
     */
    sharingProfilePermissions: Record<string, Record<string, boolean>>;

    /**
     * The granted state of each permission for each active connection, as
     * a map of object permission type string to permission map. The
     * permission map is, in turn, a map of active connection identifier to
     * boolean value. A particular permission is granted if its
     * corresponding boolean value is set to true. Valid permission type
     * strings are defined within PermissionSet.ObjectPermissionType.
     * Permissions which are not granted may be set to false, but this is
     * not required.
     */
    activeConnectionPermissions: Record<string, Record<string, boolean>>;

    /**
     * The granted state of each permission for each user, as a map of
     * object permission type string to permission map. The permission map
     * is, in turn, a map of username to boolean value. A particular
     * permission is granted if its corresponding boolean value is set to
     * true. Valid permission type strings are defined within
     * PermissionSet.ObjectPermissionType. Permissions which are not
     * granted may be set to false, but this is not required.
     */
    userPermissions: Record<string, Record<string, boolean>>;

    /**
     * The granted state of each permission for each user group, as a map of
     * object permission type string to permission map. The permission map
     * is, in turn, a map of group identifier to boolean value. A particular
     * permission is granted if its corresponding boolean value is set to
     * true. Valid permission type strings are defined within
     * PermissionSet.ObjectPermissionType. Permissions which are not
     * granted may be set to false, but this is not required.
     */
    userGroupPermissions: Record<string, Record<string, boolean>>;

    /**
     * Creates a new PermissionFlagSet.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     PermissionFlagSet.
     */
    constructor(template: Partial<PermissionFlagSet> = {}) {

        this.systemPermissions = template.systemPermissions || {};

        this.connectionPermissions = template.connectionPermissions || {
            'READ'      : {},
            'UPDATE'    : {},
            'DELETE'    : {},
            'ADMINISTER': {}
        };

        this.connectionGroupPermissions = template.connectionGroupPermissions || {
            'READ'      : {},
            'UPDATE'    : {},
            'DELETE'    : {},
            'ADMINISTER': {}
        };

        this.sharingProfilePermissions = template.sharingProfilePermissions || {
            'READ'      : {},
            'UPDATE'    : {},
            'DELETE'    : {},
            'ADMINISTER': {}
        };

        this.activeConnectionPermissions = template.activeConnectionPermissions || {
            'READ'      : {},
            'UPDATE'    : {},
            'DELETE'    : {},
            'ADMINISTER': {}
        };

        this.userPermissions = template.userPermissions || {
            'READ'      : {},
            'UPDATE'    : {},
            'DELETE'    : {},
            'ADMINISTER': {}
        };

        this.userGroupPermissions = template.userGroupPermissions || {
            'READ'      : {},
            'UPDATE'    : {},
            'DELETE'    : {},
            'ADMINISTER': {}
        };
    }

    /**
     * Creates a new PermissionFlagSet, populating it with all the permissions
     * indicated as granted within the given PermissionSet.
     *
     * @param permissionSet
     *     The PermissionSet containing the permissions to be copied into a new
     *     PermissionFlagSet.
     *
     * @returns
     *     A new PermissionFlagSet containing flags representing all granted
     *     permissions from the given PermissionSet.
     */
    static fromPermissionSet(permissionSet: PermissionSet): PermissionFlagSet {

        const permissionFlagSet = new PermissionFlagSet();

        // Add all granted system permissions
        permissionSet.systemPermissions.forEach(function addSystemPermission(type) {
            permissionFlagSet.systemPermissions[type] = true;
        });

        // Add all granted connection permissions
        PermissionFlagSet.addObjectPermissions(permissionSet.connectionPermissions, permissionFlagSet.connectionPermissions);

        // Add all granted connection group permissions
        PermissionFlagSet.addObjectPermissions(permissionSet.connectionGroupPermissions, permissionFlagSet.connectionGroupPermissions);

        // Add all granted sharing profile permissions
        PermissionFlagSet.addObjectPermissions(permissionSet.sharingProfilePermissions, permissionFlagSet.sharingProfilePermissions);

        // Add all granted active connection permissions
        PermissionFlagSet.addObjectPermissions(permissionSet.activeConnectionPermissions, permissionFlagSet.activeConnectionPermissions);

        // Add all granted user permissions
        PermissionFlagSet.addObjectPermissions(permissionSet.userPermissions, permissionFlagSet.userPermissions);

        // Add all granted user group permissions
        PermissionFlagSet.addObjectPermissions(permissionSet.userGroupPermissions, permissionFlagSet.userGroupPermissions);

        return permissionFlagSet;

    }

    /**
     * Iterates through all permissions in the given permission map, setting
     * the corresponding permission flags in the given permission flag map.
     *
     * @param permMap
     *     Map of object identifiers to the set of granted permissions. Each
     *     permission is represented by a string listed within
     *     PermissionSet.ObjectPermissionType.
     *
     * @param flagMap
     *     Map of permission type strings to identifier/flag pairs representing
     *     whether the permission of that type is granted for the object having
     *     the associated identifier.
     */
    private static addObjectPermissions(permMap: Record<string, string[]>, flagMap: Record<string, Record<string, boolean>>): void {

        // For each defined identifier in the permission map
        for (const identifier in permMap) {

            // Pull the permission array and loop through each permission
            const permissions = permMap[identifier];
            permissions.forEach(function addObjectPermission(type) {

                // Get identifier/flag mapping, creating first if necessary
                const objectFlags = flagMap[type] = flagMap[type] || {};

                // Set flag for current permission
                objectFlags[identifier] = true;

            });

        }

    }
}
