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
import org.apache.guacamole.auth.common.base.EntityModelInterface;
import org.apache.guacamole.auth.common.permission.PermissionMapperInterface;
import org.apache.guacamole.auth.jdbc.base.EntityModel;

/**
 * Generic base for mappers which handle permissions.
 *
 * @param <PermissionType>
 *            The type of permission model object handled by this mapper.
 * 
 * @param <Mapper>
 *            The specific mapper.
 */
@SuppressWarnings("unchecked")
public abstract class PermissionMapperImp<PermissionType, Mapper>
        implements PermissionMapperInterface<PermissionType> {

    protected abstract Mapper getMapper();

    /**
     * Retrieves all permissions associated with the given entity (user or user
     * group).
     *
     * @param entity
     *            The entity to retrieve permissions for.
     *
     * @param effectiveGroups
     *            The identifiers of all groups that should be taken into
     *            account when determining the permissions effectively granted
     *            to the user. If no groups are given, only permissions directly
     *            granted to the user will be used.
     *
     * @return All permissions associated with the given entity.
     */
    public Collection<PermissionType> select(EntityModelInterface entity,
            Collection<String> effectiveGroups) {
        return ((PermissionMapper<PermissionType>) getMapper())
                .select((EntityModel) entity, effectiveGroups);
    }

    /**
     * Inserts the given permissions into the database. If any permissions
     * already exist, they will be ignored.
     *
     * @param permissions
     *            The permissions to insert.
     *
     * @return The number of rows inserted.
     */
    public int insert(Collection<PermissionType> permissions) {
        return ((PermissionMapper<PermissionType>) getMapper())
                .insert(permissions);
    }

    /**
     * Deletes the given permissions from the database. If any permissions do
     * not exist, they will be ignored.
     *
     * @param permissions
     *            The permissions to delete.
     *
     * @return The number of rows deleted.
     */
    public int delete(Collection<PermissionType> permissions) {
        return ((PermissionMapper<PermissionType>) getMapper())
                .delete(permissions);
    }

}
