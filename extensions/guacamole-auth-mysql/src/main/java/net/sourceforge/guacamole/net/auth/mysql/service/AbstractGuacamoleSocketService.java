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

package net.sourceforge.guacamole.net.auth.mysql.service;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnection;
import net.sourceforge.guacamole.net.auth.mysql.dao.ParameterMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionModel;
import net.sourceforge.guacamole.net.auth.mysql.model.ParameterModel;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;


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
     * Mapper for accessing connection parameters.
     */
    @Inject
    private ParameterMapper parameterMapper;

    /**
     * The current number of concurrent uses of the connection having a given
     * identifier.
     */
    private final ConcurrentHashMap<String, AtomicInteger> activeConnectionCount =
            new ConcurrentHashMap<String, AtomicInteger>();

    /**
     * Atomically increments the current usage count for the given connection.
     *
     * @param connection
     *     The connection which is being used.
     */
    private void incrementUsage(MySQLConnection connection) {

        // Increment or initialize usage count atomically
        AtomicInteger count = activeConnectionCount.putIfAbsent(connection.getIdentifier(), new AtomicInteger(1));
        if (count != null)
            count.incrementAndGet();

    }

    /**
     * Atomically decrements the current usage count for the given connection.
     * If a combination of incrementUsage() and decrementUsage() calls result
     * in the usage counter being reduced to zero, it is guaranteed that one
     * of those decrementUsage() calls will remove the value from the map.
     *
     * @param connection
     *     The connection which is no longer being used.
     */
    private void decrementUsage(MySQLConnection connection) {

        // Decrement usage count, remove entry if it becomes zero
        AtomicInteger count = activeConnectionCount.get(connection.getIdentifier());
        if (count != null) {
            count.decrementAndGet();
            activeConnectionCount.remove(connection.getIdentifier(), 0);
        }

    }

    /**
     * Acquires possibly-exclusive access to the given connection on behalf of
     * the given user. If access is denied for any reason, an exception is
     * thrown.
     *
     * @param user
     *     The user acquiring access.
     *
     * @param connection
     *     The connection being accessed.
     *
     * @throws GuacamoleException
     *     If access is denied to the given user for any reason.
     */
    protected abstract void acquire(AuthenticatedUser user,
            MySQLConnection connection) throws GuacamoleException;

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
            MySQLConnection connection);

    /**
     * Creates a socket for the given user which connects to the given
     * connection. The given client information will be passed to guacd when
     * the connection is established. This function will apply any concurrent
     * usage rules in effect, but will NOT test object- or system-level
     * permissions.
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
     *     If the connection cannot be established due to concurrent usage
     *     rules.
     */
    @Override
    public GuacamoleSocket getGuacamoleSocket(final AuthenticatedUser user,
            final MySQLConnection connection, GuacamoleClientInformation info)
            throws GuacamoleException {

        // Generate configuration from available data
        GuacamoleConfiguration config = new GuacamoleConfiguration();

        // Set protocol from connection
        ConnectionModel model = connection.getModel();
        config.setProtocol(model.getProtocol());

        // Set parameters from associated data
        Collection<ParameterModel> parameters = parameterMapper.select(connection.getIdentifier());
        for (ParameterModel parameter : parameters)
            config.setParameter(parameter.getName(), parameter.getValue());

        // Return new socket
        try {

            // Atomically gain access to connection
            acquire(user, connection);
            incrementUsage(connection);

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
                    decrementUsage(connection);
                    release(user, connection);

                }
                
            };

        }

        // Release connection in case of error
        catch (GuacamoleException e) {

            // Atomically release access to connection
            decrementUsage(connection);
            release(user, connection);

            throw e;

        }

    }

    @Override
    public int getActiveConnections(Connection connection) {

        // If no such active connection, zero active users
        AtomicInteger count = activeConnectionCount.get(connection.getIdentifier());
        if (count == null)
            return 0;

        // Otherwise, return stored value
        return count.intValue();
        
    }
    
}
