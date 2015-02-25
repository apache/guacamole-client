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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnection;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.DirectoryObjectMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ParameterMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionModel;
import net.sourceforge.guacamole.net.auth.mysql.model.ParameterModel;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
import org.glyptodon.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connections.
 *
 * @author Michael Jumper, James Muehlner
 */
public class ConnectionService extends DirectoryObjectService<MySQLConnection, Connection, ConnectionModel> {

    /**
     * The environment of the Guacamole server.
     */
    @Inject
    private Environment environment;
    
    /**
     * Mapper for accessing connections.
     */
    @Inject
    private ConnectionMapper connectionMapper;

    /**
     * Mapper for accessing connection parameters.
     */
    @Inject
    private ParameterMapper parameterMapper;

    /**
     * Provider for creating connections.
     */
    @Inject
    private Provider<MySQLConnection> mySQLConnectionProvider;

    @Override
    protected DirectoryObjectMapper<ConnectionModel> getObjectMapper() {
        return connectionMapper;
    }

    @Override
    protected MySQLConnection getObjectInstance(AuthenticatedUser currentUser,
            ConnectionModel model) {
        MySQLConnection connection = mySQLConnectionProvider.get();
        connection.init(currentUser, model);
        return connection;
    }

    @Override
    protected ConnectionModel getModelInstance(AuthenticatedUser currentUser,
            final Connection object) {

        // Create new MySQLConnection backed by blank model
        ConnectionModel model = new ConnectionModel();
        MySQLConnection connection = getObjectInstance(currentUser, model);

        // Set model contents through MySQLConnection, copying the provided connection
        connection.setIdentifier(object.getIdentifier());
        connection.setParentIdentifier(object.getParentIdentifier());
        connection.setName(object.getName());
        connection.setConfiguration(object.getConfiguration());

        return model;
        
    }

    @Override
    protected boolean hasCreatePermission(AuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit user creation permission
        SystemPermissionSet permissionSet = user.getUser().getSystemPermissions();
        return permissionSet.hasPermission(SystemPermission.Type.CREATE_CONNECTION);

    }

    @Override
    protected ObjectPermissionSet getPermissionSet(AuthenticatedUser user)
            throws GuacamoleException {

        // Return permissions related to connections 
        return user.getUser().getConnectionPermissions();

    }

    @Override
    protected void validateNewObject(AuthenticatedUser user, Connection object)
            throws GuacamoleException {

        // Name must not be blank
        if (object.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("Connection names must not be blank.");
        
        // FIXME: Do not attempt to create duplicate connections

    }

    @Override
    protected void validateExistingObject(AuthenticatedUser user,
            MySQLConnection object) throws GuacamoleException {

        // Name must not be blank
        if (object.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("Connection names must not be blank.");
        
        // FIXME: Check whether such a connection is already present
        
    }

    /**
     * Returns the set of all identifiers for all connections within the root
     * connection group that the user has read access to.
     *
     * @param user
     *     The user retrieving the identifiers.
     *
     * @return
     *     The set of all identifiers for all connections in the root
     *     connection group that the user has read access to.
     *
     * @throws GuacamoleException
     *     If an error occurs while reading identifiers.
     */
    public Set<String> getRootIdentifiers(AuthenticatedUser user) throws GuacamoleException {

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            return connectionMapper.selectIdentifiersWithin(null);

        // Otherwise only return explicitly readable identifiers
        else
            return connectionMapper.selectReadableIdentifiersWithin(user.getUser().getModel(), null);

    }

    /**
     * Retrieves all parameters visible to the given user and associated with
     * the connection having the given identifier. If the given user has no
     * access to such parameters, or no such connection exists, the returned
     * map will be empty.
     *
     * @param user
     *     The user retrieving connection parameters.
     *
     * @param identifier
     *     The identifier of the connection whose parameters are being
     *     retrieved.
     *
     * @return
     *     A new map of all parameter name/value pairs that the given user has
     *     access to.
     */
    public Map<String, String> retrieveParameters(AuthenticatedUser user,
            String identifier) {

        // FIXME: Check permissions
        
        Map<String, String> parameterMap = new HashMap<String, String>();

        // Convert associated parameters to map
        Collection<ParameterModel> parameters = parameterMapper.select(identifier);
        for (ParameterModel parameter : parameters)
            parameterMap.put(parameter.getName(), parameter.getValue());

        return parameterMap;

    }
    
    /**
     * Connects to the given connection as the given user, using the given
     * client information. If the user does not have permission to read the
     * connection, permission will be denied.
     *
     * @param user
     *     The user connecting to the connection.
     *
     * @param connection
     *     The connection being connected to.
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
            MySQLConnection connection, GuacamoleClientInformation info)
            throws GuacamoleException {

        String identifier = connection.getIdentifier();
        
        // Connect only if READ permission is granted
        if (hasObjectPermission(user, identifier, ObjectPermission.Type.READ)) {

            // Generate configuration from available data
            GuacamoleConfiguration config = new GuacamoleConfiguration();

            // Set protocol from connection
            ConnectionModel model = connection.getModel();
            config.setProtocol(model.getProtocol());

            // Set parameters from associated data
            Collection<ParameterModel> parameters = parameterMapper.select(identifier);
            for (ParameterModel parameter : parameters)
                config.setParameter(parameter.getName(), parameter.getValue());

            // Return new socket
            return new ConfiguredGuacamoleSocket(
                new InetGuacamoleSocket(
                    environment.getRequiredProperty(Environment.GUACD_HOSTNAME),
                    environment.getRequiredProperty(Environment.GUACD_PORT)
                ),
                config
            );

        }

        // The user does not have permission to connect
        throw new GuacamoleSecurityException("Permission denied.");

    }
    
}
