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

package org.glyptodon.guacamole.net.basic.rest.permission;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.net.auth.permission.ConnectionGroupPermission;
import org.glyptodon.guacamole.net.auth.permission.ConnectionPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.UserPermission;

/**
 * The set of permissions which are granted to a specific user, organized by
 * object type and, if applicable, identifier. This object can be constructed
 * with arbitrary permissions present, or manipulated after creation through
 * the manipulation or replacement of its collections of permissions, but is
 * otherwise not intended for internal use as a data structure for permissions.
 * Its primary purpose is as a hierarchical format for exchanging granted
 * permissions with REST clients.
 */
public class APIPermissionSet {

    /**
     * Map of connection ID to the set of granted permissions.
     */
    private Map<String, EnumSet<ObjectPermission.Type>> connectionPermissions = new HashMap<String, EnumSet<ObjectPermission.Type>>();

    /**
     * Map of connection group ID to the set of granted permissions.
     */
    private Map<String, EnumSet<ObjectPermission.Type>> connectionGroupPermissions = new HashMap<String, EnumSet<ObjectPermission.Type>>();

    /**
     * Map of user ID to the set of granted permissions.
     */
    private Map<String, EnumSet<ObjectPermission.Type>> userPermissions = new HashMap<String, EnumSet<ObjectPermission.Type>>();

    /**
     * Set of all granted system-level permissions.
     */
    private EnumSet<SystemPermission.Type> systemPermissions = EnumSet.noneOf(SystemPermission.Type.class);

    /**
     * Adds the given object permission to the given map of object identifier
     * to permission set.
     *
     * @param permissions
     *     The map to add the given permission to.
     *
     * @param permission
     *     The permission to add.
     */
    private void addPermission(Map<String, EnumSet<ObjectPermission.Type>> permissions, ObjectPermission<String> permission) {

        // Pull set of permissions for given object
        String id = permission.getObjectIdentifier();
        EnumSet<ObjectPermission.Type> types = permissions.get(id);

        // If set does not yet exist, create it
        if (types == null) {
            types = EnumSet.of(permission.getType());
            permissions.put(id, types);
        }

        // Otherwise, add the specified permission
        else
            types.add(permission.getType());

    }

    /**
     * Adds the given system-level permission to the given set of granted
     * system permissions.
     *
     * @param permissions
     *     The set of system permissions to add the given permission to.
     *
     * @param permission
     *     The permission to add.
     */
    private void addPermission(EnumSet<SystemPermission.Type> permissions, SystemPermission permission) {
        permissions.add(permission.getType());
    }

    /**
     * Adds the given permission to the appropriate type-specific set or map of
     * permissions based on the permission class. Only connection, connection
     * group, user, and system permissions are supported. Unsupported
     * permission types will result in a GuacamoleException being thrown.
     *
     * @param permission The permission to add.
     * @throws GuacamoleException If the permission is of an unsupported type.
     */
    private void addPermission(Permission<?> permission) throws GuacamoleException {

        // Connection permissions
        if (permission instanceof ConnectionPermission)
            addPermission(connectionPermissions, (ConnectionPermission) permission);

        // Connection group permissions
        else if (permission instanceof ConnectionGroupPermission)
            addPermission(connectionGroupPermissions, (ConnectionGroupPermission) permission);

        // User permissions
        else if (permission instanceof UserPermission)
            addPermission(userPermissions, (UserPermission) permission);

        // System permissions
        else if (permission instanceof SystemPermission)
            addPermission(systemPermissions, (SystemPermission) permission);

        // Unknown / unsupported permission type
        else
            throw new GuacamoleServerException("Serialization of permission type \"" + permission.getClass() + "\" not implemented.");

    }

    /**
     * Creates a new permission set which contains no granted permissions. Any
     * permissions must be added by manipulating or replacing the applicable
     * permission collection.
     */
    public APIPermissionSet() {
    }

    /**
     * Creates a new permission set having the given permissions.
     *
     * @param permissions
     *     The permissions to initially store within the permission set.
     *
     * @throws GuacamoleException
     *     If any of the given permissions are of an unsupported type.
     */
    public APIPermissionSet(Iterable<Permission> permissions) throws GuacamoleException {

        // Add all provided permissions 
        for (Permission permission : permissions)
            addPermission(permission);

    }

    /**
     * Creates a new permission set having the given permissions.
     *
     * @param permissions
     *     The permissions to initially store within the permission set.
     *
     * @throws GuacamoleException
     *     If any of the given permissions are of an unsupported type.
     */
    public APIPermissionSet(Permission... permissions) throws GuacamoleException {

        // Add all provided permissions 
        for (Permission permission : permissions)
            addPermission(permission);

    }

    /**
     * Returns a map of connection IDs to the set of permissions granted for
     * that connection. If no permissions are granted to a particular
     * connection, its ID will not be present as a key in the map. This map is
     * mutable, and changes to this map will affect the permission set
     * directly.
     *
     * @return
     *     A map of connection IDs to the set of permissions granted for that
     *     connection.
     */
    public Map<String, EnumSet<ObjectPermission.Type>> getConnectionPermissions() {
        return connectionPermissions;
    }

    /**
     * Returns a map of connection group IDs to the set of permissions granted
     * for that connection group. If no permissions are granted to a particular
     * connection group, its ID will not be present as a key in the map. This
     * map is mutable, and changes to this map will affect the permission set
     * directly.
     *
     * @return
     *     A map of connection group IDs to the set of permissions granted for
     *     that connection group.
     */
    public Map<String, EnumSet<ObjectPermission.Type>> getConnectionGroupPermissions() {
        return connectionGroupPermissions;
    }

    /**
     * Returns a map of user IDs to the set of permissions granted for that
     * user. If no permissions are granted to a particular user, its ID will
     * not be present as a key in the map. This map is mutable, and changes to
     * to this map will affect the permission set directly.
     *
     * @return
     *     A map of user IDs to the set of permissions granted for that user.
     */
    public Map<String, EnumSet<ObjectPermission.Type>> getUserPermissions() {
        return userPermissions;
    }

    /**
     * Returns the set of granted system-level permissions. If no permissions
     * are granted at the system level, this will be an empty set. This set is
     * mutable, and changes to this set will affect the permission set
     * directly.
     *
     * @return
     *     The set of granted system-level permissions.
     */
    public EnumSet<SystemPermission.Type> getSystemPermissions() {
        return systemPermissions;
    }

    /**
     * Replaces the current map of connection permissions with the given map,
     * which must map connection ID to its corresponding set of granted
     * permissions. If a connection has no permissions, its ID must not be
     * present as a key in the map.
     *
     * @param connectionPermissions
     *     The map which must replace the currently-stored map of permissions.
     */
    public void setConnectionPermissions(Map<String, EnumSet<ObjectPermission.Type>> connectionPermissions) {
        this.connectionPermissions = connectionPermissions;
    }

    /**
     * Replaces the current map of connection group permissions with the given
     * map, which must map connection group ID to its corresponding set of
     * granted permissions. If a connection group has no permissions, its ID
     * must not be present as a key in the map.
     *
     * @param connectionGroupPermissions
     *     The map which must replace the currently-stored map of permissions.
     */
    public void setConnectionGroupPermissions(Map<String, EnumSet<ObjectPermission.Type>> connectionGroupPermissions) {
        this.connectionGroupPermissions = connectionGroupPermissions;
    }

    /**
     * Replaces the current map of user permissions with the given map, which
     * must map user ID to its corresponding set of granted permissions. If a
     * user has no permissions, its ID must not be present as a key in the map.
     *
     * @param userPermissions
     *     The map which must replace the currently-stored map of permissions.
     */
    public void setUserPermissions(Map<String, EnumSet<ObjectPermission.Type>> userPermissions) {
        this.userPermissions = userPermissions;
    }

    /**
     * Replaces the current set of system-level permissions with the given set.
     * If no system-level permissions are granted, the empty set must be
     * specified.
     *
     * @param systemPermissions
     *     The set which must replace the currently-stored set of permissions.
     */
    public void setSystemPermissions(EnumSet<SystemPermission.Type> systemPermissions) {
        this.systemPermissions = systemPermissions;
    }
    
}
