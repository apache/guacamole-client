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
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionModel;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordModel;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterModel;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceConflictException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.GuacamoleUpstreamException;
import org.apache.guacamole.auth.jdbc.connection.ConnectionMapper;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.mybatis.guice.transactional.Transactional;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterMapper;
import org.apache.guacamole.auth.jdbc.sharing.connection.SharedConnectionDefinition;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileParameterMapper;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileParameterModel;
import org.apache.guacamole.auth.jdbc.user.RemoteAuthenticatedUser;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.protocol.FailoverGuacamoleSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base implementation of the GuacamoleTunnelService, handling retrieval of
 * connection parameters, load balancing, and connection usage counts. The
 * implementation of concurrency rules is up to policy-specific subclasses.
 */
public abstract class AbstractGuacamoleTunnelService implements GuacamoleTunnelService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(AbstractGuacamoleTunnelService.class);

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
    private ConnectionParameterMapper connectionParameterMapper;

    /**
     * Mapper for accessing sharing profile parameters.
     */
    @Inject
    private SharingProfileParameterMapper sharingProfileParameterMapper;

    /**
     * Mapper for accessing connection history.
     */
    @Inject
    private ConnectionRecordMapper connectionRecordMapper;

    /**
     * Provider for creating active connection records.
     */
    @Inject
    private Provider<ActiveConnectionRecord> activeConnectionRecordProvider;

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
     * @param includeFailoverOnly
     *     Whether connections which have been designated for use in failover
     *     situations only (hot spares) may be considered.
     *
     * @return
     *     The connection that has been acquired on behalf of the given user.
     *
     * @throws GuacamoleException
     *     If access is denied to the given user for any reason.
     */
    protected abstract ModeledConnection acquire(RemoteAuthenticatedUser user,
            List<ModeledConnection> connections, boolean includeFailoverOnly)
            throws GuacamoleException;

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
    protected abstract void release(RemoteAuthenticatedUser user,
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
    protected abstract void acquire(RemoteAuthenticatedUser user,
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
    protected abstract void release(RemoteAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup);

    /**
     * Returns a guacamole configuration containing the protocol and parameters
     * from the given connection. If the ID of an active connection is
     * provided, that connection will be joined instead of starting a new
     * primary connection. If tokens are used in the connection parameter
     * values, credentials from the given user will be substituted
     * appropriately.
     *
     * @param user
     *     The user whose credentials should be used if necessary.
     *
     * @param connection
     *     The connection whose protocol and parameters should be added to the
     *     returned configuration.
     *
     * @param connectionID
     *     The ID of the active connection to be joined, as returned by guacd,
     *     or null if a new primary connection should be established.
     *
     * @return
     *     A GuacamoleConfiguration containing the protocol and parameters from
     *     the given connection.
     */
    private GuacamoleConfiguration getGuacamoleConfiguration(RemoteAuthenticatedUser user,
            ModeledConnection connection, String connectionID) {

        // Generate configuration from available data
        GuacamoleConfiguration config = new GuacamoleConfiguration();

        // Join existing active connection, if any
        if (connectionID != null)
            config.setConnectionID(connectionID);

        // Set protocol from connection if not joining an active connection
        else {
            ConnectionModel model = connection.getModel();
            config.setProtocol(model.getProtocol());
        }

        // Set parameters from associated data
        Collection<ConnectionParameterModel> parameters = connectionParameterMapper.select(connection.getIdentifier());
        for (ConnectionParameterModel parameter : parameters)
            config.setParameter(parameter.getName(), parameter.getValue());

        return config;
        
    }

    /**
     * Returns a guacamole configuration which joins the active connection
     * having the given ID, using the provided sharing profile to restrict the
     * access provided to the user accessing the shared connection. If tokens
     * are used in the connection parameter values of the sharing profile,
     * credentials from the given user will be substituted appropriately.
     *
     * @param user
     *     The user whose credentials should be used if necessary.
     *
     * @param sharingProfile
     *     The sharing profile whose associated parameters dictate the level
     *     of access granted to the user joining the connection.
     *
     * @param connectionID
     *     The ID of the connection being joined, as provided by guacd when the
     *     original connection was established, or null if a new connection
     *     should be created instead.
     *
     * @return
     *     A GuacamoleConfiguration containing the protocol and parameters from
     *     the given connection.
     */
    private GuacamoleConfiguration getGuacamoleConfiguration(RemoteAuthenticatedUser user,
            ModeledSharingProfile sharingProfile, String connectionID) {

        // Generate configuration from available data
        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setConnectionID(connectionID);

        // Set parameters from associated data
        Collection<SharingProfileParameterModel> parameters = sharingProfileParameterMapper.select(sharingProfile.getIdentifier());
        for (SharingProfileParameterModel parameter : parameters)
            config.setParameter(parameter.getName(), parameter.getValue());

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

        // Get associated models
        ConnectionRecordModel recordModel = new ConnectionRecordModel();

        // Copy user information and timestamps into new record
        recordModel.setUsername(record.getUsername());
        recordModel.setConnectionIdentifier(record.getConnectionIdentifier());
        recordModel.setConnectionName(record.getConnectionName());
        recordModel.setRemoteHost(record.getRemoteHost());
        recordModel.setSharingProfileIdentifier(record.getSharingProfileIdentifier());
        recordModel.setSharingProfileName(record.getSharingProfileName());
        recordModel.setStartDate(record.getStartDate());
        recordModel.setEndDate(new Date());

        // Insert connection record
        connectionRecordMapper.insert(recordModel);

    }

    /**
     * Returns an unconfigured GuacamoleSocket that is already connected to
     * guacd as specified in guacamole.properties, using SSL if necessary.
     *
     * @param proxyConfig
     *     The configuration information to use when connecting to guacd.
     *
     * @param socketClosedCallback
     *     The callback which should be invoked whenever the returned socket
     *     closes.
     *
     * @return
     *     An unconfigured GuacamoleSocket, already connected to guacd.
     *
     * @throws GuacamoleException 
     *     If an error occurs while connecting to guacd, or while parsing
     *     guacd-related properties.
     */
    private GuacamoleSocket getUnconfiguredGuacamoleSocket(
            GuacamoleProxyConfiguration proxyConfig,
            Runnable socketClosedCallback) throws GuacamoleException {

        // Select socket type depending on desired encryption
        switch (proxyConfig.getEncryptionMethod()) {

            // Use SSL if requested
            case SSL:
                return new ManagedSSLGuacamoleSocket(
                    proxyConfig.getHostname(),
                    proxyConfig.getPort(),
                    socketClosedCallback
                );

            // Use straight TCP if unencrypted
            case NONE:
                return new ManagedInetGuacamoleSocket(
                    proxyConfig.getHostname(),
                    proxyConfig.getPort(),
                    socketClosedCallback
                );

        }

        // Bail out if encryption method is unknown
        throw new GuacamoleServerException("Unimplemented encryption method.");

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

            // Connection can no longer be shared
            activeConnection.invalidate();

            // Remove underlying tunnel from list of active tunnels
            activeTunnels.remove(activeConnection.getUUID().toString());

            // Get original user
            RemoteAuthenticatedUser user = activeConnection.getUser();

            // Release the associated connection if this is the primary connection
            if (activeConnection.isPrimaryConnection()) {

                // Get connection and associated identifiers
                ModeledConnection connection = activeConnection.getConnection();
                String identifier = connection.getIdentifier();
                String parentIdentifier = connection.getParentIdentifier();

                // Release connection
                activeConnections.remove(identifier, activeConnection);
                activeConnectionGroups.remove(parentIdentifier, activeConnection);
                release(user, connection);

            }

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
     * @param tokens
     *     A Map containing the token names and corresponding values to be
     *     applied as parameter tokens when establishing the connection.
     *
     * @param interceptErrors
     *     Whether errors from the upstream remote desktop should be
     *     intercepted and rethrown as GuacamoleUpstreamExceptions.
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
            GuacamoleClientInformation info, Map<String, String> tokens,
            boolean interceptErrors) throws GuacamoleException {

        // Record new active connection
        Runnable cleanupTask = new ConnectionCleanupTask(activeConnection);
        activeTunnels.put(activeConnection.getUUID().toString(), activeConnection);

        try {

            GuacamoleConfiguration config;

            // Retrieve connection information associated with given connection record
            ModeledConnection connection = activeConnection.getConnection();

            // Pull configuration directly from the connection, additionally
            // joining the existing active connection (without sharing profile
            // restrictions) if such a connection exists
            if (activeConnection.isPrimaryConnection()) {
                activeConnections.put(connection.getIdentifier(), activeConnection);
                activeConnectionGroups.put(connection.getParentIdentifier(), activeConnection);
                config = getGuacamoleConfiguration(activeConnection.getUser(), connection, activeConnection.getConnectionID());
            }

            // If we ARE joining an active connection under the restrictions of
            // a sharing profile, generate a configuration which does so
            else {

                // Verify that the connection ID is known
                String connectionID = activeConnection.getConnectionID();
                if (connectionID == null)
                    throw new GuacamoleResourceNotFoundException("No existing connection to be joined.");

                // Build configuration from the sharing profile and the ID of
                // the connection being joined
                config = getGuacamoleConfiguration(activeConnection.getUser(),
                        activeConnection.getSharingProfile(), connectionID);

            }

            // Build token filter containing credential tokens
            TokenFilter tokenFilter = new TokenFilter();
            tokenFilter.setTokens(tokens);

            // Filter the configuration
            tokenFilter.filterValues(config.getParameters());

            // Obtain socket which will automatically run the cleanup task
            ConfiguredGuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                getUnconfiguredGuacamoleSocket(connection.getGuacamoleProxyConfiguration(),
                        cleanupTask), config, info);

            // Assign and return new tunnel
            if (interceptErrors)
                return activeConnection.assignGuacamoleTunnel(new FailoverGuacamoleSocket(socket), socket.getConnectionID());
            else
                return activeConnection.assignGuacamoleTunnel(socket, socket.getConnectionID());
            
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
    private Collection<String> getPreferredConnections(ModeledAuthenticatedUser user,
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
    private List<ModeledConnection> getBalancedConnections(ModeledAuthenticatedUser user,
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
    public Collection<ActiveConnectionRecord> getActiveConnections(ModeledAuthenticatedUser user)
        throws GuacamoleException {

        // Simply return empty list if there are no active tunnels
        Collection<ActiveConnectionRecord> records = activeTunnels.values();
        if (records.isEmpty())
            return Collections.<ActiveConnectionRecord>emptyList();

        // Privileged users (such as system administrators) can view all
        // connections; no need to filter
        if (user.isPrivileged())
            return records;

        // Build set of all connection identifiers associated with active tunnels
        Set<String> identifiers = new HashSet<String>(records.size());
        for (ActiveConnectionRecord record : records)
            identifiers.add(record.getConnection().getIdentifier());

        // Produce collection of readable connection identifiers
        Collection<ConnectionModel> connections =
                connectionMapper.selectReadable(user.getUser().getModel(),
                        identifiers, user.getEffectiveUserGroups());

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
    public GuacamoleTunnel getGuacamoleTunnel(final ModeledAuthenticatedUser user,
            final ModeledConnection connection, GuacamoleClientInformation info,
            Map<String, String> tokens) throws GuacamoleException {

        // Acquire access to single connection, ignoring the failover-only flag
        acquire(user, Collections.singletonList(connection), true);

        // Connect only if the connection was successfully acquired
        ActiveConnectionRecord connectionRecord = activeConnectionRecordProvider.get();
        connectionRecord.init(user, connection);
        return assignGuacamoleTunnel(connectionRecord, info, tokens, false);

    }

    @Override
    public Collection<ActiveConnectionRecord> getActiveConnections(Connection connection) {
        return activeConnections.get(connection.getIdentifier());
    }

    @Override
    @Transactional
    public GuacamoleTunnel getGuacamoleTunnel(ModeledAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup,
            GuacamoleClientInformation info, Map<String, String> tokens)
            throws GuacamoleException {

        // Track failures in upstream (remote desktop) connections
        boolean upstreamHasFailed = false;

        // If group has no associated balanced connections, cannot connect
        List<ModeledConnection> connections = getBalancedConnections(user, connectionGroup);
        if (connections.isEmpty())
            throw new GuacamoleSecurityException("Permission denied.");

        do {

            // Acquire group
            acquire(user, connectionGroup);

            // Attempt to acquire to any child, including failover-only
            // connections only if at least one upstream failure has occurred
            ModeledConnection connection;
            try {
                connection = acquire(user, connections, upstreamHasFailed);
            }

            // Ensure connection group is always released if child acquire fails
            catch (GuacamoleException e) {
                release(user, connectionGroup);
                throw e;
            }

            try {

                // Connect to acquired child
                ActiveConnectionRecord connectionRecord = activeConnectionRecordProvider.get();
                connectionRecord.init(user, connectionGroup, connection);
                GuacamoleTunnel tunnel = assignGuacamoleTunnel(connectionRecord,
                        info, tokens, connections.size() > 1);

                // If session affinity is enabled, prefer this connection going forward
                if (connectionGroup.isSessionAffinityEnabled())
                    user.preferConnection(connection.getIdentifier());

                // Warn if we are connecting to a failover-only connection
                if (connection.isFailoverOnly())
                    logger.warn("One or more normal connections within "
                            + "group \"{}\" have failed. Some connection "
                            + "attempts are being routed to designated "
                            + "failover-only connections.",
                            connectionGroup.getIdentifier());

                return tunnel;

            }

            // If connection failed due to an upstream error, retry other
            // connections
            catch (GuacamoleUpstreamException e) {
                logger.info("Upstream error intercepted for connection \"{}\". Failing over to next connection in group...", connection.getIdentifier());
                logger.debug("Upstream remote desktop reported an error during connection.", e);
                connections.remove(connection);
                upstreamHasFailed = true;
            }

        } while (!connections.isEmpty());

        // All connection possibilities have been exhausted
        throw new GuacamoleResourceConflictException("Cannot connect. All upstream connections are unavailable.");

    }

    @Override
    public Collection<ActiveConnectionRecord> getActiveConnections(ConnectionGroup connectionGroup) {

        // If not a balancing group, assume no connections
        if (connectionGroup.getType() != ConnectionGroup.Type.BALANCING)
            return Collections.<ActiveConnectionRecord>emptyList();

        return activeConnectionGroups.get(connectionGroup.getIdentifier());

    }

    @Override
    @Transactional
    public GuacamoleTunnel getGuacamoleTunnel(RemoteAuthenticatedUser user,
            SharedConnectionDefinition definition,
            GuacamoleClientInformation info, Map<String, String> tokens)
            throws GuacamoleException {

        // Create a connection record which describes the shared connection
        ActiveConnectionRecord connectionRecord = activeConnectionRecordProvider.get();
        connectionRecord.init(user, definition.getActiveConnection(),
                definition.getSharingProfile());

        // Connect to shared connection described by the created record
        GuacamoleTunnel tunnel = assignGuacamoleTunnel(connectionRecord, info, tokens, false);

        // Register tunnel, such that it is closed when the
        // SharedConnectionDefinition is invalidated
        definition.registerTunnel(tunnel);
        return tunnel;

    }

}
