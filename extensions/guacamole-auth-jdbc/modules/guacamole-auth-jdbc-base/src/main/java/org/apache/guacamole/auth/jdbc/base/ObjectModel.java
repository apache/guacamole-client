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

package org.apache.guacamole.auth.jdbc.base;

/**
 * Object representation of a Guacamole object, such as a user or connection,
 * as represented in the database.
 */
public abstract class ObjectModel {

    /**
     * The ID of this object in the database, if any.
     */
    private Integer objectID;

    /**
     * The unique identifier which identifies this object.
     */
    private String identifier;
    
    /**
     * Creates a new, empty object.
     */
    public ObjectModel() {
    }

    /**
     * Returns the identifier that uniquely identifies this object.
     *
     * @return
     *     The identifier that uniquely identifies this object.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the identifier that uniquely identifies this object.
     *
     * @param identifier
     *     The identifier that uniquely identifies this object.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the ID of this object in the database, if it exists.
     *
     * @return
     *     The ID of this object in the database, or null if this object was
     *     not retrieved from the database.
     */
    public Integer getObjectID() {
        return objectID;
    }

    /**
     * Sets the ID of this object to the given value.
     *
     * @param objectID
     *     The ID to assign to this object.
     */
    public void setObjectID(Integer objectID) {
        this.objectID = objectID;
    }

}
