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

import org.mongodb.morphia.annotations.Property;

/**
 * Object representation of a Guacamole object which can be the child of another
 * object, such as a connection or sharing profile, as represented in the
 * database.
 */
public abstract class ChildObjectModel extends ObjectModel {

    /** The name. */
    @Property("name")
    private String name;

    /** The parent. */
    @Property("parent")
    private String parent;

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
     * Gets the parent.
     *
     * @return the parent
     */
    public String getParent() {
        return parent;
    }

    /**
     * Sets the parent.
     *
     * @param parent
     *            the new parent
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

}
