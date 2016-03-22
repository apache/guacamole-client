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

package org.apache.guacamole.auth.jdbc.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.Permission;
import org.apache.guacamole.net.auth.permission.PermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting permissions, and for obtaining the permission sets that contain
 * these permissions. This service will automatically enforce the permissions
 * of the current user.
 *
 * @author Michael Jumper
 * @param <PermissionSetType>
 *     The type of permission sets this service provides access to.
 *
 * @param <PermissionType>
 *     The type of permission this service provides access to.
 */
public interface PermissionService<PermissionSetType extends PermissionSet<PermissionType>,
        PermissionType extends Permission> {

    /**
     * Returns a permission set that can be used to retrieve and manipulate the
     * permissions of the given user.
     *
     * @param user
     *     The user who will be retrieving or manipulating permissions through
     *     the returned permission set.
     *
     * @param targetUser
     *     The user to whom the permissions in the returned permission set are
     *     granted.
     *
     * @return
     *     A permission set that contains all permissions associated with the
     *     given user, and can be used to manipulate that user's permissions.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the permissions of the given
     *     user, or if permission to retrieve the permissions of the given
     *     user is denied.
     */
    PermissionSetType getPermissionSet(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException;

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
    Set<PermissionType> retrievePermissions(AuthenticatedUser user,
            ModeledUser targetUser) throws GuacamoleException;

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
    void createPermissions(AuthenticatedUser user, ModeledUser targetUser,
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
    void deletePermissions(AuthenticatedUser user, ModeledUser targetUser,
            Collection<PermissionType> permissions) throws GuacamoleException;

}
