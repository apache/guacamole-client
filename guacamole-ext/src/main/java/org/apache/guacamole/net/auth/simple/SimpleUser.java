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

package org.apache.guacamole.net.auth.simple;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AbstractUser;
import org.apache.guacamole.net.auth.ActivityRecord;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * An extremely basic User implementation.
 */
public class SimpleUser extends AbstractUser {

    /**
     * All connection permissions granted to this user.
     */
    private final Set<ObjectPermission> userPermissions =
            new HashSet<ObjectPermission>();

    /**
     * All connection permissions granted to this user.
     */
    private final Set<ObjectPermission> connectionPermissions =
            new HashSet<ObjectPermission>();
    
    /**
     * All connection group permissions granted to this user.
     */
    private final Set<ObjectPermission> connectionGroupPermissions =
            new HashSet<ObjectPermission>();

    /**
     * Creates a completely uninitialized SimpleUser.
     */
    public SimpleUser() {
    }

    /**
     * Creates a new SimpleUser having the given username and no permissions.
     *
     * @param username
     *     The username to assign to this SimpleUser.
     */
    public SimpleUser(String username) {

        // Set username
        setIdentifier(username);

    }

    /**
     * Adds a new READ permission to the given set of permissions for each of
     * the given identifiers.
     *
     * @param permissions
     *     The set of permissions to add READ permissions to.
     *
     * @param identifiers
     *     The identifiers which should each have a corresponding READ
     *     permission added to the given set.
     */
    private void addReadPermissions(Set<ObjectPermission> permissions,
            Collection<String> identifiers) {

        // Add a READ permission to the set for each identifier given
        for (String identifier : identifiers) {
            permissions.add(new ObjectPermission (
                ObjectPermission.Type.READ,
                identifier
            ));
        }

    }
    
    /**
     * Creates a new SimpleUser having the given username and READ access to
     * the connections and groups having the given identifiers.
     *
     * @param username
     *     The username to assign to this SimpleUser.
     *
     * @param connectionIdentifiers
     *     The identifiers of all connections this user has READ access to.
     *
     * @param connectionGroupIdentifiers
     *     The identifiers of all connection groups this user has READ access
     *     to.
     */
    public SimpleUser(String username,
            Collection<String> connectionIdentifiers,
            Collection<String> connectionGroupIdentifiers) {

        this(username);

        // Add permissions
        addReadPermissions(connectionPermissions,      connectionIdentifiers);
        addReadPermissions(connectionGroupPermissions, connectionGroupIdentifiers);

    }

    /**
     * Creates a new SimpleUser having the given username and READ access to
     * the users, connections, and groups having the given identifiers.
     *
     * @param username
     *     The username to assign to this SimpleUser.
     *
     * @param userIdentifiers
     *     The identifiers of all users this user has READ access to.
     *
     * @param connectionIdentifiers
     *     The identifiers of all connections this user has READ access to.
     *
     * @param connectionGroupIdentifiers
     *     The identifiers of all connection groups this user has READ access
     *     to.
     */
    public SimpleUser(String username,
            Collection<String> userIdentifiers,
            Collection<String> connectionIdentifiers,
            Collection<String> connectionGroupIdentifiers) {

        this(username);

        // Add permissions
        addReadPermissions(userPermissions,            userIdentifiers);
        addReadPermissions(connectionPermissions,      connectionIdentifiers);
        addReadPermissions(connectionGroupPermissions, connectionGroupIdentifiers);

    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.<String, String>emptyMap();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        // Do nothing - there are no attributes
    }

    @Override
    public Date getLastActive() {
        return null;
    }

    @Override
    public List<ActivityRecord> getHistory() throws GuacamoleException {
        return Collections.<ActivityRecord>emptyList();
    }

    @Override
    public SystemPermissionSet getSystemPermissions()
            throws GuacamoleException {
        return new SimpleSystemPermissionSet();
    }

    @Override
    public ObjectPermissionSet getConnectionPermissions()
            throws GuacamoleException {
        return new SimpleObjectPermissionSet(connectionPermissions);
    }

    @Override
    public ObjectPermissionSet getConnectionGroupPermissions()
            throws GuacamoleException {
        return new SimpleObjectPermissionSet(connectionGroupPermissions);
    }

    @Override
    public ObjectPermissionSet getUserPermissions()
            throws GuacamoleException {
        return new SimpleObjectPermissionSet(userPermissions);
    }

    @Override
    public ObjectPermissionSet getActiveConnectionPermissions()
            throws GuacamoleException {
        return new SimpleObjectPermissionSet();
    }

    @Override
    public ObjectPermissionSet getSharingProfilePermissions() {
        return new SimpleObjectPermissionSet();
    }

}
