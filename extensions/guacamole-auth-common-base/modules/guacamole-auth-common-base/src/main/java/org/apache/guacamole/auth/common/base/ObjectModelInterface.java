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

package org.apache.guacamole.auth.common.base;

import java.util.Collection;

/**
 * Object representation of a Guacamole object, such as a user or connection, as
 * represented in the database.
 */
public interface ObjectModelInterface {

    /**
     * Returns the identifier that uniquely identifies this object.
     *
     * @return The identifier that uniquely identifies this object.
     */
    public String getIdentifier();

    /**
     * Sets the identifier that uniquely identifies this object.
     *
     * @param identifier
     *            The identifier that uniquely identifies this object.
     */
    public void setIdentifier(String identifier);

    /**
     * Returns a map of attribute name/value pairs for all attributes associated
     * with this model which do not have explicit mappings to actual model
     * properties. All other attributes (those which are explicitly supported by
     * the model) should instead be mapped to properties with corresponding and
     * properly-typed columns.
     *
     * @return A map of attribute name/value pairs for all attributes associated
     *         with this model which do not otherwise have explicit mappings to
     *         properties.
     */
    public ArbitraryAttributeMapInterface getArbitraryAttributeMap();

    /**
     * Returns a Collection view of the equivalent attribute model objects which
     * make up the map of arbitrary attribute name/value pairs returned by
     * getArbitraryAttributeMap(). Additions and removals on the returned
     * Collection directly affect the attribute map.
     *
     * @return A Collection view of the map returned by
     *         getArbitraryAttributeMap().
     */
    public Collection<ArbitraryAttributeModelInterface> getArbitraryAttributes();

    /**
     * Returns the ID of this object in the database, if it exists.
     *
     * @return The ID of this object in the database, or null if this object was
     *         not retrieved from the database.
     */
    public Object getObjectID();

    /**
     * Returns whether at least one arbitrary attribute name/value pair has been
     * associated with this object.
     *
     * @return true if this object has at least one arbitrary attribute set,
     *         false otherwise.
     */
    public boolean hasArbitraryAttributes();

}
