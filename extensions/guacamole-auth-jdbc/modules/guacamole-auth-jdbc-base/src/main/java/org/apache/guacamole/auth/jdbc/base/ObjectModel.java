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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Object representation of a Guacamole object, such as a user or connection,
 * as represented in the database.
 *
 * @author Michael Jumper
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

    /**
     * Returns whether the given string is a valid identifier within the JDBC
     * authentication extension. Invalid identifiers may result in SQL errors
     * from the underlying database when used in queries.
     *
     * @param identifier
     *     The string to check for validity.
     *
     * @return
     *     true if the given string is a valid identifier, false otherwise.
     */
    public static boolean isValidIdentifier(String identifier) {

        // Empty identifiers are invalid
        if (identifier.isEmpty())
            return false;

        // Identifier is invalid if any non-numeric characters are present
        for (int i = 0; i < identifier.length(); i++) {
            if (!Character.isDigit(identifier.charAt(i)))
                return false;
        }

        // Identifier is valid - contains only numeric characters
        return true;

    }

    /**
     * Filters the given collection of strings, returning a new collection
     * containing only those strings which are valid identifiers. If no strings
     * within the collection are valid identifiers, the returned collection will
     * simply be empty.
     *
     * @param identifiers
     *     The collection of strings to filter.
     *
     * @return
     *     A new collection containing only the strings within the provided
     *     collection which are valid identifiers.
     */
    public static Collection<String> filterIdentifiers(Collection<String> identifiers) {

        // Obtain enough space for a full copy of the given identifiers
        Collection<String> validIdentifiers = new ArrayList<String>(identifiers.size());

        // Add only valid identifiers to the copy
        for (String identifier : identifiers) {
            if (ObjectModel.isValidIdentifier(identifier))
                validIdentifiers.add(identifier);
        }

        return validIdentifiers;

    }

}
