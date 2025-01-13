

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
 * Returned by REST API calls when representing the permissions
 * granted to a specific user.
 */
export class PermissionSet {

    /**
     * Map of connection identifiers to the corresponding array of granted
     * permissions. Each permission is represented by a string listed
     * within PermissionSet.ObjectPermissionType.
     */
    connectionPermissions: Record<string, string[]>;

    /**
     * Map of connection group identifiers to the corresponding array of
     * granted permissions. Each permission is represented by a string
     * listed within PermissionSet.ObjectPermissionType.
     */
    connectionGroupPermissions: Record<string, string[]>;

    /**
     * Map of sharing profile identifiers to the corresponding array of
     * granted permissions. Each permission is represented by a string
     * listed within PermissionSet.ObjectPermissionType.
     */
    sharingProfilePermissions: Record<string, string[]>;

    /**
     * Map of active connection identifiers to the corresponding array of
     * granted permissions. Each permission is represented by a string
     * listed within PermissionSet.ObjectPermissionType.
     */
    activeConnectionPermissions: Record<string, string[]>;

    /**
     * Map of user identifiers to the corresponding array of granted
     * permissions. Each permission is represented by a string listed
     * within PermissionSet.ObjectPermissionType.
     */
    userPermissions: Record<string, string[]>;

    /**
     * Map of user group identifiers to the corresponding array of granted
     * permissions. Each permission is represented by a string listed
     * within PermissionSet.ObjectPermissionType.
     */
    userGroupPermissions: Record<string, string[]>;

    /**
     * Array of granted system permissions. Each permission is represented
     * by a string listed within PermissionSet.SystemPermissionType.
     */
    systemPermissions: string[];

    /**
     * Creates a new PermissionSet.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     PermissionSet.
     */
    constructor(template: Partial<PermissionSet> = {}) {
        this.connectionPermissions = template.connectionPermissions || {};
        this.connectionGroupPermissions = template.connectionGroupPermissions || {};
        this.sharingProfilePermissions = template.sharingProfilePermissions || {};
        this.activeConnectionPermissions = template.activeConnectionPermissions || {};
        this.userPermissions = template.userPermissions || {};
        this.userGroupPermissions = template.userGroupPermissions || {};
        this.systemPermissions = template.systemPermissions || [];
    }

    /**
     * Returns whether the given permission is granted for at least one
     * arbitrary object, regardless of ID.
     *
     * @param permMap
     *     The permission map to check, where each entry maps an object
     *     identifer to the array of granted permissions.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    private static containsPermission(permMap: Record<string, string[]>, type: PermissionSet.ObjectPermissionType): boolean {

        // Search all identifiers for given permission
        for (const identifier in permMap) {

            // If permission is granted, then no further searching is necessary
            if (permMap[identifier].indexOf(type) !== -1)
                return true;

        }

        // No such permission exists
        return false;
    }

    /**
     * Returns whether the given permission is granted for the arbitrary
     * object having the given ID. If no ID is given, this function determines
     * whether the permission is granted at all for any such arbitrary object.
     *
     * @param permMap
     *     The permission map to check, where each entry maps an object
     *     identifier to the array of granted permissions.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the object to which the permission applies.
     *
     * @returns {Boolean}
     *     true if the permission is present (granted), false otherwise.
     */
    private static hasPermission(permMap: Record<string, string[]>, type: PermissionSet.ObjectPermissionType, identifier?: string): boolean {

        // No permission if no permission map at all
        if (!permMap)
            return false;

        // If no identifier given, search ignoring the identifier
        if (!identifier)
            return PermissionSet.containsPermission(permMap, type);

        // If identifier not present at all, there are no such permissions
        if (!(identifier in permMap))
            return false;

        return permMap[identifier].indexOf(type) !== -1;

    }

    /**
     * Returns whether the given permission is granted for the connection
     * having the given ID.
     *
     * @param permSet
     *     The permission set to check.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the connection to which the permission applies.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    static hasConnectionPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier?: string): boolean {
        return PermissionSet.hasPermission(permSet.connectionPermissions, type, identifier);
    }

    /**
     * Returns whether the given permission is granted for the connection group
     * having the given ID.
     *
     * @param permSet
     *     The permission set to check.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the connection group to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    static hasConnectionGroupPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier?: string): boolean {
        return PermissionSet.hasPermission(permSet.connectionGroupPermissions, type, identifier);
    }

    /**
     * Returns whether the given permission is granted for the sharing profile
     * having the given ID.
     *
     * @param permSet
     *     The permission set to check.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the sharing profile to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    static hasSharingProfilePermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier?: string): boolean {
        return PermissionSet.hasPermission(permSet.sharingProfilePermissions, type, identifier);
    }

    /**
     * Returns whether the given permission is granted for the active
     * connection having the given ID.
     *
     * @param permSet
     *     The permission set to check.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the active connection to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    static hasActiveConnectionPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier?: string): boolean {
        return PermissionSet.hasPermission(permSet.activeConnectionPermissions, type, identifier);
    }

    /**
     * Returns whether the given permission is granted for the user having the
     * given ID.
     *
     * @param permSet
     *     The permission set to check.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user to which the permission applies.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    static hasUserPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier?: string): boolean {
        return PermissionSet.hasPermission(permSet.userPermissions, type, identifier);
    }

    /**
     * Returns whether the given permission is granted for the user group having
     * the given identifier.
     *
     * @param permSet
     *     The permission set to check.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user group to which the permission applies.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    static hasUserGroupPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier?: string): boolean {
        return PermissionSet.hasPermission(permSet.userGroupPermissions, type, identifier);
    }

    /**
     * Returns whether the given permission is granted at the system level.
     *
     * @param permSet
     *     The permission set to check.
     *
     * @param type
     *     The permission to search for, as defined by
     *     PermissionSet.SystemPermissionType.
     *
     * @returns
     *     true if the permission is present (granted), false otherwise.
     */
    static hasSystemPermission(permSet: PermissionSet, type: PermissionSet.SystemPermissionType): boolean {
        if (!permSet.systemPermissions) return false;
        return permSet.systemPermissions.indexOf(type) !== -1;
    }

    /**
     * Adds the given system permission to the given permission set, if not
     * already present. If the permission is already present, this function has
     * no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.SystemPermissionType.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addSystemPermission(permSet: PermissionSet, type: PermissionSet.SystemPermissionType): boolean {

        permSet.systemPermissions = permSet.systemPermissions || [];

        // Add permission, if it doesn't already exist
        if (permSet.systemPermissions.indexOf(type) === -1) {
            permSet.systemPermissions.push(type);
            return true;
        }

        // Permission already present
        return false;
    }

    /**
     * Removes the given system permission from the given permission set, if
     * present. If the permission is not present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.SystemPermissionType.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeSystemPermission(permSet: PermissionSet, type: PermissionSet.SystemPermissionType): boolean {

        permSet.systemPermissions = permSet.systemPermissions || [];

        // Remove permission, if it exists
        const permLocation = permSet.systemPermissions.indexOf(type);
        if (permLocation !== -1) {
            permSet.systemPermissions.splice(permLocation, 1);
            return true;
        }

        // Permission not present
        return false;

    }

    /**
     * Adds the given permission applying to the arbitrary object with the
     * given ID to the given permission set, if not already present. If the
     * permission is already present, this function has no effect.
     *
     * @param permMap
     *     The permission map to modify, where each entry maps an object
     *     identifier to the array of granted permissions.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the arbitrary object to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addObjectPermission(permMap: Record<string, string[]>, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {

        // Pull array of permissions, creating it if necessary
        const permArray = permMap[identifier] = permMap[identifier] || [];

        // Add permission, if it doesn't already exist
        if (permArray.indexOf(type) === -1) {
            permArray.push(type);
            return true;
        }

        // Permission already present
        return false;

    }

    /**
     * Removes the given permission applying to the arbitrary object with the
     * given ID from the given permission set, if present. If the permission is
     * not present, this function has no effect.
     *
     * @param permMap
     *     The permission map to modify, where each entry maps an object
     *     identifier to the array of granted permissions.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the arbitrary object to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeObjectPermission(permMap: Record<string, string[]>, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {

        // Pull array of permissions
        const permArray = permMap[identifier];

        // If no permissions present at all, nothing to remove
        if (!(identifier in permMap))
            return false;

        // Remove permission, if it exists
        const permLocation = permArray.indexOf(type);
        if (permLocation !== -1) {
            permArray.splice(permLocation, 1);
            return true;
        }

        // Permission not present
        return false;

    }

    /**
     * Adds the given connection permission applying to the connection with
     * the given ID to the given permission set, if not already present. If the
     * permission is already present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the connection to which the permission applies.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addConnectionPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.connectionPermissions = permSet.connectionPermissions || {};
        return PermissionSet.addObjectPermission(permSet.connectionPermissions, type, identifier);
    }

    /**
     * Removes the given connection permission applying to the connection with
     * the given ID from the given permission set, if present. If the
     * permission is not present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the connection to which the permission applies.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeConnectionPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.connectionPermissions = permSet.connectionPermissions || {};
        return PermissionSet.removeObjectPermission(permSet.connectionPermissions, type, identifier);
    }

    /**
     * Adds the given connection group permission applying to the connection
     * group with the given ID to the given permission set, if not already
     * present. If the permission is already present, this function has no
     * effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the connection group to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addConnectionGroupPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.connectionGroupPermissions = permSet.connectionGroupPermissions || {};
        return PermissionSet.addObjectPermission(permSet.connectionGroupPermissions, type, identifier);
    }

    /**
     * Removes the given connection group permission applying to the connection
     * group with the given ID from the given permission set, if present. If
     * the permission is not present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the connection group to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeConnectionGroupPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.connectionGroupPermissions = permSet.connectionGroupPermissions || {};
        return PermissionSet.removeObjectPermission(permSet.connectionGroupPermissions, type, identifier);
    }

    /**
     * Adds the given sharing profile permission applying to the sharing profile
     * with the given ID to the given permission set, if not already present. If
     * the permission is already present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the sharing profile to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addSharingProfilePermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.sharingProfilePermissions = permSet.sharingProfilePermissions || {};
        return PermissionSet.addObjectPermission(permSet.sharingProfilePermissions, type, identifier);
    }

    /**
     * Removes the given sharing profile permission applying to the sharing
     * profile with the given ID from the given permission set, if present. If
     * the permission is not present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the sharing profile to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeSharingProfilePermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.sharingProfilePermissions = permSet.sharingProfilePermissions || {};
        return PermissionSet.removeObjectPermission(permSet.sharingProfilePermissions, type, identifier);
    }

    /**
     * Adds the given active connection permission applying to the connection
     * group with the given ID to the given permission set, if not already
     * present. If the permission is already present, this function has no
     * effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the active connection to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addActiveConnectionPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.activeConnectionPermissions = permSet.activeConnectionPermissions || {};
        return PermissionSet.addObjectPermission(permSet.activeConnectionPermissions, type, identifier);
    }

    /**
     * Removes the given active connection permission applying to the
     * connection group with the given ID from the given permission set, if
     * present. If the permission is not present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the active connection to which the permission
     *     applies.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeActiveConnectionPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.activeConnectionPermissions = permSet.activeConnectionPermissions || {};
        return PermissionSet.removeObjectPermission(permSet.activeConnectionPermissions, type, identifier);
    }

    /**
     * Adds the given user permission applying to the user with the given ID to
     * the given permission set, if not already present. If the permission is
     * already present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user to which the permission applies.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addUserPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.userPermissions = permSet.userPermissions || {};
        return PermissionSet.addObjectPermission(permSet.userPermissions, type, identifier);
    }

    /**
     * Removes the given user permission applying to the user with the given ID
     * from the given permission set, if present. If the permission is not
     * present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user to whom the permission applies.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeUserPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.userPermissions = permSet.userPermissions || {};
        return PermissionSet.removeObjectPermission(permSet.userPermissions, type, identifier);
    }

    /**
     * Adds the given user group permission applying to the user group with the
     * given identifier to the given permission set, if not already present. If
     * the permission is already present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user group to which the permission applies.
     *
     * @returns
     *     true if the permission was added, false if the permission was
     *     already present in the given permission set.
     */
    static addUserGroupPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.userGroupPermissions = permSet.userGroupPermissions || {};
        return PermissionSet.addObjectPermission(permSet.userGroupPermissions, type, identifier);
    }

    /**
     * Removes the given user group permission applying to the user group with
     * the given identifier from the given permission set, if present. If the
     * permission is not present, this function has no effect.
     *
     * @param permSet
     *     The permission set to modify.
     *
     * @param type
     *     The permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param identifier
     *     The identifier of the user group to whom the permission applies.
     *
     * @returns
     *     true if the permission was removed, false if the permission was not
     *     present in the given permission set.
     */
    static removeUserGroupPermission(permSet: PermissionSet, type: PermissionSet.ObjectPermissionType, identifier: string): boolean {
        permSet.userGroupPermissions = permSet.userGroupPermissions || {};
        return PermissionSet.removeObjectPermission(permSet.userGroupPermissions, type, identifier);
    }

}

export namespace PermissionSet {

    /**
     * Valid object permission type strings.
     */
    export enum ObjectPermissionType {
        /**
         * Permission to read from the specified object.
         */
        READ = 'READ',

        /**
         * Permission to update the specified object.
         */
        UPDATE = 'UPDATE',

        /**
         * Permission to delete the specified object.
         */
        DELETE = 'DELETE',

        /**
         * Permission to administer the specified object
         */
        ADMINISTER = 'ADMINISTER'
    }

    /**
     * Valid system permission type strings.
     */
    export enum SystemPermissionType {
        /**
         * Permission to administer the entire system.
         */
        ADMINISTER = 'ADMINISTER',

        /**
         * Permission to view connection and user records for the entire system.
         */
        AUDIT = 'AUDIT',

        /**
         * Permission to create new users.
         */
        CREATE_USER = 'CREATE_USER',

        /**
         * Permission to create new user groups.
         */
        CREATE_USER_GROUP = 'CREATE_USER_GROUP',

        /**
         * Permission to create new connections.
         */
        CREATE_CONNECTION = 'CREATE_CONNECTION',

        /**
         * Permission to create new connection groups.
         */
        CREATE_CONNECTION_GROUP = 'CREATE_CONNECTION_GROUP',

        /**
         * Permission to create new sharing profiles.
         */
        CREATE_SHARING_PROFILE = 'CREATE_SHARING_PROFILE'
    }
}
