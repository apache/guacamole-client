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

import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * Basic implementation of a Guacamole connection.
 *
 * @author Michael Jumper
 */
public abstract class AbstractConnection implements Connection {

    /**
     * The name associated with this connection.
     */
    private String name;

    /**
     * The unique identifier associated with this connection.
     */
    private String identifier;

    /**
     * The unique identifier of the parent ConnectionGroup for
     * this Connection.
     */
    private String parentIdentifier;

    /**
     * The GuacamoleConfiguration associated with this connection.
     */
    private GuacamoleConfiguration configuration;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getParentIdentifier() {
        return parentIdentifier;
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public int hashCode() {
        if (identifier == null) return 0;
        return identifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        // Not equal if null or not a Connection
        if (obj == null) return false;
        if (!(obj instanceof AbstractConnection)) return false;

        // Get identifier
        String objIdentifier = ((AbstractConnection) obj).identifier;

        // If null, equal only if this identifier is null
        if (objIdentifier == null) return identifier == null;

        // Otherwise, equal only if strings are identical
        return objIdentifier.equals(identifier);

    }

}
