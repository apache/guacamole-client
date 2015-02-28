/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.auth.jdbc.permission;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Collection;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting system permissions. This service will automatically enforce
 * the permissions of the current user.
 *
 * @author Michael Jumper
 */
public class SystemPermissionService
    extends PermissionService<SystemPermissionSet, SystemPermission, SystemPermissionModel> {

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
        model.setUserID(targetUser.getModel().getObjectID());
        model.setUsername(targetUser.getModel().getIdentifier());
        model.setType(permission.getType());

        return model;
        
    }

    @Override
    public SystemPermissionSet getPermissionSet(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Create permission set for requested user
        SystemPermissionSet permissionSet = systemPermissionSetProvider.get();
        permissionSet.init(user, targetUser);

        return permissionSet;
        
    }
    
    @Override
    public void createPermissions(AuthenticatedUser user, ModeledUser targetUser,
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
    public void deletePermissions(AuthenticatedUser user, ModeledUser targetUser,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // Only an admin can delete system permissions
        if (user.getUser().isAdministrator()) {
            Collection<SystemPermissionModel> models = getModelInstances(targetUser, permissions);
            systemPermissionMapper.delete(models);
            return;
        }

        // User lacks permission to delete system permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    /**
     * Retrieves the permission of the given type associated with the given
     * user, if it exists. If no such permission exists, null is returned.
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
     * @return
     *     The permission of the given type associated with the given user, or
     *     null if no such permission exists.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested permission.
     */
    public SystemPermission retrievePermission(AuthenticatedUser user,
            ModeledUser targetUser, SystemPermission.Type type) throws GuacamoleException {

        // Only an admin can read permissions that aren't his own
        if (user.getUser().getIdentifier().equals(targetUser.getIdentifier())
                || user.getUser().isAdministrator()) {

            // Read permission from database, return null if not found
            SystemPermissionModel model = getPermissionMapper().selectOne(targetUser.getModel(), type);
            if (model == null)
                return null;

            return getPermissionInstance(model);

        }

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

}
