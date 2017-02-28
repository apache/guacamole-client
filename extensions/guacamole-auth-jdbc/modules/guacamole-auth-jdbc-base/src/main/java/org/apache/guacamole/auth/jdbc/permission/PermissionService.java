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

package org.apache.guacamole.auth.jdbc.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
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
    PermissionSetType getPermissionSet(ModeledAuthenticatedUser user,
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
    Set<PermissionType> retrievePermissions(ModeledAuthenticatedUser user,
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
    void createPermissions(ModeledAuthenticatedUser user, ModeledUser targetUser,
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
    void deletePermissions(ModeledAuthenticatedUser user, ModeledUser targetUser,
            Collection<PermissionType> permissions) throws GuacamoleException;

}
