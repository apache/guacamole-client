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

package net.sourceforge.guacamole.net.auth.mysql.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.PermissionMapper;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.Permission;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting permissions. This service will automatically enforce the
 * permissions of the current user.
 *
 * @author Michael Jumper
 * @param <PermissionType>
 *     The type of permission this service provides access to.
 *
 * @param <ModelType>
 *     The underlying model object used to represent PermissionType in the
 *     database.
 */
public abstract class PermissionService<PermissionType extends Permission, ModelType> {

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
    protected abstract ModelType getModelInstance(MySQLUser targetUser,
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
    protected Collection<ModelType> getModelInstances(MySQLUser targetUser,
            Collection<PermissionType> permissions) {

        // Create new collection of models by manually converting each permission 
        Collection<ModelType> models = new ArrayList<ModelType>(permissions.size());
        for (PermissionType permission : permissions)
            models.add(getModelInstance(targetUser, permission));

        return models;

    }
    
    /**
     * Retrieves all permissions associated with the given user.
     *
     * @param user
     *     The user retrieving the permissions.
     *
     * @param targetUser
     *     The user associated with the permissions to be retrieved.
     *
     * @return
     *     The permissions associated with the given user.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested permissions.
     */
    public Set<PermissionType> retrievePermissions(AuthenticatedUser user,
            MySQLUser targetUser) throws GuacamoleException {

        // Only an admin can read permissions that aren't his own
        if (user.getUser().getIdentifier().equals(targetUser.getIdentifier())
                || user.getUser().isAdministrator())
            return getPermissionInstances(getPermissionMapper().select(targetUser.getModel()));

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    /**
     * Creates the given permissions within the database. If any permissions
     * already exist, they will be ignored.
     *
     * @param user
     *     The user creating the permissions.
     *
     * @param targetUser
     *     The user associated with the permissions to be created.
     *
     * @param permissions 
     *     The permissions to create.
     *
     * @throws GuacamoleException
     *     If the user lacks permission to create the permissions, or an error
     *     occurs while creating the permissions.
     */
    public abstract void createPermissions(AuthenticatedUser user,
            MySQLUser targetUser,
            Collection<PermissionType> permissions) throws GuacamoleException;

    /**
     * Deletes the given permissions. If any permissions do not exist, they
     * will be ignored.
     *
     * @param user
     *     The user deleting the permissions.
     *
     * @param targetUser
     *     The user associated with the permissions to be deleted.
     *
     * @param permissions
     *     The permissions to delete.
     *
     * @throws GuacamoleException
     *     If the user lacks permission to delete the permissions, or an error
     *     occurs while deleting the permissions.
     */
    public abstract void deletePermissions(AuthenticatedUser user,
            MySQLUser targetUser,
            Collection<PermissionType> permissions) throws GuacamoleException;

}
