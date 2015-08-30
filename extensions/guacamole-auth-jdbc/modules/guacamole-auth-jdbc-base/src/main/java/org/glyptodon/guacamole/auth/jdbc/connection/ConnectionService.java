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

package org.glyptodon.guacamole.auth.jdbc.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.base.ModeledDirectoryObjectMapper;
import org.glyptodon.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.auth.jdbc.base.ModeledGroupedDirectoryObjectService;
import org.glyptodon.guacamole.auth.jdbc.permission.ConnectionPermissionMapper;
import org.glyptodon.guacamole.auth.jdbc.permission.ObjectPermissionMapper;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connections.
 *
 * @author Michael Jumper, James Muehlner
 */
public class ConnectionService extends ModeledGroupedDirectoryObjectService<ModeledConnection, Connection, ConnectionModel> {

    /**
     * Mapper for accessing connections.
     */
    @Inject
    private ConnectionMapper connectionMapper;

    /**
     * Mapper for manipulating connection permissions.
     */
    @Inject
    private ConnectionPermissionMapper connectionPermissionMapper;
    
    /**
     * Mapper for accessing connection parameters.
     */
    @Inject
    private ParameterMapper parameterMapper;

    /**
     * Mapper for accessing connection history.
     */
    @Inject
    private ConnectionRecordMapper connectionRecordMapper;

    /**
     * Provider for creating connections.
     */
    @Inject
    private Provider<ModeledConnection> connectionProvider;

    /**
     * Service for creating and tracking tunnels.
     */
    @Inject
    private GuacamoleTunnelService tunnelService;
    
    @Override
    protected ModeledDirectoryObjectMapper<ConnectionModel> getObjectMapper() {
        return connectionMapper;
    }

    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return connectionPermissionMapper;
    }

    @Override
    protected ModeledConnection getObjectInstance(AuthenticatedUser currentUser,
            ConnectionModel model) {
        ModeledConnection connection = connectionProvider.get();
        connection.init(currentUser, model);
        return connection;
    }

    @Override
    protected ConnectionModel getModelInstance(AuthenticatedUser currentUser,
            final Connection object) {

        // Create new ModeledConnection backed by blank model
        ConnectionModel model = new ConnectionModel();
        ModeledConnection connection = getObjectInstance(currentUser, model);

        // Set model contents through ModeledConnection, copying the provided connection
        connection.setParentIdentifier(object.getParentIdentifier());
        connection.setName(object.getName());
        connection.setConfiguration(object.getConfiguration());
        connection.setAttributes(object.getAttributes());

        return model;
        
    }

    @Override
    protected boolean hasCreatePermission(AuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit connection creation permission
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
    protected void beforeCreate(AuthenticatedUser user,
            ConnectionModel model) throws GuacamoleException {

        super.beforeCreate(user, model);
        
        // Name must not be blank
        if (model.getName() == null || model.getName().trim().isEmpty())
            throw new GuacamoleClientException("Connection names must not be blank.");

        // Do not attempt to create duplicate connections
        ConnectionModel existing = connectionMapper.selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null)
            throw new GuacamoleClientException("The connection \"" + model.getName() + "\" already exists.");

    }

    @Override
    protected void beforeUpdate(AuthenticatedUser user,
            ConnectionModel model) throws GuacamoleException {

        super.beforeUpdate(user, model);
        
        // Name must not be blank
        if (model.getName() == null || model.getName().trim().isEmpty())
            throw new GuacamoleClientException("Connection names must not be blank.");
        
        // Check whether such a connection is already present
        ConnectionModel existing = connectionMapper.selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null) {

            // If the specified name matches a DIFFERENT existing connection, the update cannot continue
            if (!existing.getObjectID().equals(model.getObjectID()))
                throw new GuacamoleClientException("The connection \"" + model.getName() + "\" already exists.");

        }

    }

    /**
     * Given an arbitrary Guacamole connection, produces a collection of
     * parameter model objects containing the name/value pairs of that
     * connection's parameters.
     *
     * @param connection
     *     The connection whose configuration should be used to produce the
     *     collection of parameter models.
     *
     * @return
     *     A collection of parameter models containing the name/value pairs
     *     of the given connection's parameters.
     */
    private Collection<ParameterModel> getParameterModels(ModeledConnection connection) {

        Map<String, String> parameters = connection.getConfiguration().getParameters();
        
        // Convert parameters to model objects
        Collection<ParameterModel> parameterModels = new ArrayList<ParameterModel>(parameters.size());
        for (Map.Entry<String, String> parameterEntry : parameters.entrySet()) {

            // Get parameter name and value
            String name = parameterEntry.getKey();
            String value = parameterEntry.getValue();

            // There is no need to insert empty parameters
            if (value == null || value.isEmpty())
                continue;
            
            // Produce model object from parameter
            ParameterModel model = new ParameterModel();
            model.setConnectionIdentifier(connection.getIdentifier());
            model.setName(name);
            model.setValue(value);

            // Add model to list
            parameterModels.add(model);
            
        }

        return parameterModels;

    }

    @Override
    public ModeledConnection createObject(AuthenticatedUser user, Connection object)
            throws GuacamoleException {

        // Create connection
        ModeledConnection connection = super.createObject(user, object);
        connection.setConfiguration(object.getConfiguration());

        // Insert new parameters, if any
        Collection<ParameterModel> parameterModels = getParameterModels(connection);
        if (!parameterModels.isEmpty())
            parameterMapper.insert(parameterModels);

        return connection;

    }
    
    @Override
    public void updateObject(AuthenticatedUser user, ModeledConnection object)
            throws GuacamoleException {

        // Update connection
        super.updateObject(user, object);

        // Replace existing parameters with new parameters, if any
        Collection<ParameterModel> parameterModels = getParameterModels(object);
        parameterMapper.delete(object.getIdentifier());
        if (!parameterModels.isEmpty())
            parameterMapper.insert(parameterModels);
        
    }

    /**
     * Returns the set of all identifiers for all connections within the
     * connection group having the given identifier. Only connections that the
     * user has read access to will be returned.
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
     *     The set of all identifiers for all connections in the connection
     *     group having the given identifier that the user has read access to.
     *
     * @throws GuacamoleException
     *     If an error occurs while reading identifiers.
     */
    public Set<String> getIdentifiersWithin(AuthenticatedUser user,
            String identifier)
            throws GuacamoleException {

        // Bypass permission checks if the user is a system admin
        if (user.getUser().isAdministrator())
            return connectionMapper.selectIdentifiersWithin(identifier);

        // Otherwise only return explicitly readable identifiers
        else
            return connectionMapper.selectReadableIdentifiersWithin(user.getUser().getModel(), identifier);

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

        Map<String, String> parameterMap = new HashMap<String, String>();

        // Determine whether we have permission to read parameters
        boolean canRetrieveParameters;
        try {
            canRetrieveParameters = hasObjectPermission(user, identifier,
                    ObjectPermission.Type.UPDATE);
        }

        // Provide empty (but mutable) map if unable to check permissions
        catch (GuacamoleException e) {
            return parameterMap;
        }

        // Populate parameter map if we have permission to do so
        if (canRetrieveParameters) {
            for (ParameterModel parameter : parameterMapper.select(identifier))
                parameterMap.put(parameter.getName(), parameter.getValue());
        }

        return parameterMap;

    }

    /**
     * Retrieves the connection history of the given connection, including any
     * active connections.
     *
     * @param user
     *     The user retrieving the connection history.
     *
     * @param connection
     *     The connection whose history is being retrieved.
     *
     * @return
     *     The connection history of the given connection, including any
     *     active connections.
     *
     * @throws GuacamoleException
     *     If permission to read the connection history is denied.
     */
    public List<ConnectionRecord> retrieveHistory(AuthenticatedUser user,
            ModeledConnection connection) throws GuacamoleException {

        String identifier = connection.getIdentifier();
        
        // Retrieve history only if READ permission is granted
        if (hasObjectPermission(user, identifier, ObjectPermission.Type.READ)) {

            // Retrieve history
            List<ConnectionRecordModel> models = connectionRecordMapper.select(identifier);

            // Get currently-active connections
            List<ConnectionRecord> records = new ArrayList<ConnectionRecord>(tunnelService.getActiveConnections(connection));
            Collections.reverse(records);

            // Add past connections from model objects
            for (ConnectionRecordModel model : models)
                records.add(new ModeledConnectionRecord(model));

            // Return converted history list
            return records;
            
        }
        
        // The user does not have permission to read the history
        throw new GuacamoleSecurityException("Permission denied.");

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
     *     A connected GuacamoleTunnel associated with a newly-established
     *     connection.
     *
     * @throws GuacamoleException
     *     If permission to connect to this connection is denied.
     */
    public GuacamoleTunnel connect(AuthenticatedUser user,
            ModeledConnection connection, GuacamoleClientInformation info)
            throws GuacamoleException {

        // Connect only if READ permission is granted
        if (hasObjectPermission(user, connection.getIdentifier(), ObjectPermission.Type.READ))
            return tunnelService.getGuacamoleTunnel(user, connection, info);

        // The user does not have permission to connect
        throw new GuacamoleSecurityException("Permission denied.");

    }
    
}
