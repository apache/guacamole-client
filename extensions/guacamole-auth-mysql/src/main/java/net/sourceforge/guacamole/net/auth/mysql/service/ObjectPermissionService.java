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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
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
 * @param <ModelType>
 *     The underlying model object used to represent PermissionType in the
 *     database.
 */
public abstract class ObjectPermissionService<ModelType>
    extends PermissionService<ObjectPermissionSet, ObjectPermission, ModelType> {

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
    protected boolean canAlterPermissions(AuthenticatedUser user, MySQLUser targetUser,
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
    public void createPermissions(AuthenticatedUser user, MySQLUser targetUser,
            Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Create permissions only if user has permission to do so
        if (canAlterPermissions(user, targetUser, permissions)) {
            Collection<ModelType> models = getModelInstances(targetUser, permissions);
            getPermissionMapper().insert(models);
            return;
        }
        
        // User lacks permission to create object permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public void deletePermissions(AuthenticatedUser user, MySQLUser targetUser,
            Collection<ObjectPermission> permissions)
            throws GuacamoleException {

        // Delete permissions only if user has permission to do so
        if (canAlterPermissions(user, targetUser, permissions)) {
            Collection<ModelType> models = getModelInstances(targetUser, permissions);
            getPermissionMapper().delete(models);
            return;
        }
        
        // User lacks permission to delete object permissions
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
