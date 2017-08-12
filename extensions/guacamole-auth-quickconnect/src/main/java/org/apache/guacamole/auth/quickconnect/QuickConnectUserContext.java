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
import java.util.Collections;
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
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroupDirectory;
import org.apache.guacamole.net.auth.simple.SimpleConnectionRecordSet;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;
import org.apache.guacamole.net.auth.simple.SimpleUser;
import org.apache.guacamole.net.auth.simple.SimpleUserDirectory;

/**
 * A simple implementation of UserContext to support the QuickConnect
 * extension, primarily used for storing connections the user has
 * created using the QuickConnect bar in the webapp.
 */
public class QuickConnectUserContext implements UserContext {

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
     * Construct a QuickConnectUserContext using the authProvider and
     * the username.
     *
     * @param authProvider
     *     The authentication provider module instantiating this
     *     this class.
     * @param username
     *     The name of the user logging in and using this class.
     */
    public QuickConnectUserContext(AuthenticationProvider authProvider,
            String username) {

        // Initialize the rootGroup to a basic connection group with a
        // single root identifier.
        this.rootGroup = new QuickConnectConnectionGroup(
            ROOT_IDENTIFIER, ROOT_IDENTIFIER
        );

        // Initialize the user to a SimpleUser with the username, no
        // preexisting connections, and the single root group.
        this.self = new SimpleUser(username,
            Collections.<String>emptyList(),
            Collections.singleton(ROOT_IDENTIFIER)
        );

        // Initialize each of the directories associated with the userContext.
        this.userDirectory = new SimpleUserDirectory(self);
        this.connectionDirectory = new QuickConnectDirectory(Collections.<Connection>emptyList(), this.rootGroup);
        this.connectionGroupDirectory = new SimpleConnectionGroupDirectory(Collections.singleton(this.rootGroup));

        // Set the authProvider to the calling authProvider object.
        this.authProvider = authProvider;

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
        return userDirectory;
    }

    @Override
    public Directory<Connection> getConnectionDirectory()
            throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory()
            throws GuacamoleException {
        return connectionGroupDirectory;
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {
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
