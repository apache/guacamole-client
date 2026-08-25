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

import java.util.Collection;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

/**
 * Object representation of a Guacamole object, such as a user or connection, as
 * represented in the database.
 */
public abstract class ObjectModel {

    /** The id. */
    @Id
    @Property("id")
    private ObjectId id;

    /** The identifier. */
    private String identifier;

    /** The arbitrary attributes. */
    @Embedded(value = "arbitraryAttributes")
    private ArbitraryAttributeMap arbitraryAttributes = new ArbitraryAttributeMap();

    /**
     * Gets the identifier.
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the identifier.
     *
     * @param identifier
     *            the new identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id != null ? id.toString() : null;
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
     * Sets the arbitrary attributes.
     *
     * @param arbitraryAttributes
     *            the new arbitrary attributes
     */
    public void setArbitraryAttributes(
            ArbitraryAttributeMap arbitraryAttributes) {
        this.arbitraryAttributes = arbitraryAttributes;
    }

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
    public ArbitraryAttributeMap getArbitraryAttributeMap() {
        return arbitraryAttributes;
    }

    /**
     * Returns whether at least one arbitrary attribute name/value pair has been
     * associated with this object.
     *
     * @return true if this object has at least one arbitrary attribute set,
     *         false otherwise.
     */
    public boolean hasArbitraryAttributes() {
        return !arbitraryAttributes.isEmpty();
    }

    /**
     * Returns a Collection view of the equivalent attribute model objects which
     * make up the map of arbitrary attribute name/value pairs returned by
     * getArbitraryAttributeMap(). Additions and removals on the returned
     * Collection directly affect the attribute map.
     *
     * @return A Collection view of the map returned by
     *         getArbitraryAttributeMap().
     */
    public Collection<ArbitraryAttributeModel> getArbitraryAttributes() {
        return arbitraryAttributes.toModelCollection();
    }

    /**
     * Replaces all arbitrary attributes associated with this object with the
     * attribute name/value pairs within the given collection of model objects.
     *
     * @param arbitraryAttributes
     *            The Collection of model objects containing the attribute
     *            name/value pairs which should replace all currently-stored
     *            arbitrary attributes, if any.
     */
    public void setArbitraryAttributes(
            Collection<ArbitraryAttributeModel> arbitraryAttributes) {
        this.arbitraryAttributes = ArbitraryAttributeMap
                .fromModelCollection(arbitraryAttributes);
    }

}
