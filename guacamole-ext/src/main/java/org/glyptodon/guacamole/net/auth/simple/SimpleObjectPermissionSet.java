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

package org.glyptodon.guacamole.net.auth.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * A read-only implementation of ObjectPermissionSet which uses a backing Set
 * of Permissions to determine which permissions are present.
 *
 * @author Michael Jumper
 * @param <IdentifierType>
 *     The type of identifier used to identify objects affected by permissions
 *     stored in this SimpleObjectPermissionSet.
 */
public class SimpleObjectPermissionSet<IdentifierType>
    implements ObjectPermissionSet<IdentifierType> {

    /**
     * The set of all permissions currently granted.
     */
    private Set<ObjectPermission<IdentifierType>> permissions = Collections.EMPTY_SET;

    /**
     * Creates a new empty SimpleObjectPermissionSet.
     */
    public SimpleObjectPermissionSet() {
    }

    /**
     * Creates a new SimpleObjectPermissionSet which contains the permissions
     * within the given Set.
     *
     * @param permissions 
     *     The Set of permissions this SimpleObjectPermissionSet should
     *     contain.
     */
    public SimpleObjectPermissionSet(Set<ObjectPermission<IdentifierType>> permissions) {
        this.permissions = permissions;
    }

    /**
     * Sets the Set which backs this SimpleObjectPermissionSet. Future function
     * calls on this SimpleObjectPermissionSet will use the provided Set.
     *
     * @param permissions 
     *     The Set of permissions this SimpleObjectPermissionSet should
     *     contain.
     */
    protected void setPermissions(Set<ObjectPermission<IdentifierType>> permissions) {
        this.permissions = permissions;
    }

    /**
     * Returns the Set which currently backs this SimpleObjectPermissionSet.
     * Changes to this Set will affect future function calls on this
     * SimpleObjectPermissionSet.
     *
     * @return
     *     The Set of permissions this SimpleObjectPermissionSet currently 
     *     contains.
     */
    protected Set<ObjectPermission<IdentifierType>> getPermissions() {
        return permissions;
    }

   
    @Override
    public boolean hasPermission(ObjectPermission.Type permission,
            IdentifierType identifier) throws GuacamoleException {

        ObjectPermission<IdentifierType> objectPermission =
                new ObjectPermission<IdentifierType>(permission, identifier);
        
        return permissions.contains(objectPermission);

    }

    @Override
    public void addPermission(ObjectPermission.Type permission,
            IdentifierType identifier) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void removePermission(ObjectPermission.Type permission,
            IdentifierType identifier) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public Collection<IdentifierType> getAccessibleObjects(
            Collection<ObjectPermission.Type> permissionTypes,
            Collection<IdentifierType> identifiers) throws GuacamoleException {

        Collection<IdentifierType> accessibleObjects = new ArrayList<IdentifierType>(permissions.size());

        // For each identifier/permission combination
        for (IdentifierType identifier : identifiers) {
            for (ObjectPermission.Type permissionType : permissionTypes) {

                // Add identifier if at least one requested permission is granted
                ObjectPermission<IdentifierType> permission = new ObjectPermission<IdentifierType>(permissionType, identifier);
                if (permissions.contains(permission)) {
                    accessibleObjects.add(identifier);
                    break;
                }

            }
        }

        return accessibleObjects;
        
    }

}
