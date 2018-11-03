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

import java.util.Collections;
import org.apache.guacamole.auth.quickconnect.rest.QuickConnectREST;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AbstractUserContext;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.simple.SimpleObjectPermissionSet;
import org.apache.guacamole.net.auth.simple.SimpleUser;

/**
 * A simple implementation of UserContext to support this
 * extension, used for storing connections the user has created
 * with the QuickConnect bar in the webapp.
 */
public class QuickConnectUserContext extends AbstractUserContext {

    /**
     * The unique identifier of the root connection group.
     */
    public static final String ROOT_IDENTIFIER = DEFAULT_ROOT_CONNECTION_GROUP;

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
     * The Directory with access to all connections within the root group
     * associated with this UserContext.
     */
    private final QuickConnectDirectory connectionDirectory;

    /**
     * The root connection group.
     */
    private final ConnectionGroup rootGroup;

    /**
     * Construct a QuickConnectUserContext using the authProvider and
     * the username.
     *
     * @param authProvider
     *     The authentication provider module instantiating this
     *     this class.
     *
     * @param username
     *     The name of the user logging in that will be associated
     *     with this UserContext.
     * 
     * @throws GuacamoleException
     *     If errors occur initializing the ConnectionGroup,
     *     ConnectionDirectory, or User.
     */
    public QuickConnectUserContext(AuthenticationProvider authProvider,
            String username) throws GuacamoleException {

        // Initialize the rootGroup to a QuickConnectionGroup with a
        // single root identifier.
        this.rootGroup = new QuickConnectionGroup(
            ROOT_IDENTIFIER,
            ROOT_IDENTIFIER
        );

        // Initialize the connection directory
        this.connectionDirectory = new QuickConnectDirectory(this.rootGroup);

        // Initialize the user to a SimpleUser with the provided username,
        // no connections, and the single root group.
        this.self = new SimpleUser(username) {

            @Override
            public ObjectPermissionSet getConnectionPermissions() throws GuacamoleException {
                return new SimpleObjectPermissionSet(connectionDirectory.getIdentifiers());
            }

            @Override
            public ObjectPermissionSet getConnectionGroupPermissions() throws GuacamoleException {
                return new SimpleObjectPermissionSet(Collections.singleton(ROOT_IDENTIFIER));
            }

        };

        // Set the authProvider to the calling authProvider object.
        this.authProvider = authProvider;

    }

    @Override
    public QuickConnectDirectory getConnectionDirectory() {
        return connectionDirectory;
    }

    @Override
    public User self() {
        return self;
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return new QuickConnectREST(connectionDirectory);
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {
        return rootGroup;
    }

}
