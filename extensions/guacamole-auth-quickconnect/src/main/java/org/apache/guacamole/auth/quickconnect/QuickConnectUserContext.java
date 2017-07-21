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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.ConnectionRecordSet;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroup;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroupDirectory;
import org.apache.guacamole.net.auth.simple.SimpleConnectionRecordSet;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;
import org.apache.guacamole.net.auth.simple.SimpleUser;
import org.apache.guacamole.net.auth.simple.SimpleUserDirectory;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extremely simple UserContext implementation which provides access to
 * a defined and restricted set of GuacamoleConfigurations. Access to
 * querying or modifying either users or permissions is denied.
 */
public class QuickConnectUserContext implements UserContext {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(QuickConnectUserContext.class);

    /**
     * The unique identifier of the root connection group.
     */
    private static final String ROOT_IDENTIFIER = "ROOT";

    /**
     * The AuthenticationProvider that created this UserContext.
     */
    private final AuthenticationProvider authProvider;

    /**
     * Reference to the user whose permissions dictate the configurations
     * accessible within this UserContext.
     */
    private final User self;

    /**
     * The Directory with access only to the User associated with this
     * UserContext.
     */
    private final Directory<User> userDirectory;

    /**
     * The Directory with access only to the root group associated with this
     * UserContext.
     */
    private final Directory<ConnectionGroup> connectionGroupDirectory;

    /**
     * The Directory with access to all connections within the root group
     * associated with this UserContext.
     */
    private final Directory<Connection> connectionDirectory;

    /**
     * The root connection group.
     */
    private final ConnectionGroup rootGroup;

    /**
     * Creates a new SimpleUserContext which provides access to only those
     * configurations within the given Map. The username is assigned
     * arbitrarily.
     *
     * @param authProvider
     *     The AuthenticationProvider creating this UserContext.
     *
     * @param configs
     *     A Map of all configurations for which the user associated with this
     *     UserContext has read access.
     */
    public QuickConnectUserContext(AuthenticationProvider authProvider,
            Map<String, GuacamoleConfiguration> configs) {
        this(authProvider, UUID.randomUUID().toString(), configs);
        logger.debug(">>>QuickConnect<<< Constructor with authProvider and configs.");
    }

    /**
     * Creates a new SimpleUserContext for the user with the given username
     * which provides access to only those configurations within the given Map.
     *
     * @param authProvider
     *     The AuthenticationProvider creating this UserContext.
     *
     * @param username
     *     The username of the user associated with this UserContext.
     *
     * @param configs
     *     A Map of all configurations for which the user associated with
     *     this UserContext has read access.
     */
    public QuickConnectUserContext(AuthenticationProvider authProvider,
            String username, Map<String, GuacamoleConfiguration> configs) {

        Collection<String> connectionIdentifiers = new ArrayList<String>(configs.size());
        Collection<String> connectionGroupIdentifiers = Collections.singleton(ROOT_IDENTIFIER);
        
        // Produce collection of connections from given configs
        Collection<Connection> connections = new ArrayList<Connection>(configs.size());
        for (Map.Entry<String, GuacamoleConfiguration> configEntry : configs.entrySet()) {

            // Get connection identifier and configuration
            String identifier = configEntry.getKey();
            GuacamoleConfiguration config = configEntry.getValue();

            // Add as simple connection
            Connection connection = new SimpleConnection(identifier, identifier, config);
            connection.setParentIdentifier(ROOT_IDENTIFIER);
            connections.add(connection);

            // Add identifier to overall set of identifiers
            connectionIdentifiers.add(identifier);
            
        }
        
        // Add root group that contains only the given configurations
        this.rootGroup = new QuickConnectConnectionGroup(
            ROOT_IDENTIFIER, ROOT_IDENTIFIER,
            connectionIdentifiers
        );

        // Build new user from credentials
        this.self = new SimpleUser(username, connectionIdentifiers,
                connectionGroupIdentifiers);

        // Create directories for new user
        this.userDirectory = new SimpleUserDirectory(self);
        this.connectionDirectory = new QuickConnectDirectory(connections,this.rootGroup);
        this.connectionGroupDirectory = new SimpleConnectionGroupDirectory(Collections.singleton(this.rootGroup));

        // Associate provided AuthenticationProvider
        this.authProvider = authProvider;

    }

    public QuickConnectUserContext(AuthenticationProvider authProvider,
            String username) {

        this.rootGroup = new QuickConnectConnectionGroup(
            ROOT_IDENTIFIER, ROOT_IDENTIFIER
        );

        this.self = new SimpleUser(username,
            Collections.<String>emptyList(),
            Collections.singleton(ROOT_IDENTIFIER)
        );

        this.userDirectory = new SimpleUserDirectory(self);
        this.connectionDirectory = new QuickConnectDirectory(Collections.<Connection>emptyList(), this.rootGroup);
        this.connectionGroupDirectory = new SimpleConnectionGroupDirectory(Collections.singleton(this.rootGroup));

        this.authProvider = authProvider;

    }

    public void setConfigs(Map<String, GuacamoleConfiguration> configs) {

        return;

    }

    @Override
    public User self() {
        return self;
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return null;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Directory<User> getUserDirectory()
            throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< Returning the entire user directory: {}", userDirectory.getIdentifiers());

        return userDirectory;
    }

    @Override
    public Directory<Connection> getConnectionDirectory()
            throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< Returning the entire connection directory: {}", connectionDirectory.getIdentifiers());

        return connectionDirectory;
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory()
            throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< Returning the entire group directory: {}", connectionGroupDirectory.getIdentifiers());

        return connectionGroupDirectory;
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< Returning the entire root Connection Group: {}", rootGroup);

        return rootGroup;
    }

    @Override
    public Directory<SharingProfile> getSharingProfileDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<SharingProfile>();
    }

    @Override
    public Directory<ActiveConnection> getActiveConnectionDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<ActiveConnection>();
    }

    @Override
    public ConnectionRecordSet getConnectionHistory()
            throws GuacamoleException {
        return new SimpleConnectionRecordSet();
    }

    @Override
    public Collection<Form> getUserAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getConnectionAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getSharingProfileAttributes() {
        return Collections.<Form>emptyList();
    }

}
