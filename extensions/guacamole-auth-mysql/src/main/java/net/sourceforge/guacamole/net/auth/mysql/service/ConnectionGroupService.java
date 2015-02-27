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

package net.sourceforge.guacamole.net.auth.mysql.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnectionGroup;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionGroupMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.DirectoryObjectMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionGroupModel;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
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
public class ConnectionGroupService extends DirectoryObjectService<MySQLConnectionGroup,
        ConnectionGroup, ConnectionGroupModel> {

    /**
     * Mapper for accessing connection groups.
     */
    @Inject
    private ConnectionGroupMapper connectionGroupMapper;

    /**
     * Provider for creating connection groups.
     */
    @Inject
    private Provider<MySQLConnectionGroup> connectionGroupProvider;

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
    protected MySQLConnectionGroup getObjectInstance(AuthenticatedUser currentUser,
            ConnectionGroupModel model) {
        MySQLConnectionGroup connectionGroup = connectionGroupProvider.get();
        connectionGroup.init(currentUser, model);
        return connectionGroup;
    }

    @Override
    protected ConnectionGroupModel getModelInstance(AuthenticatedUser currentUser,
            final ConnectionGroup object) {

        // Create new MySQLConnectionGroup backed by blank model
        ConnectionGroupModel model = new ConnectionGroupModel();
        MySQLConnectionGroup connectionGroup = getObjectInstance(currentUser, model);

        // Set model contents through MySQLConnection, copying the provided connection group
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
    protected void validateNewObject(AuthenticatedUser user, ConnectionGroup object)
            throws GuacamoleException {

        // Name must not be blank
        if (object.getName().trim().isEmpty())
            throw new GuacamoleClientException("Connection group names must not be blank.");
        
        // FIXME: Do not attempt to create duplicate connection groups

    }

    @Override
    protected void validateExistingObject(AuthenticatedUser user,
            MySQLConnectionGroup object) throws GuacamoleException {

        // Name must not be blank
        if (object.getName().trim().isEmpty())
            throw new GuacamoleClientException("Connection group names must not be blank.");
        
        // FIXME: Check whether such a connection group is already present
        
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
     *     A connected GuacamoleSocket associated with a newly-established
     *     connection.
     *
     * @throws GuacamoleException
     *     If permission to connect to this connection is denied.
     */
    public GuacamoleSocket connect(AuthenticatedUser user,
            MySQLConnectionGroup connectionGroup, GuacamoleClientInformation info)
            throws GuacamoleException {

        // Connect only if READ permission is granted
        if (hasObjectPermission(user, connectionGroup.getIdentifier(), ObjectPermission.Type.READ))
            return socketService.getGuacamoleSocket(user, connectionGroup, info);

        // The user does not have permission to connect
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
