/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.jdbc.connectiongroup;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Set;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.base.DirectoryObjectMapper;
import org.glyptodon.guacamole.auth.jdbc.socket.GuacamoleSocketService;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.GuacamoleUnsupportedException;
import org.glyptodon.guacamole.auth.jdbc.base.GroupedDirectoryObjectService;
import org.glyptodon.guacamole.auth.jdbc.permission.ConnectionGroupPermissionMapper;
import org.glyptodon.guacamole.auth.jdbc.permission.ObjectPermissionMapper;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connection groups.
 *
 * @author Michael Jumper, James Muehlner
 */
public class ConnectionGroupService extends GroupedDirectoryObjectService<ModeledConnectionGroup,
        ConnectionGroup, ConnectionGroupModel> {

    /**
     * Mapper for accessing connection groups.
     */
    @Inject
    private ConnectionGroupMapper connectionGroupMapper;

    /**
     * Mapper for manipulating connection group permissions.
     */
    @Inject
    private ConnectionGroupPermissionMapper connectionGroupPermissionMapper;
    
    /**
     * Provider for creating connection groups.
     */
    @Inject
    private Provider<ModeledConnectionGroup> connectionGroupProvider;

    /**
     * Service for creating and tracking sockets.
     */
    @Inject
    private GuacamoleSocketService socketService;
    
    @Override
    protected DirectoryObjectMapper<ConnectionGroupModel> getObjectMapper() {
        return connectionGroupMapper;
    }

    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return connectionGroupPermissionMapper;
    }

    @Override
    protected ModeledConnectionGroup getObjectInstance(AuthenticatedUser currentUser,
            ConnectionGroupModel model) {
        ModeledConnectionGroup connectionGroup = connectionGroupProvider.get();
        connectionGroup.init(currentUser, model);
        return connectionGroup;
    }

    @Override
    protected ConnectionGroupModel getModelInstance(AuthenticatedUser currentUser,
            final ConnectionGroup object) {

        // Create new ModeledConnectionGroup backed by blank model
        ConnectionGroupModel model = new ConnectionGroupModel();
        ModeledConnectionGroup connectionGroup = getObjectInstance(currentUser, model);

        // Set model contents through ModeledConnectionGroup, copying the provided connection group
        connectionGroup.setParentIdentifier(object.getParentIdentifier());
        connectionGroup.setName(object.getName());
        connectionGroup.setType(object.getType());

        return model;
        
    }

    @Override
    protected boolean hasCreatePermission(AuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit connection group creation permission
        SystemPermissionSet permissionSet = user.getUser().getSystemPermissions();
        return permissionSet.hasPermission(SystemPermission.Type.CREATE_CONNECTION_GROUP);

    }

    @Override
    protected ObjectPermissionSet getPermissionSet(AuthenticatedUser user)
            throws GuacamoleException {

        // Return permissions related to connection groups 
        return user.getUser().getConnectionGroupPermissions();

    }

    @Override
    protected void beforeCreate(AuthenticatedUser user,
            ConnectionGroupModel model) throws GuacamoleException {

        super.beforeCreate(user, model);
        
        // Name must not be blank
        if (model.getName().trim().isEmpty())
            throw new GuacamoleClientException("Connection group names must not be blank.");
        
        // Do not attempt to create duplicate connection groups
        ConnectionGroupModel existing = connectionGroupMapper.selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null)
            throw new GuacamoleClientException("The connection group \"" + model.getName() + "\" already exists.");

    }

    @Override
    protected void beforeUpdate(AuthenticatedUser user,
            ConnectionGroupModel model) throws GuacamoleException {

        super.beforeUpdate(user, model);
        
        // Name must not be blank
        if (model.getName().trim().isEmpty())
            throw new GuacamoleClientException("Connection group names must not be blank.");
        
        // Check whether such a connection group is already present
        ConnectionGroupModel existing = connectionGroupMapper.selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null) {

            // If the specified name matches a DIFFERENT existing connection group, the update cannot continue
            if (!existing.getObjectID().equals(model.getObjectID()))
                throw new GuacamoleClientException("The connection group \"" + model.getName() + "\" already exists.");

        }

        // Verify that this connection group's location does not create a cycle
        String relativeParentIdentifier = model.getParentIdentifier();
        while (relativeParentIdentifier != null) {

            // Abort if cycle is detected
            if (relativeParentIdentifier.equals(model.getIdentifier()))
                throw new GuacamoleUnsupportedException("A connection group may not contain itself.");

            // Advance to next parent
            ModeledConnectionGroup relativeParentGroup = retrieveObject(user, relativeParentIdentifier);
            relativeParentIdentifier = relativeParentGroup.getModel().getParentIdentifier();

        } 

    }

    /**
     * Returns the set of all identifiers for all connection groups within the
     * connection group having the given identifier. Only connection groups
     * that the user has read access to will be returned.
     * 
     * Permission to read the connection group having the given identifier is
     * NOT checked.
     *
     * @param user
     *     The user retrieving the identifiers.
     * 
     * @param identifier
     *     The identifier of the parent connection group, or null to check the
     *     root connection group.
     *
     * @return
     *     The set of all identifiers for all connection groups in the
     *     connection group having the given identifier that the user has read
     *     access to.
     *
     * @throws GuacamoleException
     *     If an error occurs while reading identifiers.
     */
    public Set<String> getIdentifiersWithin(AuthenticatedUser user,
            String identifier)
            throws GuacamoleException {

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            return connectionGroupMapper.selectIdentifiersWithin(identifier);

        // Otherwise only return explicitly readable identifiers
        else
            return connectionGroupMapper.selectReadableIdentifiersWithin(user.getUser().getModel(), identifier);

    }

    /**
     * Connects to the given connection group as the given user, using the
     * given client information. If the user does not have permission to read
     * the connection group, permission will be denied.
     *
     * @param user
     *     The user connecting to the connection group.
     *
     * @param connectionGroup
     *     The connectionGroup being connected to.
     *
     * @param info
     *     Information associated with the connecting client.
     *
     * @return
     *     A connected GuacamoleTunnel associated with a newly-established
     *     connection.
     *
     * @throws GuacamoleException
     *     If permission to connect to this connection is denied.
     */
    public GuacamoleTunnel connect(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup, GuacamoleClientInformation info)
            throws GuacamoleException {

        // Connect only if READ permission is granted
        if (hasObjectPermission(user, connectionGroup.getIdentifier(), ObjectPermission.Type.READ))
            return socketService.getGuacamoleTunnel(user, connectionGroup, info);

        // The user does not have permission to connect
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
