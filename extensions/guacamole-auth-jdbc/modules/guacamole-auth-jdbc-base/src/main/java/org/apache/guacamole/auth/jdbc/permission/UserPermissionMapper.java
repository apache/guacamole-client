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
import org.apache.guacamole.auth.jdbc.base.EntityModel;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for user permissions.
 */
public interface UserPermissionMapper extends ObjectPermissionMapper {
    
    /**
     * Deletes the given permissions from the database. If any permissions do
     * not exist, they will be ignored.
     *
     * @param permissions
     *     The permissions to delete.
     *
     * @param caseSensitive
     *     Whether or not string comparisons for usernames will be done in a
     *     case-sensitive manner.
     * 
     * @return
     *     The number of rows deleted.
     */
    int delete(@Param("permissions") Collection<ObjectPermission.Type> permissions,
            @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Inserts the given permissions into the database. If any permissions
     * already exist, they will be ignored.
     *
     * @param permissions 
     *     The permissions to insert.
     * 
     * @param caseSensitive
     *     Whether or not string comparisons for usernames will be done in a
     *     case-sensitive manner.
     *
     * @return
     *     The number of rows inserted.
     */
    int insert(@Param("permissions") Collection<ObjectPermission.Type> permissions,
            @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Retrieves all permissions associated with the given entity (user or user
     * group).
     *
     * @param entity
     *     The entity to retrieve permissions for.
     *
     * @param effectiveGroups
     *     The identifiers of all groups that should be taken into account
     *     when determining the permissions effectively granted to the user. If
     *     no groups are given, only permissions directly granted to the user
     *     will be used.
     * 
     * @param caseSensitive
     *     Whether or not string comparisons for usernames will be done in a
     *     case-sensitive manner.
     *
     * @return
     *     All permissions associated with the given entity.
     */
    Collection<ObjectPermission.Type> select(@Param("entity") EntityModel entity,
            @Param("effectiveGroups") Collection<String> effectiveGroups,
            @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Retrieve the permission of the given type associated with the given
     * entity and object, if it exists. If no such permission exists, null is
     * returned.
     *
     * @param entity
     *     The entity to retrieve permissions for.
     *
     * @param type
     *     The type of permission to return.
     *
     * @param identifier
     *     The identifier of the object affected by the permission to return.
     *
     * @param effectiveGroups
     *     The identifiers of all groups that should be taken into account
     *     when determining the permissions effectively granted to the user. If
     *     no groups are given, only permissions directly granted to the user
     *     will be used.
     * 
     * @param caseSensitive
     *     Whether or not string comparisons for usernames will be done in a
     *     case-sensitive manner.
     *
     * @return
     *     The requested permission, or null if no such permission is granted
     *     to the given entity for the given object.
     */
    ObjectPermissionModel selectOne(@Param("entity") EntityModel entity,
            @Param("type") ObjectPermission.Type type,
            @Param("identifier") String identifier,
            @Param("effectiveGroups") Collection<String> effectiveGroups,
            @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Retrieves the subset of the given identifiers for which the given entity
     * has at least one of the given permissions.
     *
     * @param entity
     *     The entity to check permissions of.
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
     * @param effectiveGroups
     *     The identifiers of all groups that should be taken into account
     *     when determining the permissions effectively granted to the user. If
     *     no groups are given, only permissions directly granted to the user
     *     will be used.
     * 
     * @param caseSensitive
     *     Whether or not string comparisons for usernames will be done in a
     *     case-sensitive manner.
     *
     * @return
     *     A collection containing the subset of identifiers for which at least
     *     one of the specified permissions is granted.
     */
    Collection<String> selectAccessibleIdentifiers(@Param("entity") EntityModel entity,
            @Param("permissions") Collection<ObjectPermission.Type> permissions,
            @Param("identifiers") Collection<String> identifiers,
            @Param("effectiveGroups") Collection<String> effectiveGroups,
            @Param("caseSensitive") boolean caseSensitive);
    
}
