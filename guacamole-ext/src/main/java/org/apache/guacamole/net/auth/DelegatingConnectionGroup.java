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

import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleClientInformation;

/**
 * ConnectionGroup implementation which simply delegates all function calls to
 * an underlying ConnectionGroup.
 */
public class DelegatingConnectionGroup implements ConnectionGroup {

    /**
     * The wrapped ConnectionGroup.
     */
    private final ConnectionGroup connectionGroup;

    /**
     * Wraps the given ConnectionGroup such that all function calls against this
     * DelegatingConnectionGroup will be delegated to it.
     *
     * @param connectionGroup
     *     The ConnectionGroup to wrap.
     */
    public DelegatingConnectionGroup(ConnectionGroup connectionGroup) {
        this.connectionGroup = connectionGroup;
    }

    /**
     * Returns the underlying ConnectionGroup wrapped by this
     * DelegatingConnectionGroup.
     *
     * @return
     *     The ConnectionGroup wrapped by this DelegatingConnectionGroup.
     */
    protected ConnectionGroup getDelegateConnectionGroup() {
        return connectionGroup;
    }

    @Override
    public String getIdentifier() {
        return connectionGroup.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        connectionGroup.setIdentifier(identifier);
    }

    @Override
    public String getName() {
        return connectionGroup.getName();
    }

    @Override
    public void setName(String name) {
        connectionGroup.setName(name);
    }

    @Override
    public String getParentIdentifier() {
        return connectionGroup.getParentIdentifier();
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {
        connectionGroup.setParentIdentifier(parentIdentifier);
    }

    @Override
    public void setType(Type type) {
        connectionGroup.setType(type);
    }

    @Override
    public Type getType() {
        return connectionGroup.getType();
    }

    @Override
    public Set<String> getConnectionIdentifiers() throws GuacamoleException {
        return connectionGroup.getConnectionIdentifiers();
    }

    @Override
    public Set<String> getConnectionGroupIdentifiers() throws GuacamoleException {
        return connectionGroup.getConnectionGroupIdentifiers();
    }

    @Override
    public Map<String, String> getAttributes() {
        return connectionGroup.getAttributes();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        connectionGroup.setAttributes(attributes);
    }

    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info) throws GuacamoleException {
        return connectionGroup.connect(info);
    }

    @Override
    public int getActiveConnections() {
        return connectionGroup.getActiveConnections();
    }

}
