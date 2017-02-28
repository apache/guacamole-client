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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting object permissions within a backend database model. This service
 * will automatically enforce the permissions of the current user.
 */
public abstract class ModeledObjectPermissionService
    extends ModeledPermissionService<ObjectPermissionSet, ObjectPermission, ObjectPermissionModel>
    implements ObjectPermissionService {

    @Override
    protected abstract ObjectPermissionMapper getPermissionMapper();

    @Override
    protected ObjectPermission getPermissionInstance(ObjectPermissionModel model) {
        return new ObjectPermission(model.getType(), model.getObjectIdentifier());
    }

    @Override
    protected ObjectPermissionModel getModelInstance(ModeledUser targetUser,
            ObjectPermission permission) {

        ObjectPermissionModel model = new ObjectPermissionModel();

        // Populate model object with data from user and permission
        model.setUserID(targetUser.getModel().getObjectID());
        model.setUsername(targetUser.getModel().getIdentifier());
        model.setType(permission.getType());
        model.setObjectIdentifier(permission.getObjectIdentifier());

        return model;
        
    }

    /**
     * Determines whether the current user has permission to update the given
     * target user, adding or removing the given permissions. Such permission
     * depends on whether the current user is a system administrator, whether
     * they have explicit UPDATE permission on the target user, and whether
     * they have explicit ADMINISTER permission on all affected objects.
     *
     * @param user
     *     The user who is changing permissions.
     *
     * @param targetUser
     *     The user whose permissions are being changed.
     *
     * @param permissions
     *     The permissions that are being added or removed from the target
     *     user.
     *
     * @return
     *     true if the user has permission to change the target users
     *     permissions as specified, false otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while checking permission status, or if
     *     permission is denied to read the current user's permissions.
     */
    protected boolean canAlterPermissions(ModeledAuthenticatedUser user, ModeledUser targetUser,
            Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // A system adminstrator can do anything
        if (user.getUser().isAdministrator())
            return true;
        
        // Verify user has update permission on the target user
        ObjectPermissionSet userPermissionSet = user.getUser().getUserPermissions();
        if (!userPermissionSet.hasPermission(ObjectPermission.Type.UPDATE, targetUser.getIdentifier()))
            return false;

        // Produce collection of affected identifiers
        Collection<String> affectedIdentifiers = new HashSet<String>(permissions.size());
        for (ObjectPermission permission : permissions)
            affectedIdentifiers.add(permission.getObjectIdentifier());

        // Determine subset of affected identifiers that we have admin access to
        ObjectPermissionSet affectedPermissionSet = getPermissionSet(user, user.getUser());
        Collection<String> allowedSubset = affectedPermissionSet.getAccessibleObjects(
            Collections.singleton(ObjectPermission.Type.ADMINISTER),
            affectedIdentifiers
        );

        // The permissions can be altered if and only if the set of objects we
        // are allowed to administer is equal to the set of objects we will be
        // affecting.

        return affectedIdentifiers.size() == allowedSubset.size();
        
    }
    
    @Override
    public void createPermissions(ModeledAuthenticatedUser user, ModeledUser targetUser,
            Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Create permissions only if user has permission to do so
        if (canAlterPermissions(user, targetUser, permissions)) {
            Collection<ObjectPermissionModel> models = getModelInstances(targetUser, permissions);
            getPermissionMapper().insert(models);
            return;
        }
        
        // User lacks permission to create object permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public void deletePermissions(ModeledAuthenticatedUser user, ModeledUser targetUser,
            Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Delete permissions only if user has permission to do so
        if (canAlterPermissions(user, targetUser, permissions)) {
            Collection<ObjectPermissionModel> models = getModelInstances(targetUser, permissions);
            getPermissionMapper().delete(models);
            return;
        }
        
        // User lacks permission to delete object permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public ObjectPermission retrievePermission(ModeledAuthenticatedUser user,
            ModeledUser targetUser, ObjectPermission.Type type,
            String identifier) throws GuacamoleException {

        // Retrieve permissions only if allowed
        if (canReadPermissions(user, targetUser)) {

            // Read permission from database, return null if not found
            ObjectPermissionModel model = getPermissionMapper().selectOne(targetUser.getModel(), type, identifier);
            if (model == null)
                return null;

            return getPermissionInstance(model);

        }

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    @Override
    public Collection<String> retrieveAccessibleIdentifiers(ModeledAuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission.Type> permissions,
            Collection<String> identifiers) throws GuacamoleException {

        // Nothing is always accessible
        if (identifiers.isEmpty())
            return identifiers;
        
        // Retrieve permissions only if allowed
        if (canReadPermissions(user, targetUser)) {

            // If user is an admin, everything is accessible
            if (user.getUser().isAdministrator())
                return identifiers;

            // Otherwise, return explicitly-retrievable identifiers
            return getPermissionMapper().selectAccessibleIdentifiers(targetUser.getModel(), permissions, identifiers);
            
        }

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
