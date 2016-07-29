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

package org.apache.guacamole.auth.jdbc.user;


import org.apache.guacamole.auth.jdbc.connectiongroup.RootConnectionGroup;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupDirectory;
import org.apache.guacamole.auth.jdbc.connection.ConnectionDirectory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Collection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.base.RestrictedObject;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionDirectory;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordSet;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileDirectory;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.User;

/**
 * UserContext implementation which is driven by an arbitrary, underlying
 * database.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class UserContext extends RestrictedObject
    implements org.apache.guacamole.net.auth.UserContext {

    /**
     * User directory restricted by the permissions of the user associated
     * with this context.
     */
    @Inject
    private UserDirectory userDirectory;
 
    /**
     * Connection directory restricted by the permissions of the user
     * associated with this context.
     */
    @Inject
    private ConnectionDirectory connectionDirectory;

    /**
     * Connection group directory restricted by the permissions of the user
     * associated with this context.
     */
    @Inject
    private ConnectionGroupDirectory connectionGroupDirectory;

    /**
     * Sharing profile directory restricted by the permissions of the user
     * associated with this context.
     */
    @Inject
    private SharingProfileDirectory sharingProfileDirectory;

    /**
     * ActiveConnection directory restricted by the permissions of the user
     * associated with this context.
     */
    @Inject
    private ActiveConnectionDirectory activeConnectionDirectory;

    /**
     * Provider for creating the root group.
     */
    @Inject
    private Provider<RootConnectionGroup> rootGroupProvider;

    /**
     * Provider for creating connection record sets.
     */
    @Inject
    private Provider<ConnectionRecordSet> connectionRecordSetProvider;
    
    @Override
    public void init(AuthenticatedUser currentUser) {

        super.init(currentUser);
        
        // Init directories
        userDirectory.init(currentUser);
        connectionDirectory.init(currentUser);
        connectionGroupDirectory.init(currentUser);
        sharingProfileDirectory.init(currentUser);
        activeConnectionDirectory.init(currentUser);

    }

    @Override
    public User self() {
        return getCurrentUser().getUser();
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return getCurrentUser().getModelAuthenticationProvider();
    }

    @Override
    public Directory<User> getUserDirectory() throws GuacamoleException {
        return userDirectory;
    }

    @Override
    public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory() throws GuacamoleException {
        return connectionGroupDirectory;
    }

    @Override
    public Directory<SharingProfile> getSharingProfileDirectory()
            throws GuacamoleException {
        return sharingProfileDirectory;
    }

    @Override
    public Directory<ActiveConnection> getActiveConnectionDirectory()
            throws GuacamoleException {
        return activeConnectionDirectory;
    }

    @Override
    public ConnectionRecordSet getConnectionHistory()
            throws GuacamoleException {
        ConnectionRecordSet connectionRecordSet = connectionRecordSetProvider.get();
        connectionRecordSet.init(getCurrentUser());
        return connectionRecordSet;
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {

        // Build and return a root group for the current user
        RootConnectionGroup rootGroup = rootGroupProvider.get();
        rootGroup.init(getCurrentUser());
        return rootGroup;

    }

    @Override
    public Collection<Form> getUserAttributes() {
        return ModeledUser.ATTRIBUTES;
    }

    @Override
    public Collection<Form> getConnectionAttributes() {
        return ModeledConnection.ATTRIBUTES;
    }

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        return ModeledConnectionGroup.ATTRIBUTES;
    }

    @Override
    public Collection<Form> getSharingProfileAttributes() {
        return ModeledSharingProfile.ATTRIBUTES;
    }

}
