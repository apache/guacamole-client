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

package org.apache.guacamole.auth.jdbc.activeconnection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.jdbc.permission.AbstractPermissionService;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionService;
import org.apache.guacamole.auth.jdbc.tunnel.ActiveConnectionRecord;
import org.apache.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating active connections.
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
    public ObjectPermission retrievePermission(ModeledAuthenticatedUser user,
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
    public Set<ObjectPermission> retrievePermissions(ModeledAuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Retrieve permissions only if allowed
        if (canReadPermissions(user, targetUser)) {

            // Only administrators may access active connections
            boolean isAdmin = targetUser.isAdministrator();

            // Get all active connections
            Collection<ActiveConnectionRecord> records = tunnelService.getActiveConnections(user);

            // We have READ, and possibly DELETE, on all active connections
            Set<ObjectPermission> permissions = new HashSet<ObjectPermission>();
            for (ActiveConnectionRecord record : records) {

                // Add implicit READ
                String identifier = record.getUUID().toString();
                permissions.add(new ObjectPermission(ObjectPermission.Type.READ, identifier));

                // If we're and admin, then we also have DELETE
                if (isAdmin)
                    permissions.add(new ObjectPermission(ObjectPermission.Type.DELETE, identifier));

            }

            return permissions;
            
        }

        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    @Override
    public Collection<String> retrieveAccessibleIdentifiers(ModeledAuthenticatedUser user,
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
    public ObjectPermissionSet getPermissionSet(ModeledAuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {
    
        // Create permission set for requested user
        ActiveConnectionPermissionSet permissionSet = activeConnectionPermissionSetProvider.get();
        permissionSet.init(user, targetUser);

        return permissionSet;
 
    }

    @Override
    public void createPermissions(ModeledAuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Creating active connection permissions is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public void deletePermissions(ModeledAuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Deleting active connection permissions is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
