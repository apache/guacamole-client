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

package org.apache.guacamole.auth.jdbc.tunnel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionModel;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordModel;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterModel;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.connection.ConnectionMapper;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.StandardTokens;
import org.apache.guacamole.token.TokenFilter;
import org.mybatis.guice.transactional.Transactional;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterMapper;


/**
 * Base implementation of the GuacamoleTunnelService, handling retrieval of
 * connection parameters, load balancing, and connection usage counts. The
 * implementation of concurrency rules is up to policy-specific subclasses.
 *
 * @author Michael Jumper
 */
public abstract class AbstractGuacamoleTunnelService implements GuacamoleTunnelService {

    /**
     * The environment of the Guacamole server.
     */
    @Inject
    private JDBCEnvironment environment;
 
    /**
     * Mapper for accessing connections.
     */
    @Inject
    private ConnectionMapper connectionMapper;

    /**
     * Provider for creating connections.
     */
    @Inject
    private Provider<ModeledConnection> connectionProvider;

    /**
     * Mapper for accessing connection parameters.
     */
    @Inject
    private ConnectionParameterMapper parameterMapper;

    /**
     * Mapper for accessing connection history.
     */
    @Inject
    private ConnectionRecordMapper connectionRecordMapper;

    /**
     * The hostname to use when connecting to guacd if no hostname is provided
     * within guacamole.properties.
     */
    private static final String DEFAULT_GUACD_HOSTNAME = "localhost";

    /**
     * The port to use when connecting to guacd if no port is provided within
     * guacamole.properties.
     */
    private static final int DEFAULT_GUACD_PORT = 4822;

    /**
     * All active connections through the tunnel having a given UUID.
     */
    private final Map<String, ActiveConnectionRecord> activeTunnels =
            new ConcurrentHashMap<String, ActiveConnectionRecord>();
    
    /**
     * All active connections to a connection having a given identifier.
     */
    private final ActiveConnectionMultimap activeConnections = new ActiveConnectionMultimap();

    /**
     * All active connections to a connection group having a given identifier.
     */
    private final ActiveConnectionMultimap activeConnectionGroups = new ActiveConnectionMultimap();

    /**
     * Acquires possibly-exclusive access to any one of the given connections
     * on behalf of the given user. If access is denied for any reason, or if
     * no connection is available, an exception is thrown.
     *
     * @param user
     *     The user acquiring access.
     *
     * @param connections
     *     The connections being accessed.
     *
     * @return
     *     The connection that has been acquired on behalf of the given user.
     *
     * @throws GuacamoleException
     *     If access is denied to the given user for any reason.
     */
    protected abstract ModeledConnection acquire(AuthenticatedUser user,
            List<ModeledConnection> connections) throws GuacamoleException;

    /**
     * Releases possibly-exclusive access to the given connection on behalf of
     * the given user. If the given user did not already have access, the
     * behavior of this function is undefined.
     *
     * @param user
     *     The user releasing access.
     *
     * @param connection
     *     The connection being released.
     */
    protected abstract void release(AuthenticatedUser user,
            ModeledConnection connection);

    /**
     * Acquires possibly-exclusive access to the given connection group on
     * behalf of the given user. If access is denied for any reason, an
     * exception is thrown.
     *
     * @param user
     *     The user acquiring access.
     *
     * @param connectionGroup
     *     The connection group being accessed.
     *
     * @throws GuacamoleException
     *     If access is denied to the given user for any reason.
     */
    protected abstract void acquire(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup) throws GuacamoleException;

    /**
     * Releases possibly-exclusive access to the given connection group on
     * behalf of the given user. If the given user did not already have access,
     * the behavior of this function is undefined.
     *
     * @param user
     *     The user releasing access.
     *
     * @param connectionGroup
     *     The connection group being released.
     */
    protected abstract void release(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup);

    /**
     * Returns a guacamole configuration containing the protocol and parameters
     * from the given connection. If tokens are used in the connection
     * parameter values, credentials from the given user will be substituted
     * appropriately.
     *
     * @param user
     *     The user whose credentials should be used if necessary.
     *
     * @param connection
     *     The connection whose protocol and parameters should be added to the
     *     returned configuration.
     *
     * @return
     *     A GuacamoleConfiguration containing the protocol and parameters from
     *     the given connection.
     */
    private GuacamoleConfiguration getGuacamoleConfiguration(AuthenticatedUser user,
            ModeledConnection connection) {

        // Generate configuration from available data
        GuacamoleConfiguration config = new GuacamoleConfiguration();

        // Set protocol from connection
        ConnectionModel model = connection.getModel();
        config.setProtocol(model.getProtocol());

        // Set parameters from associated data
        Collection<ConnectionParameterModel> parameters = parameterMapper.select(connection.getIdentifier());
        for (ConnectionParameterModel parameter : parameters)
            config.setParameter(parameter.getName(), parameter.getValue());

        // Build token filter containing credential tokens
        TokenFilter tokenFilter = new TokenFilter();
        StandardTokens.addStandardTokens(tokenFilter, user.getCredentials());

        // Filter the configuration
        tokenFilter.filterValues(config.getParameters());

        return config;
        
    }

    /**
     * Saves the given ActiveConnectionRecord to the database. The end date of
     * the saved record will be populated with the current time.
     *
     * @param record
     *     The record to save.
     */
    private void saveConnectionRecord(ActiveConnectionRecord record) {

        // Get associated connection
        ModeledConnection connection = record.getConnection();
        
        // Get associated models
        AuthenticatedUser user = record.getUser();
        ConnectionRecordModel recordModel = new ConnectionRecordModel();

        // Copy user information and timestamps into new record
        recordModel.setUsername(user.getIdentifier());
        recordModel.setConnectionIdentifier(connection.getIdentifier());
        recordModel.setConnectionName(connection.getName());
        recordModel.setStartDate(record.getStartDate());
        recordModel.setEndDate(new Date());

        // Insert connection record
        connectionRecordMapper.insert(recordModel);

    }

    /**
     * Returns an unconfigured GuacamoleSocket that is already connected to
     * guacd as specified in guacamole.properties, using SSL if necessary.
     *
     * @return
     *     An unconfigured GuacamoleSocket, already connected to guacd.
     *
     * @throws GuacamoleException 
     *     If an error occurs while connecting to guacd, or while parsing
     *     guacd-related properties.
     */
    private GuacamoleSocket getUnconfiguredGuacamoleSocket(Runnable socketClosedCallback)
        throws GuacamoleException {

        // Use SSL if requested
        if (environment.getProperty(Environment.GUACD_SSL, false))
            return new ManagedSSLGuacamoleSocket(
                environment.getProperty(Environment.GUACD_HOSTNAME, DEFAULT_GUACD_HOSTNAME),
                environment.getProperty(Environment.GUACD_PORT,     DEFAULT_GUACD_PORT),
                socketClosedCallback
            );

        // Otherwise, just use straight TCP
        return new ManagedInetGuacamoleSocket(
            environment.getProperty(Environment.GUACD_HOSTNAME, DEFAULT_GUACD_HOSTNAME),
            environment.getProperty(Environment.GUACD_PORT,     DEFAULT_GUACD_PORT),
            socketClosedCallback
        );

    }

    /**
     * Task which handles cleanup of a connection associated with some given
     * ActiveConnectionRecord.
     */
    private class ConnectionCleanupTask implements Runnable {

        /**
         * Whether this task has run.
         */
        private final AtomicBoolean hasRun = new AtomicBoolean(false);

        /**
         * The ActiveConnectionRecord whose connection will be cleaned up once
         * this task runs.
         */
        private final ActiveConnectionRecord activeConnection;

        /**
         * Creates a new task which automatically cleans up after the
         * connection associated with the given ActiveConnectionRecord. The
         * connection and parent group will be removed from the maps of active
         * connections and groups, and exclusive access will be released.
         *
         * @param activeConnection
         *     The ActiveConnectionRecord whose associated connection should be
         *     cleaned up once this task runs.
         */
        public ConnectionCleanupTask(ActiveConnectionRecord activeConnection) {
            this.activeConnection = activeConnection;
        }
        
        @Override
        public void run() {

            // Only run once
            if (!hasRun.compareAndSet(false, true))
                return;

            // Get original user and connection
            AuthenticatedUser user = activeConnection.getUser();
            ModeledConnection connection = activeConnection.getConnection();

            // Get associated identifiers
            String identifier = connection.getIdentifier();
            String parentIdentifier = connection.getParentIdentifier();

            // Release connection
            activeTunnels.remove(activeConnection.getUUID().toString());
            activeConnections.remove(identifier, activeConnection);
            activeConnectionGroups.remove(parentIdentifier, activeConnection);
            release(user, connection);

            // Release any associated group
            if (activeConnection.hasBalancingGroup())
                release(user, activeConnection.getBalancingGroup());
            
            // Save history record to database
            saveConnectionRecord(activeConnection);

        }

    }

    /**
     * Creates a tunnel for the given user which connects to the given
     * connection, which MUST already be acquired via acquire(). The given
     * client information will be passed to guacd when the connection is
     * established.
     * 
     * The connection will be automatically released when it closes, or if it
     * fails to establish entirely.
     *
     * @param activeConnection
     *     The active connection record of the connection in use.
     *
     * @param info
     *     Information describing the Guacamole client connecting to the given
     *     connection.
     *
     * @return
     *     A new GuacamoleTunnel which is configured and connected to the given
     *     connection.
     *
     * @throws GuacamoleException
     *     If an error occurs while the connection is being established, or
     *     while connection configuration information is being retrieved.
     */
    private GuacamoleTunnel assignGuacamoleTunnel(ActiveConnectionRecord activeConnection,
            GuacamoleClientInformation info)
            throws GuacamoleException {

        ModeledConnection connection = activeConnection.getConnection();
        
        // Record new active connection
        Runnable cleanupTask = new ConnectionCleanupTask(activeConnection);
        activeTunnels.put(activeConnection.getUUID().toString(), activeConnection);
        activeConnections.put(connection.getIdentifier(), activeConnection);
        activeConnectionGroups.put(connection.getParentIdentifier(), activeConnection);

        try {

            // Obtain socket which will automatically run the cleanup task
            GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                getUnconfiguredGuacamoleSocket(cleanupTask),
                getGuacamoleConfiguration(activeConnection.getUser(), connection),
                info
            );

            // Assign and return new tunnel 
            return activeConnection.assignGuacamoleTunnel(socket);
            
        }

        // Execute cleanup if socket could not be created
        catch (GuacamoleException e) {
            cleanupTask.run();
            throw e;
        }

    }

    /**
     * Filters the given collection of connection identifiers, returning a new
     * collection which contains only those identifiers which are preferred. If
     * no connection identifiers within the given collection are preferred, the
     * collection is left untouched.
     *
     * @param user
     *     The user whose preferred connections should be used to filter the
     *     given collection of connection identifiers.
     *
     * @param identifiers
     *     The collection of connection identifiers that should be filtered.
     *
     * @return
     *     A collection of connection identifiers containing only the subset of
     *     connection identifiers which are also preferred or, if none of the
     *     provided identifiers are preferred, the original collection of
     *     identifiers.
     */
    private Collection<String> getPreferredConnections(AuthenticatedUser user,
            Collection<String> identifiers) {

        // Search provided identifiers for any preferred connections
        for (String identifier : identifiers) {

            // If at least one prefferred connection is found, assume it is the
            // only preferred connection
            if (user.isPreferredConnection(identifier))
                return Collections.singletonList(identifier);

        }

        // No preferred connections were found
        return identifiers;

    }

    /**
     * Returns a list of all balanced connections within a given connection
     * group. If the connection group is not balancing, or it contains no
     * connections, an empty list is returned.
     *
     * @param user
     *     The user on whose behalf the balanced connections within the given
     *     connection group are being retrieved.
     *
     * @param connectionGroup
     *     The connection group to retrieve the balanced connections of.
     *
     * @return
     *     A list containing all balanced connections within the given group,
     *     or an empty list if there are no such connections.
     */
    private List<ModeledConnection> getBalancedConnections(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup) {

        // If not a balancing group, there are no balanced connections
        if (connectionGroup.getType() != ConnectionGroup.Type.BALANCING)
            return Collections.<ModeledConnection>emptyList();

        // If group has no children, there are no balanced connections
        Collection<String> identifiers = connectionMapper.selectIdentifiersWithin(connectionGroup.getIdentifier());
        if (identifiers.isEmpty())
            return Collections.<ModeledConnection>emptyList();

        // Restrict to preferred connections if session affinity is enabled
        if (connectionGroup.isSessionAffinityEnabled())
            identifiers = getPreferredConnections(user, identifiers);

        // Retrieve all children
        Collection<ConnectionModel> models = connectionMapper.select(identifiers);
        List<ModeledConnection> connections = new ArrayList<ModeledConnection>(models.size());

        // Convert each retrieved model to a modeled connection
        for (ConnectionModel model : models) {
            ModeledConnection connection = connectionProvider.get();
            connection.init(user, model);
            connections.add(connection);
        }

        return connections;
        
    }

    @Override
    public Collection<ActiveConnectionRecord> getActiveConnections(AuthenticatedUser user)
        throws GuacamoleException {

        // Simply return empty list if there are no active tunnels
        Collection<ActiveConnectionRecord> records = activeTunnels.values();
        if (records.isEmpty())
            return Collections.<ActiveConnectionRecord>emptyList();

        // A system administrator can view all connections; no need to filter
        if (user.getUser().isAdministrator())
            return records;

        // Build set of all connection identifiers associated with active tunnels
        Set<String> identifiers = new HashSet<String>(records.size());
        for (ActiveConnectionRecord record : records)
            identifiers.add(record.getConnection().getIdentifier());

        // Produce collection of readable connection identifiers
        Collection<ConnectionModel> connections = connectionMapper.selectReadable(user.getUser().getModel(), identifiers);

        // Ensure set contains only identifiers of readable connections
        identifiers.clear();
        for (ConnectionModel connection : connections)
            identifiers.add(connection.getIdentifier());

        // Produce readable subset of records
        Collection<ActiveConnectionRecord> visibleRecords = new ArrayList<ActiveConnectionRecord>(records.size());
        for (ActiveConnectionRecord record : records) {
            if (identifiers.contains(record.getConnection().getIdentifier()))
                visibleRecords.add(record);
        }

        return visibleRecords;

    }

    @Override
    @Transactional
    public GuacamoleTunnel getGuacamoleTunnel(final AuthenticatedUser user,
            final ModeledConnection connection, GuacamoleClientInformation info)
            throws GuacamoleException {

        // Acquire and connect to single connection
        acquire(user, Collections.singletonList(connection));
        return assignGuacamoleTunnel(new ActiveConnectionRecord(user, connection), info);

    }

    @Override
    public Collection<ActiveConnectionRecord> getActiveConnections(Connection connection) {
        return activeConnections.get(connection.getIdentifier());
    }

    @Override
    @Transactional
    public GuacamoleTunnel getGuacamoleTunnel(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup,
            GuacamoleClientInformation info) throws GuacamoleException {

        // If group has no associated balanced connections, cannot connect
        List<ModeledConnection> connections = getBalancedConnections(user, connectionGroup);
        if (connections.isEmpty())
            throw new GuacamoleSecurityException("Permission denied.");

        // Acquire group
        acquire(user, connectionGroup);

        // Attempt to acquire to any child
        ModeledConnection connection;
        try {
            connection = acquire(user, connections);
        }

        // Ensure connection group is always released if child acquire fails
        catch (GuacamoleException e) {
            release(user, connectionGroup);
            throw e;
        }

        // If session affinity is enabled, prefer this connection going forward
        if (connectionGroup.isSessionAffinityEnabled())
            user.preferConnection(connection.getIdentifier());

        // Connect to acquired child
        return assignGuacamoleTunnel(new ActiveConnectionRecord(user, connectionGroup, connection), info);

    }

    @Override
    public Collection<ActiveConnectionRecord> getActiveConnections(ConnectionGroup connectionGroup) {

        // If not a balancing group, assume no connections
        if (connectionGroup.getType() != ConnectionGroup.Type.BALANCING)
            return Collections.<ActiveConnectionRecord>emptyList();

        return activeConnectionGroups.get(connectionGroup.getIdentifier());

    }

}
