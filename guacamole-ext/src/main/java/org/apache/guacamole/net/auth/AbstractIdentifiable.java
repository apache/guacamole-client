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

package org.apache.guacamole.net.auth;

/**
 * Abstract implementation of Identifiable which provides equals() and
 * hashCode() implementations which use the identifier to determine equality.
 * The identifier comparison is case-sensitive.
 */
public abstract class AbstractIdentifiable implements Identifiable {

    /**
     * The unique string which identifies this object.
     */
    private String identifier;

    @Override
    public String getIdentifier() {
        if (identifier == null || isCaseSensitive())
            return identifier;
        
        return identifier.toLowerCase();
    }

    @Override
    public void setIdentifier(String identifier) {
        if (isCaseSensitive() || identifier == null)
            this.identifier = identifier;
        else
            this.identifier = identifier.toLowerCase();
    }

    @Override
    public int hashCode() {

        if (identifier == null)
            return 0;

        if (isCaseSensitive())
            return identifier.hashCode();
        
        return identifier.toLowerCase().hashCode();
    }

    @Override
    public boolean equals(Object other) {

        // Not equal if null or not the same type of object
        if (other == null || getClass() != other.getClass())
            return false;

        // Get identifier of other object
        String otherIdentifier = ((AbstractIdentifiable) other).getIdentifier();

        // If null, equal only if this identifier is null
        if (otherIdentifier == null)
            return identifier == null;

        // If either this identifier or the one we're comparing to is
        // case-sensitive, evaluate with case sensitivity.
        if (isCaseSensitive() || ((AbstractIdentifiable) other).isCaseSensitive())
            return otherIdentifier.equals(identifier);
        
        // Both identifiers can be evaluated in a case-insensitive manner.
        return otherIdentifier.equalsIgnoreCase(identifier);

    }

}
