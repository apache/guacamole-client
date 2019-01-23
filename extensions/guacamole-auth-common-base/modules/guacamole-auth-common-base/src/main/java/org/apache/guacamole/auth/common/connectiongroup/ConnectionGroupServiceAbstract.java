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

package org.apache.guacamole.auth.common.connectiongroup;

import java.util.Map;
import java.util.Set;

import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.auth.common.base.ModeledChildDirectoryObjectServiceAbstract;
import org.apache.guacamole.auth.common.base.ModeledDirectoryObjectMapperInterface;
import org.apache.guacamole.auth.common.permission.CommonPermissionMapperInterface;
import org.apache.guacamole.auth.common.permission.ConnectionGroupPermissionMapperInterface;
import org.apache.guacamole.auth.common.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;
import org.apache.guacamole.protocol.GuacamoleClientInformation;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connection groups.
 */
public abstract class ConnectionGroupServiceAbstract extends
        ModeledChildDirectoryObjectServiceAbstract<ModeledConnectionGroup, ConnectionGroup, ConnectionGroupModelInterface> {

    /**
     * Mapper for accessing connection groups.
     */
    @Inject
    private ConnectionGroupMapperInterface connectionGroupMapper;

    /**
     * Mapper for manipulating connection group permissions.
     */
    @Inject
    private ConnectionGroupPermissionMapperInterface connectionGroupPermissionMapper;

    /**
     * Provider for creating connection groups.
     */
    @Inject
    private Provider<ModeledConnectionGroup> connectionGroupProvider;

    /**
     * Service for creating and tracking tunnels.
     */
    @Inject
    private GuacamoleTunnelService tunnelService;

    @SuppressWarnings("unchecked")
	@Override
    protected ModeledDirectoryObjectMapperInterface<ConnectionGroupModelInterface> getObjectMapper() {
        return (ModeledDirectoryObjectMapperInterface<ConnectionGroupModelInterface>) connectionGroupMapper;
    }

    @Override
    protected CommonPermissionMapperInterface getPermissionMapper() {
        return connectionGroupPermissionMapper;
    }

    @Override
    protected ModeledConnectionGroup getObjectInstance(
            ModeledAuthenticatedUser currentUser, ConnectionGroupModelInterface model) {
        ModeledConnectionGroup connectionGroup = connectionGroupProvider.get();
        connectionGroup.init(currentUser, model);
        return connectionGroup;
    }

    @Override
    protected boolean hasCreatePermission(ModeledAuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit connection group creation permission
        SystemPermissionSet permissionSet = user.getUser()
                .getSystemPermissions();
        return permissionSet
                .hasPermission(SystemPermission.Type.CREATE_CONNECTION_GROUP);

    }

    @Override
    protected ObjectPermissionSet getEffectivePermissionSet(
            ModeledAuthenticatedUser user) throws GuacamoleException {

        // Return permissions related to connection groups
        return user.getUser().getConnectionGroupPermissions();

    }

    @Override
    protected ObjectPermissionSet getParentEffectivePermissionSet(
            ModeledAuthenticatedUser user) throws GuacamoleException {

        // Connection groups are contained by other connection groups
        return user.getUser().getConnectionGroupPermissions();

    }

    @Override
    protected void beforeCreate(ModeledAuthenticatedUser user,
            ConnectionGroup object, ConnectionGroupModelInterface model)
            throws GuacamoleException {

        super.beforeCreate(user, object, model);

        // Name must not be blank
        if (model.getName() == null || model.getName().trim().isEmpty())
            throw new GuacamoleClientException(
                    "Connection group names must not be blank.");

        // Do not attempt to create duplicate connection groups
        ConnectionGroupModelInterface existing = connectionGroupMapper
                .selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null)
            throw new GuacamoleClientException("The connection group \""
                    + model.getName() + "\" already exists.");

    }

    @Override
    protected void beforeUpdate(ModeledAuthenticatedUser user,
            ModeledConnectionGroup object, ConnectionGroupModelInterface model)
            throws GuacamoleException {

        super.beforeUpdate(user, object, model);

        // Name must not be blank
        if (model.getName() == null || model.getName().trim().isEmpty())
            throw new GuacamoleClientException(
                    "Connection group names must not be blank.");

        // Check whether such a connection group is already present
        ConnectionGroupModelInterface existing = connectionGroupMapper
                .selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null) {

            // If the specified name matches a DIFFERENT existing connection
            // group, the update cannot continue
            if (!existing.getObjectID().equals(model.getObjectID()))
                throw new GuacamoleClientException("The connection group \""
                        + model.getName() + "\" already exists.");

        }

        // Verify that this connection group's location does not create a cycle
        String relativeParentIdentifier = model.getParentIdentifier();
        while (relativeParentIdentifier != null) {

            // Abort if cycle is detected
            if (relativeParentIdentifier.equals(model.getIdentifier()))
                throw new GuacamoleUnsupportedException(
                        "A connection group may not contain itself.");

            // Advance to next parent
            ModeledConnectionGroup relativeParentGroup = retrieveObject(user,
                    relativeParentIdentifier);
            relativeParentIdentifier = relativeParentGroup.getModel()
                    .getParentIdentifier();

        }

    }

    /**
     * Returns the set of all identifiers for all connection groups within the
     * connection group having the given identifier. Only connection groups that
     * the user has read access to will be returned.
     * 
     * Permission to read the connection group having the given identifier is
     * NOT checked.
     *
     * @param user
     *            The user retrieving the identifiers.
     * 
     * @param identifier
     *            The identifier of the parent connection group, or null to
     *            check the root connection group.
     *
     * @return The set of all identifiers for all connection groups in the
     *         connection group having the given identifier that the user has
     *         read access to.
     *
     * @throws GuacamoleException
     *             If an error occurs while reading identifiers.
     */
    public Set<String> getIdentifiersWithin(ModeledAuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            return connectionGroupMapper.selectIdentifiersWithin(identifier);

        // Otherwise only return explicitly readable identifiers
        else
            return connectionGroupMapper.selectReadableIdentifiersWithin(
                    user.getUser().getModel(), identifier,
                    user.getEffectiveUserGroups());

    }

    /**
     * Connects to the given connection group as the given user, using the given
     * client information. If the user does not have permission to read the
     * connection group, permission will be denied.
     *
     * @param user
     *            The user connecting to the connection group.
     *
     * @param connectionGroup
     *            The connectionGroup being connected to.
     *
     * @param info
     *            Information associated with the connecting client.
     *
     * @return A connected GuacamoleTunnel associated with a newly-established
     *         connection.
     *
     * @throws GuacamoleException
     *             If permission to connect to this connection is denied.
     */
    public GuacamoleTunnel connect(ModeledAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup, GuacamoleClientInformation info,
            Map<String, String> tokens) throws GuacamoleException {

        // Connect only if READ permission is granted
        if (hasObjectPermission(user, connectionGroup.getIdentifier(),
                ObjectPermission.Type.READ))
            return tunnelService.getGuacamoleTunnel(user, connectionGroup, info, tokens);

        // The user does not have permission to connect
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
