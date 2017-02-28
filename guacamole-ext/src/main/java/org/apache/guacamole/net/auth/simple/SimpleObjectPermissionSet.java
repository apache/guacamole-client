/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.net.auth.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * A read-only implementation of ObjectPermissionSet which uses a backing Set
 * of Permissions to determine which permissions are present.
 */
public class SimpleObjectPermissionSet implements ObjectPermissionSet {

    /**
     * The set of all permissions currently granted.
     */
    private Set<ObjectPermission> permissions = Collections.<ObjectPermission>emptySet();

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
    public SimpleObjectPermissionSet(Set<ObjectPermission> permissions) {
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
    protected void setPermissions(Set<ObjectPermission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public Set<ObjectPermission> getPermissions() {
        return permissions;
    }

    @Override
    public boolean hasPermission(ObjectPermission.Type permission,
            String identifier) throws GuacamoleException {

        ObjectPermission objectPermission =
                new ObjectPermission(permission, identifier);
        
        return permissions.contains(objectPermission);

    }

    @Override
    public void addPermission(ObjectPermission.Type permission,
            String identifier) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void removePermission(ObjectPermission.Type permission,
            String identifier) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public Collection<String> getAccessibleObjects(
            Collection<ObjectPermission.Type> permissionTypes,
            Collection<String> identifiers) throws GuacamoleException {

        Collection<String> accessibleObjects = new ArrayList<String>(permissions.size());

        // For each identifier/permission combination
        for (String identifier : identifiers) {
            for (ObjectPermission.Type permissionType : permissionTypes) {

                // Add identifier if at least one requested permission is granted
                ObjectPermission permission = new ObjectPermission(permissionType, identifier);
                if (permissions.contains(permission)) {
                    accessibleObjects.add(identifier);
                    break;
                }

            }
        }

        return accessibleObjects;
        
    }

    @Override
    public void addPermissions(Set<ObjectPermission> permissions)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void removePermissions(Set<ObjectPermission> permissions)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
