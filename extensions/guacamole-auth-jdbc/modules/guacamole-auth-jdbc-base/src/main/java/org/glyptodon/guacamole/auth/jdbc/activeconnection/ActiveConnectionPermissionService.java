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

package org.glyptodon.guacamole.auth.jdbc.activeconnection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.auth.jdbc.permission.AbstractPermissionService;
import org.glyptodon.guacamole.auth.jdbc.permission.ObjectPermissionService;
import org.glyptodon.guacamole.auth.jdbc.tunnel.ActiveConnectionRecord;
import org.glyptodon.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating active connections.
 *
 * @author Michael Jumper
 */
public class ActiveConnectionPermissionService
    extends AbstractPermissionService<ObjectPermissionSet, ObjectPermission>
    implements ObjectPermissionService {

    /**
     * Service for creating and tracking tunnels.
     */
    @Inject
    private GuacamoleTunnelService tunnelService;

    /**
     * Provider for active connection permission sets.
     */
    @Inject
    private Provider<ActiveConnectionPermissionSet> activeConnectionPermissionSetProvider;

    @Override
    public ObjectPermission retrievePermission(AuthenticatedUser user,
            ModeledUser targetUser, ObjectPermission.Type type,
            String identifier) throws GuacamoleException {

        // Retrieve permissions
        Set<ObjectPermission> permissions = retrievePermissions(user, targetUser);

        // If retrieved permissions contains the requested permission, return it
        ObjectPermission permission = new ObjectPermission(type, identifier); 
        if (permissions.contains(permission))
            return permission;

        // Otherwise, no such permission
        return null;

    }

    @Override
    public Set<ObjectPermission> retrievePermissions(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Retrieve permissions only if allowed
        if (canReadPermissions(user, targetUser)) {

            // Only administrators may access active connections
            if (!targetUser.isAdministrator())
                return Collections.EMPTY_SET;

            // Get all active connections
            Collection<ActiveConnectionRecord> records = tunnelService.getActiveConnections(user);

            // We have READ and DELETE on all active connections
            Set<ObjectPermission> permissions = new HashSet<ObjectPermission>();
            for (ActiveConnectionRecord record : records) {

                // Add implicit READ and DELETE
                String identifier = record.getUUID().toString();
                permissions.add(new ObjectPermission(ObjectPermission.Type.READ,   identifier));
                permissions.add(new ObjectPermission(ObjectPermission.Type.DELETE, identifier));

            }

            return permissions;
            
        }

        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    @Override
    public Collection<String> retrieveAccessibleIdentifiers(AuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission.Type> permissionTypes,
            Collection<String> identifiers) throws GuacamoleException {

        Set<ObjectPermission> permissions = retrievePermissions(user, targetUser);
        Collection<String> accessibleObjects = new ArrayList<String>(permissions.size());

        // For each identifier/permission combination
        for (String identifier : identifiers) {
            for (ObjectPermission.Type permissionType : permissionTypes) {

                // Add identifier if at least one requested permission is granted
                ObjectPermission permission = new ObjectPermission(permissionType, identifier);
                if (permissions.contains(permission)) {
                    accessibleObjects.add(identifier);
                    break;
                }

            }
        }

        return accessibleObjects;

    }

    @Override
    public ObjectPermissionSet getPermissionSet(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {
    
        // Create permission set for requested user
        ActiveConnectionPermissionSet permissionSet = activeConnectionPermissionSetProvider.get();
        permissionSet.init(user, targetUser);

        return permissionSet;
 
    }

    @Override
    public void createPermissions(AuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Creating active connection permissions is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public void deletePermissions(AuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Deleting active connection permissions is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
