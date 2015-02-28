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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting object permissions. This service will automatically enforce the
 * permissions of the current user.
 *
 * @author Michael Jumper
 */
public abstract class ObjectPermissionService
    extends PermissionService<ObjectPermissionSet, ObjectPermission, ObjectPermissionModel> {

    @Override
    protected abstract ObjectPermissionMapper getPermissionMapper();

    /**
     * Returns the permission set associated with the given user and related
     * to the type of objects affected the permissions handled by this
     * permission service.
     *
     * @param user
     *     The user whose permissions are being retrieved.
     *
     * @return
     *     A permission set which contains the permissions associated with the
     *     given user and related to the type of objects handled by this
     *     directory object service.
     * 
     * @throws GuacamoleException
     *     If permission to read the user's permissions is denied.
     */
    protected abstract ObjectPermissionSet getAffectedPermissionSet(AuthenticatedUser user)
            throws GuacamoleException;

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
    protected boolean canAlterPermissions(AuthenticatedUser user, ModeledUser targetUser,
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
        Collection<String> affectedIdentifiers = new HashSet(permissions.size());
        for (ObjectPermission permission : permissions)
            affectedIdentifiers.add(permission.getObjectIdentifier());

        // Determine subset of affected identifiers that we have admin access to
        ObjectPermissionSet affectedPermissionSet = getAffectedPermissionSet(user);
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
    public void createPermissions(AuthenticatedUser user, ModeledUser targetUser,
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
    public void deletePermissions(AuthenticatedUser user, ModeledUser targetUser,
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

    /**
     * Retrieves the permission of the given type associated with the given
     * user and object, if it exists. If no such permission exists, null is
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
     * @param identifier
     *     The identifier of the object affected by the permission to return.
     *
     * @return
     *     The permission of the given type associated with the given user and
     *     object, or null if no such permission exists.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the requested permission.
     */
    public ObjectPermission retrievePermission(AuthenticatedUser user,
            ModeledUser targetUser, ObjectPermission.Type type,
            String identifier) throws GuacamoleException {

        // Only an admin can read permissions that aren't his own
        if (user.getUser().getIdentifier().equals(targetUser.getIdentifier())
                || user.getUser().isAdministrator()) {

            // Read permission from database, return null if not found
            ObjectPermissionModel model = getPermissionMapper().selectOne(targetUser.getModel(), type, identifier);
            if (model == null)
                return null;

            return getPermissionInstance(model);

        }

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");
        
    }

    /**
     * Retrieves the subset of the given identifiers for which the given user
     * has at least one of the given permissions.
     *
     * @param user
     *     The user checking the permissions.
     *
     * @param targetUser
     *     The user to check permissions of.
     *
     * @param permissions
     *     The permissions to check. An identifier will be included in the
     *     resulting collection if at least one of these permissions is granted
     *     for the associated object
     *
     * @param identifiers
     *     The identifiers of the objects affected by the permissions being
     *     checked.
     *
     * @return
     *     A collection containing the subset of identifiers for which at least
     *     one of the specified permissions is granted.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving permissions.
     */
    public Collection<String> retrieveAccessibleIdentifiers(AuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission.Type> permissions,
            Collection<String> identifiers) throws GuacamoleException {

        // Determine whether the user is an admin
        boolean isAdmin = user.getUser().isAdministrator();
        
        // Only an admin can read permissions that aren't his own
        if (isAdmin || user.getUser().getIdentifier().equals(targetUser.getIdentifier())) {

            // If user is an admin, everything is accessible
            if (isAdmin)
                return identifiers;

            // Otherwise, return explicitly-retrievable identifiers
            return getPermissionMapper().selectAccessibleIdentifiers(targetUser.getModel(), permissions, identifiers);
            
        }

        // User cannot read this user's permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
