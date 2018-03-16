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

import java.util.Collection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.quickconnect.utility.QCParser;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.simple.SimpleConnectionDirectory;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * Implementation of the Connection Directory, stored
 * completely in-memory.
 */
public class QuickConnectDirectory extends SimpleConnectionDirectory {

    /**
     * The unique identifier of the root connection group.
     */
    private static final String ROOT_IDENTIFIER = "ROOT";

    /**
     * The root connection group for this directory.
     */
    private final QuickConnectConnectionGroup rootGroup;

    /**
     * The internal counter for connection IDs.
     */
    private int CONNECTION_ID = 0;

    /**
     * Creates a new QuickConnectDirectory which provides access to the
     * connections contained within the given Map.
     *
     * @param connections
     *     A Collection of all connections that should be present in this
     *     connection directory.
     * @param rootGroup
     *     A group that should be at the base of this directory.
     */
    public QuickConnectDirectory(Collection<Connection> connections, ConnectionGroup rootGroup) {
        super(connections);
        this.rootGroup = (QuickConnectConnectionGroup)rootGroup;
    }

    /**
     * Returns the current counter and then increments it.
     *
     * @return
     *     An Integer representing the next available connection
     *     ID to get used when adding connections.
     */
    private Integer getNextConnectionID() {
        return CONNECTION_ID++;
    }

    @Override
    public void add(Connection object) throws GuacamoleException {

        put(new QuickConnection(object));

    }

    /**
     * Create a connection object on the tree using an existing
     * QuickConnection connection, after setting the identifier
     * and parent object.
     *
     * @param connection
     *     The QuickConnection object to add to the tree.
     *
     * @return
     *     The connectionId of the object added to the directory.
     *
     * @throws GuacamoleException
     *     If an error is encountered adding the object to the
     *     directory.
     */
    public String put(QuickConnection connection) throws GuacamoleException {

        // Get the next connection ID.
        String connectionId = getNextConnectionID().toString();

        // Set up identifier and parent on object.
        connection.setIdentifier(connectionId);
        connection.setParentIdentifier(ROOT_IDENTIFIER);

        // Add connection to the directory
        putConnection(connection);

        // Add connection to the tree
        this.rootGroup.addConnectionIdentifier(connectionId);

        return connectionId;

    }

    /**
     * Create a QuickConnection object from a GuacamoleConfiguration
     * and get an ID and place it on the tree.
     *
     * @param config
     *     The GuacamoleConfiguration to use to create the
     *     QuickConnection object.
     *
     * @return
     *     The connectionId of the object creation in the directory.
     *
     * @throws GuacamoleException
     *     If an error occurs adding the object to the tree.
     */
    public String create(GuacamoleConfiguration config) throws GuacamoleException {

        // Get the next connection ID
        String connectionId = getNextConnectionID().toString();

        // Generate a name for the configuration
        String name = QCParser.getName(config);

        // Create a new connection and set parent identifier.
        Connection connection = new QuickConnection(name, connectionId, config);
        connection.setParentIdentifier(ROOT_IDENTIFIER);

        // Place the object in directory
        putConnection(connection);

        // Add connection to the tree.
        this.rootGroup.addConnectionIdentifier(connectionId);

        return connectionId;
    }

    @Override
    public void update(Connection object) throws GuacamoleException {
        putConnection(object);
    }

}
