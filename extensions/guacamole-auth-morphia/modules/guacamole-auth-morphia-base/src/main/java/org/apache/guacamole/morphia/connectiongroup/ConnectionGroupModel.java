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

package org.apache.guacamole.morphia.connectiongroup;

import java.util.HashSet;
import java.util.Set;

import org.apache.guacamole.morphia.base.ChildObjectModel;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;

/**
 * Object representation of a Guacamole connection group, as represented in the
 * database.
 * 
 * guacamole_connection_group: { id: string, (connection_group_id) parent:
 * ConnectionGroupModel, connection_group_name: string, type:
 * ConnectionGroup.Type, max_connections: int, max_connections_per_user: int,
 * enable_session_affinity: boolean }
 *
 */
@Entity("guacamole_connection_group")
public class ConnectionGroupModel extends ChildObjectModel {

    /**
     * The type of this connection group, such as organizational or balancing.
     */
    @Embedded(value = "type")
    private ConnectionGroup.Type type;

    /**
     * The maximum number of connections that can be established to this
     * connection group concurrently, zero if no restriction applies, or null if
     * the default restrictions should be applied.
     */
    @Property("max_connections")
    private Integer maxConnections;

    /**
     * The maximum number of connections that can be established to this
     * connection group concurrently by any one user, zero if no restriction
     * applies, or null if the default restrictions should be applied.
     */
    @Property("max_connections_per_user")
    private Integer maxConnectionsPerUser;

    /**
     * Whether individual users should be consistently assigned the same
     * connection within a balancing group until they log out.
     */
    @Property("enable_session_affinity")
    private boolean sessionAffinityEnabled;

    /**
     * The identifiers of all readable child connections within this connection
     * group.
     */
    private Set<String> connectionIdentifiers = new HashSet<String>();

    /**
     * The identifiers of all readable child connection groups within this
     * connection group.
     */
    private Set<String> connectionGroupIdentifiers = new HashSet<String>();

    /**
     * Instantiates a new connection group model.
     */
    public ConnectionGroupModel() {
        this.type = ConnectionGroup.Type.ORGANIZATIONAL;
        this.sessionAffinityEnabled = Boolean.FALSE;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public ConnectionGroup.Type getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type
     *            the new type
     */
    public void setType(ConnectionGroup.Type type) {
        this.type = type;
    }

    /**
     * Gets the max connections.
     *
     * @return the max connections
     */
    public Integer getMaxConnections() {
        return maxConnections;
    }

    /**
     * Sets the max connections.
     *
     * @param maxConnections
     *            the new max connections
     */
    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Gets the max connections per user.
     *
     * @return the max connections per user
     */
    public Integer getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    /**
     * Sets the max connections per user.
     *
     * @param maxConnectionsPerUser
     *            the new max connections per user
     */
    public void setMaxConnectionsPerUser(Integer maxConnectionsPerUser) {
        this.maxConnectionsPerUser = maxConnectionsPerUser;
    }

    /**
     * Checks if is session affinity enabled.
     *
     * @return true, if is session affinity enabled
     */
    public boolean isSessionAffinityEnabled() {
        return sessionAffinityEnabled;
    }

    /**
     * Sets the session affinity enabled.
     *
     * @param sessionAffinityEnabled
     *            the new session affinity enabled
     */
    public void setSessionAffinityEnabled(boolean sessionAffinityEnabled) {
        this.sessionAffinityEnabled = sessionAffinityEnabled;
    }

    /**
     * Gets the connection identifiers.
     *
     * @return the connection identifiers
     */
    public Set<String> getConnectionIdentifiers() {
        return connectionIdentifiers;
    }

    /**
     * Sets the connection identifiers.
     *
     * @param connectionIdentifiers
     *            the new connection identifiers
     */
    public void setConnectionIdentifiers(Set<String> connectionIdentifiers) {
        this.connectionIdentifiers = connectionIdentifiers;
    }

    /**
     * Gets the connection group identifiers.
     *
     * @return the connection group identifiers
     */
    public Set<String> getConnectionGroupIdentifiers() {
        return connectionGroupIdentifiers;
    }

    /**
     * Sets the connection group identifiers.
     *
     * @param connectionGroupIdentifiers
     *            the new connection group identifiers
     */
    public void setConnectionGroupIdentifiers(
            Set<String> connectionGroupIdentifiers) {
        this.connectionGroupIdentifiers = connectionGroupIdentifiers;
    }

    @Override
    public String getIdentifier() {
        return getId();

    }

    @Override
    public void setIdentifier(String identifier) {
        throw new UnsupportedOperationException(
                "Connection group identifiers are derived from IDs. They cannot be set.");
    }

}
