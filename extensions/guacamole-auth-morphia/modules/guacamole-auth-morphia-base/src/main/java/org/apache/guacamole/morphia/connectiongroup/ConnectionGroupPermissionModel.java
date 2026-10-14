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

package org.apache.guacamole.morphia.connectiongroup;

import org.apache.guacamole.morphia.permission.ObjectPermissionModel;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

/**
 * 
 * Object representation of an object-related Guacamole permission, as
 * represented in the database.
 * 
 * guacamole_connection_group_permission: { id: string, user: UserModel,
 * connection_group: ConnectionGroupModel, permission: PermissionType }
 *
 */
@Entity("guacamole_connection_group_permission")
public class ConnectionGroupPermissionModel extends ObjectPermissionModel {

    /** The connection group. */
    @Reference(value = "connection_group")
    private ConnectionGroupModel connectionGroup;

    /**
     * Instantiates a new connection group permission model.
     */
    public ConnectionGroupPermissionModel() {
        super();
    }

    /**
     * Gets the connection group.
     *
     * @return the connection group
     */
    public ConnectionGroupModel getConnectionGroup() {
        return connectionGroup;
    }

    /**
     * Sets the connection group.
     *
     * @param connectionGroup
     *            the new connection group
     */
    public void setConnectionGroup(ConnectionGroupModel connectionGroup) {
        this.connectionGroup = connectionGroup;
    }

}
