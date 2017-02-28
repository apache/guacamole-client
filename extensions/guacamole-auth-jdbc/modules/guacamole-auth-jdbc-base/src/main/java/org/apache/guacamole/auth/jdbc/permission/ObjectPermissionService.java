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

import java.util.Collection;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * deleting object permissions. This service will automatically enforce the
 * permissions of the current user.
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
    ObjectPermission retrievePermission(ModeledAuthenticatedUser user,
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
    Collection<String> retrieveAccessibleIdentifiers(ModeledAuthenticatedUser user,
            ModeledUser targetUser, Collection<ObjectPermission.Type> permissions,
            Collection<String> identifiers) throws GuacamoleException;

}
