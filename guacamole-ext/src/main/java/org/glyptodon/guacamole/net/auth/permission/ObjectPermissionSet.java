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

package org.glyptodon.guacamole.net.auth.permission;

import java.util.Collection;
import org.glyptodon.guacamole.GuacamoleException;


/**
 * A set of permissions which affect arbitrary objects, where each object has
 * an associated unique identifier.
 *
 * @author Michael Jumper
 * @param <IdentifierType>
 *     The type of identifier used to identify objects affected by permissions
 *     stored in this ObjectPermissionSet.
 */
public interface ObjectPermissionSet<IdentifierType> {

    /**
     * Tests whether the permission of the given type is granted for the
     * object having the given identifier.
     *
     * @param permission
     *     The permission to check.
     *
     * @param identifier
     *     The identifier of the object affected by the permission being
     *     checked.
     *
     * @return
     *     true if the permission is granted, false otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while checking permissions, or if permissions
     *     cannot be checked due to lack of permissions to do so.
     */
    boolean hasPermission(ObjectPermission.Type permission,
            IdentifierType identifier) throws GuacamoleException;

    /**
     * Adds the specified permission for the object having the given
     * identifier.
     *
     * @param permission
     *     The permission to add.
     *
     * @param identifier
     *     The identifier of the object affected by the permission being
     *     added.
     *
     * @throws GuacamoleException
     *     If an error occurs while adding the permission, or if permission to
     *     add permissions is denied.
     */
    void addPermission(ObjectPermission.Type permission,
            IdentifierType identifier) throws GuacamoleException;

    /**
     * Removes the specified permission for the object having the given
     * identifier.
     *
     * @param permission
     *     The permission to remove.
     *
     * @param identifier
     *     The identifier of the object affected by the permission being
     *     added.
     *
     * @throws GuacamoleException
     *     If an error occurs while removing the permission, or if permission
     *     to remove permissions is denied.
     */
    void removePermission(ObjectPermission.Type permission,
            IdentifierType identifier) throws GuacamoleException;

    /**
     * Tests whether this user has the specified permissions for the objects
     * having the given identifiers. The identifier of an object is returned
     * in a new collection if at least one of the specified permissions is
     * granted for that object.
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
     *     If an error occurs while checking permissions, or if permissions
     *     cannot be checked due to lack of permissions to do so.
     */
    Collection<IdentifierType> getAccessibleObjects(
            Collection<ObjectPermission.Type> permissions,
            Collection<IdentifierType> identifiers) throws GuacamoleException;

}
