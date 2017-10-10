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

package org.apache.guacamole.auth.ldap.user;

import com.google.inject.Inject;
import com.novell.ldap.LDAPConnection;
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.auth.ldap.LDAPAuthenticationProvider;
import org.apache.guacamole.auth.ldap.connection.ConnectionService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.ActivityRecord;
import org.apache.guacamole.net.auth.ActivityRecordSet;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.simple.SimpleActivityRecordSet;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroup;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroupDirectory;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;
import org.apache.guacamole.net.auth.simple.SimpleUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An LDAP-specific implementation of UserContext which queries all Guacamole
 * connections and users from the LDAP directory.
 */
public class UserContext implements org.apache.guacamole.net.auth.UserContext {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(UserContext.class);

    /**
     * Service for retrieving Guacamole connections from the LDAP server.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * Service for retrieving Guacamole users from the LDAP server.
     */
    @Inject
    private UserService userService;

    /**
     * Reference to the AuthenticationProvider associated with this
     * UserContext.
     */
    @Inject
    private AuthenticationProvider authProvider;

    /**
     * Reference to a User object representing the user whose access level
     * dictates the users and connections visible through this UserContext.
     */
    private User self;

    /**
     * Directory containing all User objects accessible to the user associated
     * with this UserContext.
     */
    private Directory<User> userDirectory;

    /**
     * Directory containing all Connection objects accessible to the user
     * associated with this UserContext.
     */
    private Directory<Connection> connectionDirectory;

    /**
     * Directory containing all ConnectionGroup objects accessible to the user
     * associated with this UserContext.
     */
    private Directory<ConnectionGroup> connectionGroupDirectory;

    /**
     * Reference to the root connection group.
     */
    private ConnectionGroup rootGroup;

    /**
     * Initializes this UserContext using the provided AuthenticatedUser and
     * LDAPConnection.
     *
     * @param user
     *     The AuthenticatedUser representing the user that authenticated. This
     *     user may have been authenticated by a different authentication
     *     provider (not LDAP).
     *
     * @param ldapConnection
     *     The connection to the LDAP server to use when querying accessible
     *     Guacamole users and connections.
     *
     * @throws GuacamoleException
     *     If associated data stored within the LDAP directory cannot be
     *     queried due to an error.
     */
    public void init(AuthenticatedUser user, LDAPConnection ldapConnection)
            throws GuacamoleException {

        // Query all accessible users
        userDirectory = new SimpleDirectory<User>(
            userService.getUsers(ldapConnection)
        );

        // Query all accessible connections
        connectionDirectory = new SimpleDirectory<Connection>(
            connectionService.getConnections(user, ldapConnection)
        );

        // Root group contains only connections
        rootGroup = new SimpleConnectionGroup(
            LDAPAuthenticationProvider.ROOT_CONNECTION_GROUP,
            LDAPAuthenticationProvider.ROOT_CONNECTION_GROUP,
            connectionDirectory.getIdentifiers(),
            Collections.<String>emptyList()
        );

        // Expose only the root group in the connection group directory
        connectionGroupDirectory = new SimpleConnectionGroupDirectory(Collections.singleton(rootGroup));

        // Init self with basic permissions
        self = new SimpleUser(
            user.getIdentifier(),
            userDirectory.getIdentifiers(),
            connectionDirectory.getIdentifiers(),
            connectionGroupDirectory.getIdentifiers()
        );

    }

    @Override
    public User self() {
        return self;
    }

    @Override
    public String getResource() {
        return null;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Directory<User> getUserDirectory() throws GuacamoleException {
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
    public ActivityRecordSet<ConnectionRecord> getConnectionHistory()
            throws GuacamoleException {
        return new SimpleActivityRecordSet<ConnectionRecord>();
    }

    @Override
    public ActivityRecordSet<ActivityRecord> getUserHistory()
            throws GuacamoleException {
        return new SimpleActivityRecordSet<ActivityRecord>();
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

    @Override
    public void invalidate() {
        // Nothing to invalidate
    }

}
