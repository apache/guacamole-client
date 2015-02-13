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
import java.util.Map;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

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
    private Map<String, Set<ObjectPermission.Type>> connectionPermissions =
            new HashMap<String, Set<ObjectPermission.Type>>();

    /**
     * Map of connection group ID to the set of granted permissions.
     */
    private Map<String, Set<ObjectPermission.Type>> connectionGroupPermissions =
            new HashMap<String, Set<ObjectPermission.Type>>();

    /**
     * Map of user ID to the set of granted permissions.
     */
    private Map<String, Set<ObjectPermission.Type>> userPermissions =
            new HashMap<String, Set<ObjectPermission.Type>>();

    /**
     * Set of all granted system-level permissions.
     */
    private Set<SystemPermission.Type> systemPermissions =
            EnumSet.noneOf(SystemPermission.Type.class);

    /**
     * Creates a new permission set which contains no granted permissions. Any
     * permissions must be added by manipulating or replacing the applicable
     * permission collection.
     */
    public APIPermissionSet() {
    }

    /**
     * Adds the system permissions from the given SystemPermissionSet to the
     * Set of system permissions provided.
     *
     * @param permissions
     *     The Set to add system permissions to.
     *
     * @param permSet
     *     The SystemPermissionSet containing the system permissions to add.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving system permissions from the
     *     SystemPermissionSet.
     */
    private void addSystemPermissions(Set<SystemPermission.Type> permissions,
            SystemPermissionSet permSet) throws GuacamoleException {

        // Add all provided system permissions 
        for (SystemPermission permission : permSet.getPermissions())
            permissions.add(permission.getType());

    }
    
    /**
     * Adds the object permissions from the given ObjectPermissionSet to the
     * Map of object permissions provided.
     *
     * @param permissions
     *     The Map to add object permissions to.
     *
     * @param permSet
     *     The ObjectPermissionSet containing the object permissions to add.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving object permissions from the
     *     ObjectPermissionSet.
     */
    private void addObjectPermissions(Map<String, Set<ObjectPermission.Type>> permissions,
            ObjectPermissionSet permSet) throws GuacamoleException {

        // Add all provided object permissions 
        for (ObjectPermission permission : permSet.getPermissions()) {

            // Get associated set of permissions
            String identifier = permission.getObjectIdentifier();
            Set<ObjectPermission.Type> objectPermissions = permissions.get(identifier);

            // Create new set if none yet exists
            if (objectPermissions == null)
                permissions.put(identifier, EnumSet.of(permission.getType()));

            // Otherwise add to existing set
            else
                objectPermissions.add(permission.getType());

        }

    }
    
    /**
     * Creates a new permission set containing all permissions currently
     * granted to the given user.
     *
     * @param user
     *     The user whose permissions should be stored within this permission
     *     set.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the user's permissions.
     */
    public APIPermissionSet(User user) throws GuacamoleException {

        // Add all permissions from the provided user
        addSystemPermissions(systemPermissions,          user.getSystemPermissions());
        addObjectPermissions(connectionPermissions,      user.getConnectionPermissions());
        addObjectPermissions(connectionGroupPermissions, user.getConnectionGroupPermissions());
        addObjectPermissions(userPermissions,            user.getUserPermissions());
        
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
    public Map<String, Set<ObjectPermission.Type>> getConnectionPermissions() {
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
    public Map<String, Set<ObjectPermission.Type>> getConnectionGroupPermissions() {
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
    public Map<String, Set<ObjectPermission.Type>> getUserPermissions() {
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
    public Set<SystemPermission.Type> getSystemPermissions() {
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
    public void setConnectionPermissions(Map<String, Set<ObjectPermission.Type>> connectionPermissions) {
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
    public void setConnectionGroupPermissions(Map<String, Set<ObjectPermission.Type>> connectionGroupPermissions) {
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
    public void setUserPermissions(Map<String, Set<ObjectPermission.Type>> userPermissions) {
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
    public void setSystemPermissions(Set<SystemPermission.Type> systemPermissions) {
        this.systemPermissions = systemPermissions;
    }
    
}
