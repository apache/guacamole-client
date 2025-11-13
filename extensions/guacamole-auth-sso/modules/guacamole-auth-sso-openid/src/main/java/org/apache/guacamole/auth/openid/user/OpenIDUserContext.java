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

package org.apache.guacamole.auth.openid.user;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.openid.usermapping.UserMappingService;
import org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser;
import org.apache.guacamole.net.auth.AbstractUserContext;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.simple.SimpleConnection;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroup;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;
import org.apache.guacamole.net.auth.simple.SimpleObjectPermissionSet;
import org.apache.guacamole.net.auth.simple.SimpleUser;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserContext implementation that provides connections from user-mapping.xml
 * to OpenID-authenticated users who have the "admin" or "console_accesser" role.
 * All connections are placed in the ROOT group.
 */
public class OpenIDUserContext extends AbstractUserContext {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(OpenIDUserContext.class);

    /**
     * Required roles that allow access to connections from user-mapping.xml.
     */
    private static final String ADMIN_ROLE = "admin";
    private static final String CONSOLE_ACCESSER_ROLE = "console_accesser";

    /**
     * The unique identifier for the root connection group.
     */
    private static final String ROOT_CONNECTION_GROUP = "ROOT";

    /**
     * The AuthenticationProvider that created this UserContext.
     */
    private final AuthenticationProvider authProvider;

    /**
     * The username of the authenticated user.
     */
    private final String username;

    /**
     * The authenticated user object containing role information.
     */
    private SSOAuthenticatedUser authenticatedUser;

    /**
     * Directory containing all connections from user-mapping.xml.
     */
    private Directory<Connection> connectionDirectory;

    /**
     * The root connection group containing all connections.
     */
    private ConnectionGroup rootGroup;

    /**
     * Service for reading user-mapping.xml.
     */
    @Inject
    private UserMappingService userMappingService;

    /**
     * Creates a new OpenIDUserContext for the given user and authentication provider.
     *
     * @param authProvider
     *     The AuthenticationProvider that created this UserContext.
     *
     * @param username
     *     The username of the authenticated user.
     *
     * @param authenticatedUser
     *     The authenticated user object containing role information from Keycloak.
     */
    public OpenIDUserContext(AuthenticationProvider authProvider, String username, 
            SSOAuthenticatedUser authenticatedUser) {
        this.authProvider = authProvider;
        this.username = username;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Checks if the authenticated user has the required role to access connections.
     * The user must have either "admin" or "console_accesser" role from Keycloak.
     *
     * @return
     *     true if the user has the required role, false otherwise.
     */
    private boolean hasRequiredRole() {
        if (authenticatedUser == null) {
            logger.debug("No authenticated user available for role check.");
            return false;
        }

        Set<String> groups = authenticatedUser.getEffectiveUserGroups();
        if (groups == null || groups.isEmpty()) {
            logger.debug("User {} has no groups/roles assigned.", username);
            return false;
        }

        // Check if user has admin or console_accesser role
        boolean hasAdmin = groups.contains(ADMIN_ROLE);
        boolean hasConsoleAccesser = groups.contains(CONSOLE_ACCESSER_ROLE);

        if (hasAdmin || hasConsoleAccesser) {
            logger.debug("User {} has required role (admin: {}, console_accesser: {})", 
                    username, hasAdmin, hasConsoleAccesser);
            return true;
        }

        logger.debug("User {} does not have required role. Available roles: {}", 
                username, groups);
        return false;
    }

    /**
     * Initializes this UserContext by reading connections from user-mapping.xml.
     * Connections are only provided if the user has the required role (admin or console_accesser).
     * This method must be called after dependency injection is complete.
     *
     * @throws GuacamoleException
     *     If the UserContext cannot be initialized due to an error.
     */
    public void init() throws GuacamoleException {
        // Check if user has required role
        if (!hasRequiredRole()) {
            logger.info("User {} does not have required role (admin or console_accesser). " +
                    "No connections from user-mapping.xml will be provided.", username);
            // Create empty directories
            this.connectionDirectory = new SimpleDirectory<Connection>(Collections.<Connection>emptyMap());
            this.rootGroup = new SimpleConnectionGroup(
                    ROOT_CONNECTION_GROUP,
                    ROOT_CONNECTION_GROUP,
                    Collections.<String>emptySet(),
                    Collections.<String>emptyList()
            );
            return;
        }

        // Get all connections from user-mapping.xml
        Map<String, GuacamoleConfiguration> configs = userMappingService.getAllConnections();

        // Create connections from configurations
        Map<String, Connection> connections = new ConcurrentHashMap<String, Connection>(configs.size());
        for (Map.Entry<String, GuacamoleConfiguration> configEntry : configs.entrySet()) {

            // Get connection identifier and configuration
            String identifier = configEntry.getKey();
            GuacamoleConfiguration config = configEntry.getValue();

            // Add as simple connection, interpreting tokens
            Connection connection = new SimpleConnection(identifier, identifier, config, true);
            connection.setParentIdentifier(ROOT_CONNECTION_GROUP);
            connections.put(identifier, connection);

        }

        // Create connection directory
        this.connectionDirectory = new SimpleDirectory<Connection>(connections);

        // Create root connection group containing all connections
        this.rootGroup = new SimpleConnectionGroup(
                ROOT_CONNECTION_GROUP,
                ROOT_CONNECTION_GROUP,
                connectionDirectory.getIdentifiers(),
                Collections.<String>emptyList()
        );
    }

    @Override
    public User self() {
        return new SimpleUser(username) {

            @Override
            public ObjectPermissionSet getConnectionPermissions() throws GuacamoleException {
                return new SimpleObjectPermissionSet(connectionDirectory.getIdentifiers());
            }

            @Override
            public ObjectPermissionSet getConnectionGroupPermissions() throws GuacamoleException {
                return new SimpleObjectPermissionSet(Collections.singleton(ROOT_CONNECTION_GROUP));
            }

        };
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {
        return rootGroup;
    }

}

