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

package org.apache.guacamole.vault.openbao.user;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserGroup;
import org.apache.guacamole.vault.user.VaultDirectoryService;

/**
 * OpenBao implementation of VaultDirectoryService.
 * Since OpenBao only provides secrets (not user/group/connection management),
 * all directory methods simply pass through the underlying directories unchanged.
 */
public class OpenBaoDirectoryService extends VaultDirectoryService {

    @Override
    public Directory<User> getUserDirectory(Directory<User> underlyingUserDirectory)
            throws GuacamoleException {
        // OpenBao doesn't manage users, just return the underlying directory
        return underlyingUserDirectory;
    }

    @Override
    public Directory<UserGroup> getUserGroupDirectory(Directory<UserGroup> underlyingUserGroupDirectory)
            throws GuacamoleException {
        // OpenBao doesn't manage user groups, just return the underlying directory
        return underlyingUserGroupDirectory;
    }

    @Override
    public Directory<Connection> getConnectionDirectory(Directory<Connection> underlyingConnectionDirectory)
            throws GuacamoleException {
        // OpenBao doesn't manage connections, just return the underlying directory
        return underlyingConnectionDirectory;
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory(
            Directory<ConnectionGroup> underlyingConnectionGroupDirectory) throws GuacamoleException {
        // OpenBao doesn't manage connection groups, just return the underlying directory
        return underlyingConnectionGroupDirectory;
    }

    @Override
    public Directory<ActiveConnection> getActiveConnectionDirectory(
            Directory<ActiveConnection> underlyingActiveConnectionDirectory) throws GuacamoleException {
        // OpenBao doesn't manage active connections, just return the underlying directory
        return underlyingActiveConnectionDirectory;
    }

    @Override
    public Directory<SharingProfile> getSharingProfileDirectory(
            Directory<SharingProfile> underlyingSharingProfileDirectory) throws GuacamoleException {
        // OpenBao doesn't manage sharing profiles, just return the underlying directory
        return underlyingSharingProfileDirectory;
    }
}
