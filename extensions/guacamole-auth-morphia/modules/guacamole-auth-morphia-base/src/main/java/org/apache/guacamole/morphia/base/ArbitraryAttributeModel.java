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

package org.apache.guacamole.morphia.base;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

/**
 * A single attribute name/value pair belonging to a object which implements the
 * Attributes interface, such as a Connection or User. Attributes stored as raw
 * name/value pairs are the attributes which are given to the database
 * authentication extension for storage by other extensions. Attributes which
 * are directly supported by the database authentication extension have defined
 * columns and properties with proper types, constraints, etc.
 */
public class ArbitraryAttributeModel {

    /** The id. */
    @Id
    @Property("id")
    private ObjectId id;

    /** The name. */
    @Property("attribute_name")
    private String name;

    /** The value. */
    @Property("attribute_value")
    private String value;

    /**
     * Instantiates a new arbitrary attribute model.
     */
    public ArbitraryAttributeModel() {
    }

    /**
     * Instantiates a new arbitrary attribute model.
     *
     * @param name
     *            the name
     * @param value
     *            the value
     */
    public ArbitraryAttributeModel(String name, String value) {
        this.name = name;
        this.value = value;
    }

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
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name
     *            the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value
     *            the new value
     */
    public void setValue(String value) {
        this.value = value;
    }

}
