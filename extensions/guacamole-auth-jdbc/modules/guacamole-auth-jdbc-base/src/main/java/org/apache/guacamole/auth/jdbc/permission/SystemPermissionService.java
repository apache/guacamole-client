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

package org.apache.guacamole.auth.jdbc.permission;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Collection;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.net.auth.permission.SystemPermission;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting system permissions. This service will automatically enforce
 * the permissions of the current user.
 */
public class SystemPermissionService
    extends ModeledPermissionService<SystemPermissionSet, SystemPermission, SystemPermissionModel> {

    /**
     * Mapper for system-level permissions.
     */
    @Inject
    private SystemPermissionMapper systemPermissionMapper;

    /**
     * Provider for creating system permission sets.
     */
    @Inject
    private Provider<SystemPermissionSet> systemPermissionSetProvider;

    @Override
    protected SystemPermissionMapper getPermissionMapper() {
        return systemPermissionMapper;
    }
    
    @Override
    protected SystemPermission getPermissionInstance(SystemPermissionModel model) {
        return new SystemPermission(model.getType());
    }

    @Override
    protected SystemPermissionModel getModelInstance(final ModeledUser targetUser,
            final SystemPermission permission) {

        SystemPermissionModel model = new SystemPermissionModel();

        // Populate model object with data from user and permission
        model.setEntityID(targetUser.getModel().getEntityID());
        model.setType(permission.getType());

        return model;
        
    }

    @Override
    public SystemPermissionSet getPermissionSet(ModeledAuthenticatedUser user,
            ModeledUser targetUser, boolean inherit) throws GuacamoleException {

        // Create permission set for requested user
        SystemPermissionSet permissionSet = systemPermissionSetProvider.get();
        permissionSet.init(user, targetUser, inherit);

        return permissionSet;
        
    }
    
    @Override
    public void createPermissions(ModeledAuthenticatedUser user, ModeledUser targetUser,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // Only an admin can create system permissions
        if (user.getUser().isAdministrator()) {
            Collection<SystemPermissionModel> models = getModelInstances(targetUser, permissions);
            systemPermissionMapper.insert(models);
            return;
        }

        // User lacks permission to create system permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    @Override
    public void deletePermissions(ModeledAuthenticatedUser user, ModeledUser targetUser,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // Only an admin can delete system permissions
        if (user.getUser().isAdministrator()) {

            // Do not allow users to remove their own admin powers
            if (user.getUser().getIdentifier().equals(targetUser.getIdentifier()))
                throw new GuacamoleUnsupportedException("Removing your own administrative permissions is not allowed.");
            
            Collection<SystemPermissionModel> models = getModelInstances(targetUser, permissions);
            systemPermissionMapper.delete(models);
            return;
        }

        // User lacks permission to delete system permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    /**
     * Retrieves whether the permission of the given type has been granted to
     * the given user. Permission inheritance through group membership is taken
     * into account.
     *
     * @param user
     *     The user retrieving the permission.
     *
     * @param targetUser
     *     The user associated with the permission to be retrieved.
     * 
     * @param type
     *     The type of permission to retrieve.
     *
     * @param inherit
     *     Whether permissions inherited through user groups should be taken
     *     into account. If false, only permissions granted directly will be
     *     included.
     *
     * @return
     *     true if permission of the given type has been granted to the given
     *     user, false otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested permission.
     */
    public boolean hasPermission(ModeledAuthenticatedUser user,
            ModeledUser targetUser, SystemPermission.Type type,
            boolean inherit) throws GuacamoleException {

        // Retrieve permissions only if allowed
        if (canReadPermissions(user, targetUser))
            return getPermissionMapper().selectOne(targetUser.getModel(), type, inherit) != null;

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

}
