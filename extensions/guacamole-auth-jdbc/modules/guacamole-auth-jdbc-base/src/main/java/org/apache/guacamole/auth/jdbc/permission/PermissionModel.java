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

/**
 * Generic base permission model which grants a permission of a particular type
 * to a specific user.
 *
 * @param <PermissionType>
 *     The type of permissions allowed within this model.
 */
public abstract class PermissionModel<PermissionType> {

    /**
     * The database ID of the user to whom this permission is granted.
     */
    private Integer userID;

    /**
     * The username of the user to whom this permission is granted.
     */
    private String username;

    /**
     * The type of action granted by this permission.
     */
    private PermissionType type;
    
    /**
     * Returns the database ID of the user to whom this permission is granted.
     * 
     * @return
     *     The database ID of the user to whom this permission is granted.
     */
    public Integer getUserID() {
        return userID;
    }

    /**
     * Sets the database ID of the user to whom this permission is granted.
     *
     * @param userID
     *     The database ID of the user to whom this permission is granted.
     */
    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    /**
     * Returns the username of the user to whom this permission is granted.
     * 
     * @return
     *     The username of the user to whom this permission is granted.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user to whom this permission is granted.
     *
     * @param username
     *     The username of the user to whom this permission is granted.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the type of action granted by this permission.
     *
     * @return
     *     The type of action granted by this permission.
     */
    public PermissionType getType() {
        return type;
    }

    /**
     * Sets the type of action granted by this permission.
     *
     * @param type
     *     The type of action granted by this permission.
     */
    public void setType(PermissionType type) {
        this.type = type;
    }

}
