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

import org.apache.guacamole.morphia.user.UserModel;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

/**
 * Generic base permission model which grants a permission of a particular type
 * to a specific user.
 *
 * @param <PermissionType>
 *            The type of permissions allowed within this model.
 */
public abstract class PermissionModel<PermissionType> {

    /** The id. */
    @Id
    @Property("id")
    private ObjectId id;

    /** The user. */
    @Reference(value = "user")
    private UserModel user;

    /** The username. */
    @Property("username")
    private String username;

    /** The type. */
    @Embedded(value = "permission")
    private PermissionType type;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id.toString();
    }

    /**
     * Sets the id.
     *
     * @param id
     *            the new id
     */
    public void setId(String id) {
        this.id = new ObjectId(id);
    }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public UserModel getUser() {
        return user;
    }

    /**
     * Sets the user.
     *
     * @param user
     *            the new user
     */
    public void setUser(UserModel user) {
        this.user = user;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username
     *            the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public PermissionType getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type
     *            the new type
     */
    public void setType(PermissionType type) {
        this.type = type;
    }

}
