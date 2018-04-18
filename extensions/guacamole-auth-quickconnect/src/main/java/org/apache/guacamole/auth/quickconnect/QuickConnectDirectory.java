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

package org.apache.guacamole.auth.quickconnect;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.quickconnect.utility.QCParser;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * Implementation of the Connection Directory, stored
 * completely in-memory.
 */
public class QuickConnectDirectory extends SimpleDirectory<Connection> {

    /**
     * The unique identifier of the root connection group.
     */
    private static final String ROOT_IDENTIFIER = "ROOT";

    /**
     * The connections to store.
     */
    private final Map<String, Connection> connections =
            new HashMap<String, Connection>();

    /**
     * The root connection group for this directory.
     */
    private final QuickConnectionGroup rootGroup;

    /**
     * The internal counter for connection IDs.
     */
    private AtomicInteger connectionId;

    /**
     * Creates a new QuickConnectDirectory which provides access to the
     * connections contained within the given Map.
     *
     * @param connections
     *     A Map of all connections that should be present in this
     *     connection directory.
     * @param rootGroup
     *     A group that should be at the base of this directory.
     */
    public QuickConnectDirectory(ConnectionGroup rootGroup) {
        this.rootGroup = (QuickConnectionGroup)rootGroup;
        this.connectionId = new AtomicInteger();
        super.setObjects(this.connections);
    }

    /**
     * Returns the current counter and then increments it.
     *
     * @return
     *     An Integer representing the next available connection
     *     ID to get used when adding connections.
     */
    private int getNextConnectionID() {
        return connectionId.getAndIncrement();
    }

    @Override
    public void add(Connection connection) throws GuacamoleException {
        connections.put(connection.getIdentifier(), connection);
    }

    /**
     * Create a SimpleConnection object from a GuacamoleConfiguration
     * and get an ID and place it on the tree, returning the new
     * connection identifier value.
     *
     * @param config
     *     The GuacamoleConfiguration to use to create the
     *     SimpleConnection object.
     *
     * @return
     *     The identifier of the connection created in the directory.
     *
     * @throws GuacamoleException
     *     If an error occurs adding the object to the tree.
     */
    public String create(GuacamoleConfiguration config) throws GuacamoleException {

        // Get the next connection ID
        String connectionId = Integer.toString(getNextConnectionID());

        // Generate a name for the configuration
        String name = QCParser.getName(config);

        // Create a new connection and set parent identifier.
        Connection connection = new SimpleConnection(name, connectionId, config);
        connection.setParentIdentifier(ROOT_IDENTIFIER);

        // Place the object in directory
        add(connection);

        // Add connection to the tree.
        this.rootGroup.addConnectionIdentifier(connectionId);

        return connectionId;
    }

}
