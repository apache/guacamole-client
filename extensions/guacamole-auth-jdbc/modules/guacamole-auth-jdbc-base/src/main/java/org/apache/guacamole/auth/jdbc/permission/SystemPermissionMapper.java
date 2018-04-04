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

import org.apache.guacamole.auth.jdbc.base.EntityModel;
import org.apache.ibatis.annotations.Param;
import org.apache.guacamole.net.auth.permission.SystemPermission;

/**
 * Mapper for system-level permissions.
 */
public interface SystemPermissionMapper extends PermissionMapper<SystemPermissionModel> {

    /**
     * Retrieve the permission of the given type associated with the given
     * entity, if it exists. If no such permission exists, null is returned.
     *
     * @param entity
     *     The entity to retrieve permissions for.
     *
     * @param type
     *     The type of permission to return.
     *
     * @param inherit
     *     Whether permissions inherited through user groups should be taken
     *     into account. If false, only permissions granted directly will be
     *     included.
     *
     * @return
     *     The requested permission, or null if no such permission is granted
     *     to the given entity.
     */
    SystemPermissionModel selectOne(@Param("entity") EntityModel entity,
            @Param("type") SystemPermission.Type type,
            @Param("inherit") boolean inherit);

}
