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

package org.apache.guacamole.auth.common.user;

import java.util.Collection;
import java.util.Date;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.activeconnection.ActiveConnectionDirectory;
import org.apache.guacamole.auth.common.base.ActivityRecordModelInterface;
import org.apache.guacamole.auth.common.base.RestrictedObject;
import org.apache.guacamole.auth.common.connection.ConnectionDirectory;
import org.apache.guacamole.auth.common.connection.ConnectionRecordSet;
import org.apache.guacamole.auth.common.connection.ModeledConnection;
import org.apache.guacamole.auth.common.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.common.connectiongroup.RootConnectionGroup;
import org.apache.guacamole.auth.common.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.common.sharingprofile.SharingProfileDirectoryInterface;
import org.apache.guacamole.auth.common.usergroup.ModeledUserGroup;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.ActivityRecord;
import org.apache.guacamole.net.auth.ActivityRecordSet;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserGroup;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * UserContext implementation which is driven by an arbitrary, underlying
 * database.
 */
@SuppressWarnings("unchecked")
public abstract class ModeledUserContextAbstract extends RestrictedObject
        implements org.apache.guacamole.net.auth.UserContext {

    /**
     * User directory restricted by the permissions of the user associated with
     * this context.
     */
    @Inject
    protected Directory<User> userDirectory;

    /**
     * User group directory restricted by the permissions of the user associated
     * with this context.
     */
    @Inject
    protected Directory<UserGroup> userGroupDirectory;

    /**
     * Connection group directory restricted by the permissions of the user
     * associated with this context.
     */
    @Inject
    protected Directory<ConnectionGroup> connectionGroupDirectory;

    /**
     * Connection directory restricted by the permissions of the user associated
     * with this context.
     */
    @Inject
    protected ConnectionDirectory connectionDirectory;

    /**
     * Sharing profile directory restricted by the permissions of the user
     * associated with this context.
     */
    @Inject
    protected SharingProfileDirectoryInterface sharingProfileDirectory;

    /**
     * ActiveConnection directory restricted by the permissions of the user
     * associated with this context.
     */
    @Inject
    protected ActiveConnectionDirectory activeConnectionDirectory;

    /**
     * Provider for creating the root group.
     */
    @Inject
    protected Provider<RootConnectionGroup> rootGroupProvider;

    /**
     * Provider for creating connection record sets.
     */
    @Inject
    protected Provider<ConnectionRecordSet> connectionRecordSetProvider;

    /**
     * Provider for creating user record sets.
     */
    @Inject
    protected Provider<UserRecordSet> userRecordSetProvider;

    /**
     * Mapper for user login records.
     */
    @Inject
    protected UserRecordMapperInterface userRecordMapper;

    /**
     * The activity record associated with this user's Guacamole session.
     */
    protected ActivityRecordModelInterface userRecord;

    @Override
    public User self() {
        return getCurrentUser().getUser();
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return null;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return getCurrentUser().getModelAuthenticationProvider();
    }

    @Override
    public Directory<User> getUserDirectory() throws GuacamoleException {
        return (Directory<User>) userDirectory;
    }

    @Override
    public Directory<UserGroup> getUserGroupDirectory()
            throws GuacamoleException {
        return (Directory<UserGroup>) userGroupDirectory;
    }

    @Override
    public Directory<Connection> getConnectionDirectory()
            throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory()
            throws GuacamoleException {
        return (Directory<ConnectionGroup>) connectionGroupDirectory;
    }

    @Override
    public Directory<SharingProfile> getSharingProfileDirectory()
            throws GuacamoleException {
        return (Directory<SharingProfile>) sharingProfileDirectory;
    }

    @Override
    public Directory<ActiveConnection> getActiveConnectionDirectory()
            throws GuacamoleException {
        return activeConnectionDirectory;
    }

    @Override
    public ConnectionRecordSet getConnectionHistory()
            throws GuacamoleException {
        ConnectionRecordSet connectionRecordSet = connectionRecordSetProvider
                .get();
        connectionRecordSet.init(getCurrentUser());
        return connectionRecordSet;
    }

    @Override
    public ActivityRecordSet<ActivityRecord> getUserHistory()
            throws GuacamoleException {
        UserRecordSet userRecordSet = userRecordSetProvider.get();
        userRecordSet.init(getCurrentUser());
        return userRecordSet;
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
        return ModeledUserAbstract.ATTRIBUTES;
    }

    @Override
    public Collection<Form> getUserGroupAttributes() {
        return ModeledUserGroup.ATTRIBUTES;
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

    @Override
    public void invalidate() {

        // Record logout time
        userRecord.setEndDate(new Date());
        userRecordMapper.update(userRecord);

    }

}
