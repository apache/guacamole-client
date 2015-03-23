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
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.user.ModeledUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting object permissions. This service will automatically enforce the
 * permissions of the current user.
 *
 * @author Michael Jumper
 */
public interface ObjectPermissionService
    extends PermissionService<ObjectPermissionSet, ObjectPermission> {

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
    ObjectPermission retrievePermission(AuthenticatedUser user,
            ModeledUser targetUser, ObjectPermission.Type type,
            String identifier) throws GuacamoleException;

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
    Collection<String> retrieveAccessibleIdentifiers(AuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission.Type> permissions,
            Collection<String> identifiers) throws GuacamoleException;

}
