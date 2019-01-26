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

package org.apache.guacamole.auth.common.permission;

import java.util.Collection;
import java.util.Set;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.auth.common.base.EntityModelInterface;
import org.apache.guacamole.auth.common.base.ModeledPermissions;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.auth.permission.SystemPermission;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting system permissions. This service will automatically enforce the
 * permissions of the current user.
 */
public abstract class SystemPermissionServiceAbstract extends
        ModeledPermissionServiceAbstract<SystemPermissionSet, SystemPermission, SystemPermissionModelInterface> {

    /**
     * Mapper for system-level permissions.
     */
    @Inject
    private SystemPermissionMapperInterface systemPermissionMapper;

    /**
     * Provider for creating system permission sets.
     */
    @Inject
    private Provider<SystemPermissionSet> systemPermissionSetProvider;

    @Override
    protected SystemPermissionMapperInterface getPermissionMapper() {
        return systemPermissionMapper;
    }

    @Override
    protected SystemPermission getPermissionInstance(
            SystemPermissionModelInterface model) {
        return new SystemPermission(model.getType());
    }

    @Override
    public SystemPermissionSet getPermissionSet(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Set<String> effectiveGroups) throws GuacamoleException {

        // Create permission set for requested user
        SystemPermissionSet permissionSet = systemPermissionSetProvider.get();
        permissionSet.init(user, targetEntity, effectiveGroups);

        return permissionSet;

    }

	@Override
    public void createPermissions(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // Only an admin can create system permissions
        if (user.getUser().isAdministrator()) {
            Collection<SystemPermissionModelInterface> models = getModelInstances(targetEntity, permissions);
            systemPermissionMapper.insert(models);
            return;
        }

        // User lacks permission to create system permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

	@Override
    public void deletePermissions(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // Only an admin can delete system permissions
        if (user.getUser().isAdministrator()) {

            // Do not allow users to remove their own admin powers
            if (user.getUser().getIdentifier().equals(targetEntity.getIdentifier()))
                throw new GuacamoleUnsupportedException("Removing your own administrative permissions is not allowed.");

            Collection<SystemPermissionModelInterface> models = getModelInstances( targetEntity, permissions);
            systemPermissionMapper.delete(models);
            return;
        }

        // User lacks permission to delete system permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

    /**
     * Retrieves whether the permission of the given type has been granted to
     * the given entity. Permission inheritance through group membership is
     * taken into account.
     *
     * @param user
     *     The user retrieving the permission.
     *
     * @param targetEntity
     *     The entity associated with the permission to be retrieved.
     * 
     * @param type
     *     The type of permission to retrieve.
     *
     * @param effectiveGroups
     *     The identifiers of all groups that should be taken into account
     *     when determining the permissions effectively granted to the entity.
     *     If no groups are given, only permissions directly granted to the
     *     entity will be used.
     *
     * @return
     *     true if permission of the given type has been granted to the given
     *     entity, false otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested permission.
     */
    public boolean hasPermission(ModeledAuthenticatedUser user,
            ModeledPermissions<? extends EntityModelInterface> targetEntity,
            SystemPermission.Type type, Set<String> effectiveGroups)
            throws GuacamoleException {

        // Retrieve permissions only if allowed
        if (canReadPermissions(user, targetEntity))
            return getPermissionMapper().selectOne(targetEntity.getModel(), type, effectiveGroups) != null;

        // User cannot read this entity's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

}
