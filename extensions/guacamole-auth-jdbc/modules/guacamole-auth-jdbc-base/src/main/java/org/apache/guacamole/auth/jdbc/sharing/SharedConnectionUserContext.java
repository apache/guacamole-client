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

package org.apache.guacamole.auth.jdbc.sharing;

import com.google.inject.Inject;
import com.google.inject.Provider;
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
import org.apache.guacamole.net.auth.simple.SimpleConnectionDirectory;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroup;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroupDirectory;
import org.apache.guacamole.net.auth.simple.SimpleConnectionRecordSet;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;
import org.apache.guacamole.net.auth.simple.SimpleUser;
import org.apache.guacamole.net.auth.simple.SimpleUserDirectory;

/**
 * The user context of a SharedConnectionUser, providing access ONLY to the
 * user themselves, the single SharedConnection associated with that user, and
 * an internal root connection group containing only that single
 * SharedConnection.
 *
 * @author Michael Jumper
 */
public class SharedConnectionUserContext implements UserContext {

    /**
     * Provider for retrieving SharedConnection instances.
     */
    @Inject
    private Provider<SharedConnection> connectionProvider;

    /**
     * The AuthenticationProvider that created this SharedConnectionUserContext.
     */
    @Inject
    private AuthenticationProvider authProvider;

    /**
     * The user whose level of access is represented by this user context.
     */
    private User self;

    /**
     * A directory of all connections visible to the user for whom this user
     * context was created.
     */
    private Directory<Connection> connectionDirectory;

    /**
     * A directory of all connection groups visible to the user for whom this
     * user context was created.
     */
    private Directory<ConnectionGroup> connectionGroupDirectory;

    /**
     * A directory of all users visible to the user for whom this user context
     * was created.
     */
    private Directory<User> userDirectory;

    /**
     * The root connection group of the hierarchy containing all connections
     * and connection groups visible to the user for whom this user context was
     * created.
     */
    private ConnectionGroup rootGroup;

    /**
     * Creates a new SharedConnectionUserContext which provides access ONLY to
     * the given user, the single SharedConnection associated with that user,
     * and an internal root connection group containing only that single
     * SharedConnection.
     *
     * @param user
     *     The SharedConnectionUser for whom this SharedConnectionUserContext
     *     is being created.
     */
    public void init(SharedConnectionUser user) {

        // Get the definition of the shared connection
        SharedConnectionDefinition definition =
                user.getSharedConnectionDefinition();

        // Create a single shared connection accessible by the user
        SharedConnection connection = connectionProvider.get();
        connection.init(user, definition);

        // Build list of all accessible connection identifiers
        Collection<String> connectionIdentifiers =
                Collections.singletonList(connection.getIdentifier());

        // The connection directory should contain only the shared connection
        this.connectionDirectory = new SimpleConnectionDirectory(
                Collections.<Connection>singletonList(connection));

        // The user should have access only to the shared connection and himself
        this.self = new SimpleUser(user.getIdentifier(),
                Collections.<String>singletonList(user.getIdentifier()),
                connectionIdentifiers,
                Collections.<String>emptyList());

        // The root group contains only the shared connection
        String rootIdentifier = connection.getParentIdentifier();
        this.rootGroup = new SimpleConnectionGroup(rootIdentifier, rootIdentifier,
                connectionIdentifiers, Collections.<String>emptyList());

        // The connection group directory contains only the root group
        this.connectionGroupDirectory = new SimpleConnectionGroupDirectory(
                Collections.singletonList(this.rootGroup));

        // The user directory contains only this user
        this.userDirectory = new SimpleUserDirectory(this.self);

    }

    @Override
    public User self() {
        return self;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Directory<User> getUserDirectory() {
        return userDirectory;
    }

    @Override
    public Directory<Connection> getConnectionDirectory()
            throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory() {
        return connectionGroupDirectory;
    }

    @Override
    public Directory<ActiveConnection> getActiveConnectionDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<ActiveConnection>();
    }

    @Override
    public Directory<SharingProfile> getSharingProfileDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<SharingProfile>();
    }

    @Override
    public ConnectionRecordSet getConnectionHistory() {
        return new SimpleConnectionRecordSet();
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() {
        return rootGroup;
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
