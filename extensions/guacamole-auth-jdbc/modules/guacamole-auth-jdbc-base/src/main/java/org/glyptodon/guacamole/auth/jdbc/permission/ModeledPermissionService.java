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

package org.glyptodon.guacamole.auth.jdbc.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.auth.permission.PermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting permissions within a backend database model, and for obtaining the
 * permission sets that contain these permissions. This service will
 * automatically enforce the permissions of the current user.
 *
 * @author Michael Jumper
 * @param <PermissionSetType>
 *     The type of permission sets this service provides access to.
 *
 * @param <PermissionType>
 *     The type of permission this service provides access to.
 *
 * @param <ModelType>
 *     The underlying model object used to represent PermissionType in the
 *     database.
 */
public abstract class ModeledPermissionService<PermissionSetType extends PermissionSet<PermissionType>,
        PermissionType extends Permission, ModelType>
    implements PermissionService<PermissionSetType, PermissionType> {

    /**
     * Returns an instance of a mapper for the type of permission used by this
     * service.
     *
     * @return
     *     A mapper which provides access to the model objects associated with
     *     the permissions used by this service.
     */
    protected abstract PermissionMapper<ModelType> getPermissionMapper();

    /**
     * Returns an instance of a permission which is based on the given model
     * object.
     *
     * @param model
     *     The model object to use to produce the returned permission.
     *
     * @return
     *     A permission which is based on the given model object.
     */
    protected abstract PermissionType getPermissionInstance(ModelType model);

    /**
     * Returns a collection of permissions which are based on the models in
     * the given collection.
     *
     * @param models
     *     The model objects to use to produce the permissions within the
     *     returned set.
     *
     * @return
     *     A set of permissions which are based on the models in the given
     *     collection.
     */
    protected Set<PermissionType> getPermissionInstances(Collection<ModelType> models) {

        // Create new collection of permissions by manually converting each model
        Set<PermissionType> permissions = new HashSet<PermissionType>(models.size());
        for (ModelType model : models)
            permissions.add(getPermissionInstance(model));

        return permissions;
        
    }

    /**
     * Returns an instance of a model object which is based on the given
     * permission and target user.
     *
     * @param targetUser
     *     The user to whom this permission is granted.
     *
     * @param permission
     *     The permission to use to produce the returned model object.
     *
     * @return
     *     A model object which is based on the given permission and target
     *     user.
     */
    protected abstract ModelType getModelInstance(ModeledUser targetUser,
            PermissionType permission);
    
    /**
     * Returns a collection of model objects which are based on the given
     * permissions and target user.
     *
     * @param targetUser
     *     The user to whom this permission is granted.
     *
     * @param permissions
     *     The permissions to use to produce the returned model objects.
     *
     * @return
     *     A collection of model objects which are based on the given
     *     permissions and target user.
     */
    protected Collection<ModelType> getModelInstances(ModeledUser targetUser,
            Collection<PermissionType> permissions) {

        // Create new collection of models by manually converting each permission 
        Collection<ModelType> models = new ArrayList<ModelType>(permissions.size());
        for (PermissionType permission : permissions)
            models.add(getModelInstance(targetUser, permission));

        return models;

    }

    /**
     * Determines whether the given user can read the permissions currently
     * granted to the given target user. If the reading user and the target
     * user are not the same, then explicit READ or SYSTEM_ADMINISTER access is
     * required.
     *
     * @param user
     *     The user attempting to read permissions.
     *
     * @param targetUser
     *     The user whose permissions are being read.
     *
     * @return
     *     true if permission is granted, false otherwise.
     *
     * @throws GuacamoleException 
     *     If an error occurs while checking permission status, or if
     *     permission is denied to read the current user's permissions.
     */
    protected boolean canReadPermissions(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // A user can always read their own permissions
        if (user.getUser().getIdentifier().equals(targetUser.getIdentifier()))
            return true;
        
        // A system adminstrator can do anything
        if (user.getUser().isAdministrator())
            return true;

        // Can read permissions on target user if explicit READ is granted
        ObjectPermissionSet userPermissionSet = user.getUser().getUserPermissions();
        return userPermissionSet.hasPermission(ObjectPermission.Type.READ, targetUser.getIdentifier());

    }

    @Override
    public Set<PermissionType> retrievePermissions(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException {

        // Retrieve permissions only if allowed
        if (canReadPermissions(user, targetUser))
            return getPermissionInstances(getPermissionMapper().select(targetUser.getModel()));

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

}
