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

package org.glyptodon.guacamole.auth.jdbc.tunnel;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.glyptodon.guacamole.GuacamoleClientTooManyException;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.connection.ModeledConnection;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceConflictException;
import org.glyptodon.guacamole.auth.jdbc.JDBCEnvironment;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;


/**
 * GuacamoleTunnelService implementation which restricts concurrency for each
 * connection and group according to a maximum number of connections and
 * maximum number of connections per user.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
@Singleton
public class ConfigurableGuacamoleTunnelService
    extends AbstractGuacamoleTunnelService {
    
    /**
     * The Guacamole server environment.
     */
    @Inject
    private JDBCEnvironment environment;

    /**
     * Set of all currently-active user/connection pairs (seats).
     */
    private final ConcurrentHashMultiset<Seat> activeSeats = ConcurrentHashMultiset.<Seat>create();

    /**
     * Set of all currently-active connections.
     */
    private final ConcurrentHashMultiset<String> activeConnections = ConcurrentHashMultiset.<String>create();

    /**
     * Set of all currently-active user/connection group pairs (seats).
     */
    private final ConcurrentHashMultiset<Seat> activeGroupSeats = ConcurrentHashMultiset.<Seat>create();

    /**
     * Set of all currently-active connection groups.
     */
    private final ConcurrentHashMultiset<String> activeGroups = ConcurrentHashMultiset.<String>create();

    /**
     * Attempts to add a single instance of the given value to the given
     * multiset without exceeding the specified maximum number of values. If
     * the value cannot be added without exceeding the maximum, false is
     * returned.
     *
     * @param <T>
     *     The type of values contained within the multiset.
     *
     * @param multiset
     *     The multiset to attempt to add a value to.
     *
     * @param value
     *     The value to attempt to add.
     *
     * @param max
     *     The maximum number of each distinct value that the given multiset
     *     should hold, or zero if no limit applies.
     *
     * @return
     *     true if the value was successfully added without exceeding the
     *     specified maximum, false if the value could not be added.
     */
    private <T> boolean tryAdd(ConcurrentHashMultiset<T> multiset, T value, int max) {

        // Repeatedly attempt to add a new value to the given multiset until we
        // explicitly succeed or explicitly fail
        while (true) {

            // Get current number of values
            int count = multiset.count(value);

            // Bail out if the maximum has already been reached
            if (count >= max && max != 0)
                return false;

            // Attempt to add one more value
            if (multiset.setCount(value, count, count+1))
                return true;

            // Try again if unsuccessful

        }

    }

    @Override
    protected ModeledConnection acquire(AuthenticatedUser user,
            List<ModeledConnection> connections) throws GuacamoleException {

        // Get username
        String username = user.getUser().getIdentifier();

        // Sort connections in ascending order of usage
        ModeledConnection[] sortedConnections = connections.toArray(new ModeledConnection[connections.size()]);
        Arrays.sort(sortedConnections, new Comparator<ModeledConnection>() {

            @Override
            public int compare(ModeledConnection a, ModeledConnection b) {

                return getActiveConnections(a).size()
                     - getActiveConnections(b).size();

            }

        });

        // Track whether acquire fails due to user-specific limits
        boolean userSpecificFailure = true;

        // Return the first unreserved connection
        for (ModeledConnection connection : sortedConnections) {

            // Determine per-user limits on this connection
            Integer connectionMaxConnectionsPerUser = connection.getModel().getMaxConnectionsPerUser();
            if (connectionMaxConnectionsPerUser == null)
                connectionMaxConnectionsPerUser = environment.getDefaultMaxConnectionsPerUser();

            // Determine overall limits on this connection
            Integer connectionMaxConnections = connection.getModel().getMaxConnections();
            if (connectionMaxConnections == null)
                connectionMaxConnections = environment.getDefaultMaxConnections();

            // Attempt to aquire connection according to per-user limits
            Seat seat = new Seat(username, connection.getIdentifier());
            if (tryAdd(activeSeats, seat, connectionMaxConnectionsPerUser)) {

                // Attempt to aquire connection according to overall limits
                if (tryAdd(activeConnections, connection.getIdentifier(),
                        connectionMaxConnections))
                    return connection;

                // Acquire failed - retry with next connection
                activeSeats.remove(seat);

                // Failure to acquire is not user-specific
                userSpecificFailure = false;

            }

        }

        // Too many connections by this user
        if (userSpecificFailure)
            throw new GuacamoleClientTooManyException("Cannot connect. Connection already in use by this user.");

        // Too many connections, but not necessarily due purely to this user
        else
            throw new GuacamoleResourceConflictException("Cannot connect. This connection is in use.");

    }

    @Override
    protected void release(AuthenticatedUser user, ModeledConnection connection) {
        activeSeats.remove(new Seat(user.getUser().getIdentifier(), connection.getIdentifier()));
        activeConnections.remove(connection.getIdentifier());
    }

    @Override
    protected void acquire(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup) throws GuacamoleException {

        // Get username
        String username = user.getUser().getIdentifier();

        // Determine per-user limits on this connection group
        Integer connectionGroupMaxConnectionsPerUser = connectionGroup.getModel().getMaxConnectionsPerUser();
        if (connectionGroupMaxConnectionsPerUser == null)
            connectionGroupMaxConnectionsPerUser = environment.getDefaultMaxGroupConnectionsPerUser();

        // Determine overall limits on this connection group
        Integer connectionGroupMaxConnections = connectionGroup.getModel().getMaxConnections();
        if (connectionGroupMaxConnections == null)
            connectionGroupMaxConnections = environment.getDefaultMaxGroupConnections();

        // Attempt to aquire connection group according to per-user limits
        Seat seat = new Seat(username, connectionGroup.getIdentifier());
        if (tryAdd(activeGroupSeats, seat,
                connectionGroupMaxConnectionsPerUser)) {

            // Attempt to aquire connection group according to overall limits
            if (tryAdd(activeGroups, connectionGroup.getIdentifier(),
                    connectionGroupMaxConnections))
                return;

            // Acquire failed
            activeGroupSeats.remove(seat);

            // Failure to acquire is not user-specific
            throw new GuacamoleResourceConflictException("Cannot connect. This connection group is in use.");

        }

        // Already in use by this user
        throw new GuacamoleClientTooManyException("Cannot connect. Connection group already in use by this user.");

    }

    @Override
    protected void release(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup) {
        activeGroupSeats.remove(new Seat(user.getUser().getIdentifier(), connectionGroup.getIdentifier()));
        activeGroups.remove(connectionGroup.getIdentifier());
    }

}
