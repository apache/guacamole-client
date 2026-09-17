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

package org.apache.guacamole.cluster.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.ArrayList;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.SeatKey;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatResult;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis-backed implementation of ClusterStore, using Lettuce.
 */
public class RedisClusterStore implements ClusterStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterStore.class);

    /**
     * Seconds any single Redis command may take before it is abandoned. Seat
     * acquisition sits directly in the connect path, so this bounds how long a
     * user waits when Redis is degraded rather than cleanly down.
     */
    private static final long COMMAND_TIMEOUT_SECONDS = 2L;

    private static final String ACQUIRE_SEATS_SCRIPT =
            "/org/apache/guacamole/cluster/redis/acquire-seats.lua";

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final LuaScript acquireSeats = LuaScript.load(ACQUIRE_SEATS_SCRIPT);
    private final long staleWindowMs;
    private final String nodeId;

    private volatile boolean available = true;

    /**
     * @param redisUri
     *     Lettuce URI of the Redis server or Sentinel set.
     *
     * @param staleWindowMs
     *     Milliseconds after which an unrefreshed member is considered dead.
     *
     * @param nodeId
     *     Identity of this replica.
     */
    public RedisClusterStore(String redisUri, long staleWindowMs, String nodeId) {

        // The timeout belongs on the URI; AbstractRedisClient.setDefaultTimeout
        // is deprecated, and this module compiles with -Werror.
        RedisURI parsedUri = RedisURI.create(redisUri);
        parsedUri.setTimeout(Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS));
        this.client = RedisClient.create(parsedUri);

        // Fail fast rather than queue. Lettuce's default is to buffer commands
        // while the connection is down and wait out a long command timeout, so
        // the first connect attempt after Redis becomes unreachable blocks for
        // minutes instead of degrading -- measured at over 180 seconds on a
        // live cluster. Rejecting immediately is what makes the documented
        // fallback to per-replica limits actually reachable.
        client.setOptions(ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build());

        this.connection = client.connect();
        this.staleWindowMs = staleWindowMs;
        this.nodeId = nodeId;
    }

    private RedisCommands<String, String> commands() {
        return connection.sync();
    }

    @Override
    public SeatResult acquireSeats(SeatRequest request) throws GuacamoleException {

        List<SeatKey> seatKeys = request.getKeys();

        String[] keys = new String[seatKeys.size()];
        String[] args = new String[seatKeys.size() + 2];

        args[0] = request.getTunnelUuid();
        args[1] = Long.toString(staleWindowMs);

        for (int i = 0; i < seatKeys.size(); i++) {
            keys[i] = seatKeys.get(i).getRedisKey();
            args[i + 2] = Integer.toString(seatKeys.get(i).getLimit());
        }

        try {

            long result = acquireSeats.eval(commands(), keys, args);
            available = true;

            if (result == 0)
                return SeatResult.SUCCESS;

            return seatKeys.get((int) result - 1).getFailureResult();

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to acquire cluster seats.", e);
        }

    }

    @Override
    public void releaseSeats(SeatRequest request) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();
            for (SeatKey key : request.getKeys())
                commands.zrem(key.getRedisKey(), request.getTunnelUuid());

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to release cluster seats.", e);
        }

    }

    /**
     * Returns every sorted set the given tunnel is indexed in. The group index
     * is included only when the tunnel was established through a balancing
     * group.
     *
     * @param registration
     *     The tunnel to index.
     *
     * @return
     *     Every index key for the given tunnel.
     */
    private List<String> indexKeys(TunnelRegistration registration) {

        List<String> keys = new ArrayList<String>(5);
        keys.add(ClusterKeys.ALL_INDEX);
        keys.add(ClusterKeys.userIndex(registration.getUsername()));
        keys.add(ClusterKeys.guacdIndex(registration.getEndpoint().toKey()));
        keys.add(ClusterKeys.connectionIndex(registration.getConnectionIdentifier()));

        if (registration.getGroupIdentifier() != null)
            keys.add(ClusterKeys.groupIndex(registration.getGroupIdentifier()));

        return keys;

    }

    /**
     * @return
     *     The current time according to the Redis server, in milliseconds.
     */
    private long serverTimeMillis(RedisCommands<String, String> commands) {
        List<String> time = commands.time();
        return (Long.parseLong(time.get(0)) * 1000L)
                + (Long.parseLong(time.get(1)) / 1000L);
    }

    @Override
    public void registerTunnel(TunnelRegistration registration) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();
            long now = serverTimeMillis(commands);

            Map<String, String> record = new HashMap<String, String>();
            record.put("nodeId", registration.getNodeId());
            record.put("guacdConnectionId", registration.getGuacdConnectionId());
            record.put("guacdEndpoint", registration.getEndpoint().toKey());
            record.put("connIdentifier", registration.getConnectionIdentifier());
            record.put("username", registration.getUsername());
            record.put("startTime", Long.toString(registration.getStartTime()));

            if (registration.getGroupIdentifier() != null)
                record.put("groupIdentifier", registration.getGroupIdentifier());

            if (registration.getSharingProfileIdentifier() != null)
                record.put("sharingProfileId", registration.getSharingProfileIdentifier());

            if (registration.getRemoteHost() != null)
                record.put("remoteHost", registration.getRemoteHost());

            String tunnelKey = ClusterKeys.tunnel(registration.getTunnelUuid());
            commands.hset(tunnelKey, record);
            commands.pexpire(tunnelKey, staleWindowMs);

            for (String key : indexKeys(registration))
                commands.zadd(key, (double) now, registration.getTunnelUuid());

            if (registration.getGuacdConnectionId() != null) {
                String routeKey = ClusterKeys.route(registration.getGuacdConnectionId());
                commands.psetex(routeKey, staleWindowMs, registration.getEndpoint().toKey());
            }

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to register tunnel with cluster.", e);
        }

    }

    @Override
    public void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();

            for (String key : indexKeys(registration))
                commands.zrem(key, registration.getTunnelUuid());

            commands.del(ClusterKeys.tunnel(registration.getTunnelUuid()));

            if (registration.getGuacdConnectionId() != null)
                commands.del(ClusterKeys.route(registration.getGuacdConnectionId()));

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to unregister tunnel from cluster.", e);
        }

    }

    @Override
    public void heartbeat(Collection<TunnelRegistration> registrations) throws GuacamoleException {

        if (registrations.isEmpty())
            return;

        try {

            RedisCommands<String, String> commands = commands();
            long now = serverTimeMillis(commands);

            for (TunnelRegistration registration : registrations) {

                for (String key : indexKeys(registration))
                    commands.zadd(key, (double) now, registration.getTunnelUuid());

                commands.pexpire(ClusterKeys.tunnel(registration.getTunnelUuid()), staleWindowMs);

                if (registration.getGuacdConnectionId() != null)
                    commands.pexpire(ClusterKeys.route(registration.getGuacdConnectionId()),
                            staleWindowMs);

            }

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to refresh cluster heartbeat.", e);
        }

    }

    @Override
    public GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException {

        try {

            String value = commands().get(ClusterKeys.route(guacdConnectionId));
            available = true;

            if (value == null)
                return null;

            return GuacdEndpoint.fromKey(value);

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to look up guacd route.", e);
        }

    }

    @Override
    public long countTunnels(GuacdEndpoint endpoint) {

        try {

            RedisCommands<String, String> commands = commands();
            long cutoff = serverTimeMillis(commands) - staleWindowMs;

            Long count = commands.zcount(ClusterKeys.guacdIndex(endpoint.toKey()),
                    io.lettuce.core.Range.from(
                            io.lettuce.core.Range.Boundary.excluding((double) cutoff),
                            io.lettuce.core.Range.Boundary.unbounded()));

            available = true;
            return count == null ? 0L : count;

        }
        catch (RedisException e) {
            available = false;
            logger.warn("Unable to count tunnels for guacd \"{}\". Treating as unloaded.",
                    endpoint, e);
            return 0L;
        }

    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void shutdown() {
        connection.close();
        client.shutdown();
    }

}
