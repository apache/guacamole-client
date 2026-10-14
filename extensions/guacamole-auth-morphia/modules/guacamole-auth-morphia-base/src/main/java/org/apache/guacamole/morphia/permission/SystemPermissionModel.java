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

package org.apache.guacamole.morphia.permission;

import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.mongodb.morphia.annotations.Entity;

/**
 * 
 * Object representation of an system-level Guacamole permission, as represented
 * in the database.
 * 
 * guacamole_system_permission: { id: string, user: UserModel, permission:
 * PermissionType }
 *
 */
@Entity("guacamole_system_permission")
public class SystemPermissionModel
        extends PermissionModel<SystemPermission.Type> {

    /**
     * Creates a new, empty System permission.
     */
    public SystemPermissionModel() {
    }

}
