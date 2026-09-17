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

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.guacamole.GuacamoleClientTooManyException;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceConflictException;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatRequestBuilder;
import org.apache.guacamole.cluster.SeatResult;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.user.RemoteAuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * GuacamoleTunnelService implementation which restricts concurrency for each
 * connection and group according to a maximum number of connections and
 * maximum number of connections per user.
 */
@Singleton
public class RestrictedGuacamoleTunnelService
    extends AbstractGuacamoleTunnelService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RestrictedGuacamoleTunnelService.class);

    /**
     * The environment of the Guacamole server.
     */
    @Inject
    private JDBCEnvironment environment;

    /**
     * Cluster-wide seat accounting. Bound to NoOpClusterStore when clustering
     * is disabled, in which case every acquire falls through to the in-memory
     * counters below.
     */
    @Inject
    private ClusterStore clusterStore;

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
     * The total number of active connections within this instance of
     * Guacamole.
     */
    private final AtomicInteger totalActiveConnections = new AtomicInteger(0);

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

    /**
     * Attempts to take cluster-wide seats for one connection.
     *
     * @param seatToken
     *     Identifier for the seats.
     *
     * @param username
     *     The user connecting.
     *
     * @param connection
     *     The connection being acquired.
     *
     * @return
     *     The outcome, or null if the cluster store is unavailable and the
     *     caller must fall back to replica-local accounting.
     *
     * @throws GuacamoleException
     *     If the connection's configured limits cannot be read.
     */
    private SeatResult tryClusterAcquire(String seatToken, String username,
            ModeledConnection connection) throws GuacamoleException {

        if (!clusterStore.isAvailable())
            return null;

        SeatRequest request = SeatRequestBuilder.forConnection(seatToken, username,
                connection.getIdentifier(), connection.getMaxConnections(),
                connection.getMaxConnectionsPerUser());

        try {
            return clusterStore.acquireSeats(request);
        }

        // A Redis outage must not become a connection outage. Limits degrade to
        // per-replica, which is exactly upstream behavior (spec 6.1).
        catch (GuacamoleException e) {
            logger.error("Cluster seat acquisition failed for connection \"{}\". "
                    + "Concurrency limits are now enforced per replica only.",
                    connection.getIdentifier(), e);
            return null;
        }

    }

    /**
     * Attempts to increment the given AtomicInteger without exceeding the
     * specified maximum value. If the AtomicInteger cannot be incremented
     * without exceeding the maximum, false is returned.
     *
     * @param counter
     *     The AtomicInteger to attempt to increment.
     *
     * @param max
     *     The maximum value that the given AtomicInteger should contain, or
     *     zero if no limit applies.
     *
     * @return
     *     true if the AtomicInteger was successfully incremented without
     *     exceeding the specified maximum, false if the AtomicInteger could
     *     not be incremented.
     */
    private boolean tryIncrement(AtomicInteger counter, int max) {

        // Repeatedly attempt to increment the given AtomicInteger until we
        // explicitly succeed or explicitly fail
        while (true) {

            // Get current value
            int count = counter.get();

            // Bail out if the maximum has already been reached
            if (count >= max && max != 0)
                return false;

            // Attempt to increment
            if (counter.compareAndSet(count, count+1))
                return true;

            // Try again if unsuccessful

        }

    }

    @Override
    protected ModeledConnection acquire(RemoteAuthenticatedUser user,
            List<ModeledConnection> connections, boolean includeFailoverOnly,
            String seatToken)
            throws GuacamoleException {

        // Do not acquire connection unless within overall limits
        if (!tryIncrement(totalActiveConnections, environment.getAbsoluteMaxConnections()))
            throw new GuacamoleResourceConflictException("Cannot connect. Overall maximum connections reached.");

        // Get username
        String username = user.getIdentifier();

        // Sort connections in ascending order of usage
        ModeledConnection[] sortedConnections = connections.toArray(new ModeledConnection[connections.size()]);
        Arrays.sort(sortedConnections, new Comparator<ModeledConnection>() {

            @Override
            public int compare(ModeledConnection a, ModeledConnection b) {

                // Active connections
                int connA = getActiveConnections(a).size();
                int connB = getActiveConnections(b).size();

                // Assigned weight
                int weightA = a.getConnectionWeight();
                int weightB = b.getConnectionWeight();

                // Calculated weight of connections
                int calcWeightA = connA * weightB;
                int calcWeightB = connB * weightA;

                // If calculated weights are equal, return difference in assigned weight
                if (calcWeightA == calcWeightB)
                    return (weightA - weightB);

                // Return different in calculated weights
                return (calcWeightA - calcWeightB);

            }

        });

        // Track whether acquire fails due to user-specific limits
        boolean userSpecificFailure = true;

        // Return the first unreserved connection
        for (ModeledConnection connection : sortedConnections) {

            // If connection weight is less than 1 this host is disabled and should not be used.
            if (connection.getConnectionWeight() < 1) {
                logger.debug("Weight for {} is < 1, connection will be skipped.", connection.getName());
                continue;
            }

            // Skip connections which are failover-only if they are excluded
            // from this connection attempt
            if (!includeFailoverOnly && connection.isFailoverOnly())
                continue;

            // Cluster-wide decision first; null means Redis could not answer
            SeatResult result = tryClusterAcquire(seatToken, username, connection);

            if (result == SeatResult.SUCCESS)
                return connection;

            if (result == SeatResult.CONNECTION_LIMIT) {
                // Busy, but not because of this user -- try the next child
                userSpecificFailure = false;
                continue;
            }

            if (result == SeatResult.USER_CONNECTION_LIMIT)
                continue;

            // Redis unavailable: fall back to replica-local accounting, which
            // is exactly upstream behavior
            Seat seat = new Seat(username, connection.getIdentifier());
            if (tryAdd(activeSeats, seat,
                    connection.getMaxConnectionsPerUser())) {

                // Attempt to aquire connection according to overall limits
                if (tryAdd(activeConnections, connection.getIdentifier(),
                        connection.getMaxConnections()))
                    return connection;

                // Acquire failed - retry with next connection
                activeSeats.remove(seat);

                // Failure to acquire is not user-specific
                userSpecificFailure = false;

            }

        }

        // Acquire failed
        totalActiveConnections.decrementAndGet();

        // Too many connections by this user
        if (userSpecificFailure)
            throw new GuacamoleClientTooManyException("Cannot connect. Connection already in use by this user.");

        // Too many connections, but not necessarily due purely to this user
        else
            throw new GuacamoleResourceConflictException("Cannot connect. This connection is in use.");

    }

    @Override
    protected void release(RemoteAuthenticatedUser user,
            ModeledConnection connection, String seatToken) {

        // Release cluster seats first; the local counters are the fallback and
        // are always safe to decrement
        try {
            if (clusterStore.isAvailable())
                clusterStore.releaseSeats(SeatRequestBuilder.forConnection(
                        seatToken, user.getIdentifier(), connection.getIdentifier(),
                        connection.getMaxConnections(),
                        connection.getMaxConnectionsPerUser()));
        }

        // Not fatal: the seat ages out of every index within the stale window
        catch (GuacamoleException e) {
            logger.warn("Unable to release cluster seats for connection \"{}\". "
                    + "They will expire on their own.", connection.getIdentifier(), e);
        }

        activeSeats.remove(new Seat(user.getIdentifier(), connection.getIdentifier()));
        activeConnections.remove(connection.getIdentifier());
        totalActiveConnections.decrementAndGet();

    }

    @Override
    protected void acquire(RemoteAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup, String seatToken)
            throws GuacamoleException {

        // Get username
        String username = user.getIdentifier();

        // Attempt to aquire connection group according to per-user limits
        Seat seat = new Seat(username, connectionGroup.getIdentifier());
        if (tryAdd(activeGroupSeats, seat,
                connectionGroup.getMaxConnectionsPerUser())) {

            // Attempt to aquire connection group according to overall limits
            if (tryAdd(activeGroups, connectionGroup.getIdentifier(),
                    connectionGroup.getMaxConnections()))
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
    protected void release(RemoteAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup, String seatToken) {
        activeGroupSeats.remove(new Seat(user.getIdentifier(), connectionGroup.getIdentifier()));
        activeGroups.remove(connectionGroup.getIdentifier());
    }

}
