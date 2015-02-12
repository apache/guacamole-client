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

package org.glyptodon.guacamole.net.basic.rest.user;

import java.util.HashSet;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.auth.permission.PermissionSet;

/**
 * A set of changes to be applied to a PermissionSet, describing the set of
 * permissions being added and removed.
 * 
 * @author Michael Jumper
 * @param <PermissionType>
 *     The type of permissions being added and removed.
 */
public class PermissionSetPatch<PermissionType extends Permission> {

    /**
     * The set of all permissions being added.
     */
    private final Set<PermissionType> addedPermissions =
            new HashSet<PermissionType>();
    
    /**
     * The set of all permissions being removed.
     */
    private final Set<PermissionType> removedPermissions =
            new HashSet<PermissionType>();

    /**
     * Queues the given permission to be added. The add operation will be
     * performed only when apply() is called.
     *
     * @param permission
     *     The permission to add.
     */
    public void addPermission(PermissionType permission) {
        addedPermissions.add(permission);
    }
    
    /**
     * Queues the given permission to be removed. The remove operation will be
     * performed only when apply() is called.
     *
     * @param permission
     *     The permission to remove.
     */
    public void removePermission(PermissionType permission) {
        removedPermissions.add(permission);
    }

    /**
     * Applies all queued changes to the given permission set.
     *
     * @param permissionSet
     *     The permission set to add and remove permissions from.
     *
     * @throws GuacamoleException
     *     If an error occurs while manipulating the permissions of the given
     *     permission set.
     */
    public void apply(PermissionSet<PermissionType> permissionSet)
        throws GuacamoleException {

        // Add any added permissions
        if (!addedPermissions.isEmpty())
            permissionSet.addPermissions(addedPermissions);

        // Remove any removed permissions
        if (!removedPermissions.isEmpty())
            permissionSet.removePermissions(removedPermissions);

    }
    
}
