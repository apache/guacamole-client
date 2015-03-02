/*
 * Copyright (C) 2015 Glyptodon LLC
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

package org.glyptodon.guacamole.auth.jdbc.socket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.connection.ModeledConnection;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionRecordMapper;
import org.glyptodon.guacamole.auth.jdbc.connection.ParameterMapper;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionModel;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionRecordModel;
import org.glyptodon.guacamole.auth.jdbc.connection.ParameterModel;
import org.glyptodon.guacamole.auth.jdbc.user.UserModel;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionMapper;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.token.StandardTokens;
import org.glyptodon.guacamole.token.TokenFilter;
import org.mybatis.guice.transactional.Transactional;


/**
 * Base implementation of the GuacamoleSocketService, handling retrieval of
 * connection parameters, load balancing, and connection usage counts. The
 * implementation of concurrency rules is up to policy-specific subclasses.
 *
 * @author Michael Jumper
 */
public abstract class AbstractGuacamoleSocketService implements GuacamoleSocketService {

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
     * Provider for creating connections.
     */
    @Inject
    private Provider<ModeledConnection> connectionProvider;

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
     * Creates a socket for the given user which connects to the given
     * connection, which MUST already be acquired via acquire(). The given
     * client information will be passed to guacd when the connection is
     * established.
     * 
     * The connection will be automatically released when it closes, or if it
     * fails to establish entirely.
     *
     * @param user
     *     The user for whom the connection is being established.
     *
     * @param connection
     *     The connection the user is connecting to.
     *
     * @param info
     *     Information describing the Guacamole client connecting to the given
     *     connection.
     *
     * @return
     *     A new GuacamoleSocket which is configured and connected to the given
     *     connection.
     *
     * @throws GuacamoleException
     *     If an error occurs while the connection is being established, or
     *     while connection configuration information is being retrieved.
     */
    private GuacamoleSocket connect(final AuthenticatedUser user,
            final ModeledConnection connection, GuacamoleClientInformation info)
            throws GuacamoleException {

        // Create record for active connection
        final ActiveConnectionRecord activeConnection = new ActiveConnectionRecord(user);

        // Get relevant identifiers
        final String identifier = connection.getIdentifier();
        final String parentIdentifier = connection.getParentIdentifier();
        
        // Generate configuration from available data
        GuacamoleConfiguration config = new GuacamoleConfiguration();

        // Set protocol from connection
        ConnectionModel model = connection.getModel();
        config.setProtocol(model.getProtocol());

        // Set parameters from associated data
        Collection<ParameterModel> parameters = parameterMapper.select(identifier);
        for (ParameterModel parameter : parameters)
            config.setParameter(parameter.getName(), parameter.getValue());

        // Build token filter containing credential tokens
        TokenFilter tokenFilter = new TokenFilter();
        StandardTokens.addStandardTokens(tokenFilter, user.getCredentials());

        // Filter the configuration
        tokenFilter.filterValues(config.getParameters());

        // Return new socket
        try {

            // Record new active connection
            activeConnections.put(identifier, activeConnection);
            activeConnectionGroups.put(parentIdentifier, activeConnection);

            // Return newly-reserved connection
            return new ConfiguredGuacamoleSocket(
                new InetGuacamoleSocket(
                    environment.getRequiredProperty(Environment.GUACD_HOSTNAME),
                    environment.getRequiredProperty(Environment.GUACD_PORT)
                ),
                config
            ) {

                @Override
                public void close() throws GuacamoleException {

                    // Attempt to close connection
                    super.close();
                    
                    // Release connection upon close
                    activeConnections.remove(identifier, activeConnection);
                    activeConnectionGroups.remove(parentIdentifier, activeConnection);
                    release(user, connection);

                    UserModel userModel = user.getUser().getModel();
                    ConnectionRecordModel recordModel = new ConnectionRecordModel();

                    // Copy user information and timestamps into new record
                    recordModel.setUserID(userModel.getObjectID());
                    recordModel.setUsername(userModel.getIdentifier());
                    recordModel.setConnectionIdentifier(identifier);
                    recordModel.setStartDate(activeConnection.getStartDate());
                    recordModel.setEndDate(new Date());

                    // Insert connection record
                    connectionRecordMapper.insert(recordModel);
                    
                }
                
            };

        }

        // Release connection in case of error
        catch (GuacamoleException e) {

            // Atomically release access to connection
            activeConnections.remove(identifier, activeConnection);
            activeConnectionGroups.remove(parentIdentifier, activeConnection);
            release(user, connection);

            throw e;

        }

    }

    @Override
    @Transactional
    public GuacamoleSocket getGuacamoleSocket(final AuthenticatedUser user,
            final ModeledConnection connection, GuacamoleClientInformation info)
            throws GuacamoleException {

        // Acquire and connect to single connection
        acquire(user, Collections.singletonList(connection));
        return connect(user, connection, info);

    }

    @Override
    public Collection<ConnectionRecord> getActiveConnections(Connection connection) {
        return activeConnections.get(connection.getIdentifier());
    }

    @Override
    @Transactional
    public GuacamoleSocket getGuacamoleSocket(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup,
            GuacamoleClientInformation info) throws GuacamoleException {

        // If not a balancing group, cannot connect
        if (connectionGroup.getType() != ConnectionGroup.Type.BALANCING)
            throw new GuacamoleSecurityException("Permission denied.");
        
        // If group has no children, cannot connect
        Collection<String> identifiers = connectionMapper.selectIdentifiersWithin(connectionGroup.getIdentifier());
        if (identifiers.isEmpty())
            throw new GuacamoleSecurityException("Permission denied.");

        // Otherwise, retrieve all children
        Collection<ConnectionModel> models = connectionMapper.select(identifiers);
        List<ModeledConnection> connections = new ArrayList<ModeledConnection>(models.size());

        // Convert each retrieved model to a modeled connection
        for (ConnectionModel model : models) {
            ModeledConnection connection = connectionProvider.get();
            connection.init(user, model);
            connections.add(connection);
        }

        // Acquire and connect to any child
        ModeledConnection connection = acquire(user, connections);
        return connect(user, connection, info);

    }

    @Override
    public Collection<ConnectionRecord> getActiveConnections(ConnectionGroup connectionGroup) {

        // If not a balancing group, assume no connections
        if (connectionGroup.getType() != ConnectionGroup.Type.BALANCING)
            return Collections.EMPTY_LIST;

        return activeConnectionGroups.get(connectionGroup.getIdentifier());

    }

}
