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

import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.auth.permission.PermissionSet;

/**
 * Abstract PermissionService implementation which provides additional
 * convenience methods for enforcing the permission model.
 *
 * @author Michael Jumper
 * @param <PermissionSetType>
 *     The type of permission sets this service provides access to.
 *
 * @param <PermissionType>
 *     The type of permission this service provides access to.
 */
public abstract class AbstractPermissionService<PermissionSetType extends PermissionSet<PermissionType>,
        PermissionType extends Permission>
    implements PermissionService<PermissionSetType, PermissionType> {

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

}
