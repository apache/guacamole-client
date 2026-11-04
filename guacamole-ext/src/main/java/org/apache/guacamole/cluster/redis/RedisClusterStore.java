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
import io.lettuce.core.SslOptions;
import io.lettuce.core.ZAddArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.io.File;
import java.util.ArrayList;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.ClusterKillHandler;
import org.apache.guacamole.cluster.ClusterLogoutHandler;
import org.apache.guacamole.cluster.ClusterShareRevocationHandler;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.SeatKey;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatResult;
import org.apache.guacamole.cluster.SharedConnectionEntry;
import org.apache.guacamole.cluster.TokenIdentity;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.cluster.metrics.ClusterMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis-backed implementation of ClusterStore, using the Lettuce client. All
 * replicas of a cluster share one Redis server (or Sentinel-managed set), and
 * this class is the only code which reads or writes cluster state there.
 *
 * Every key lives under the "guac:" prefix and is named by ClusterKeys:
 *
 * - "guac:tunnel:{uuid}" is a hash describing one live tunnel (owning
 *   replica, guacd endpoint, connection, user, and so on).
 * - "guac:idx:*" are sorted sets indexing live tunnels cluster-wide, by user,
 *   by connection, by balancing group, and by guacd instance. Members are
 *   tunnel UUIDs; scores are the Redis server time, in milliseconds, of each
 *   tunnel's most recent heartbeat.
 * - "guac:seat:*" are sorted sets of tunnel UUIDs used to enforce per-user
 *   concurrency limits, scored by the server time at which each seat was
 *   claimed. The connection and group indexes double as the sets enforcing
 *   overall per-connection and per-group limits.
 * - "guac:route:{id}" maps a guacd connection ID to the guacd instance
 *   hosting it, so that a join can be routed to the right instance.
 * - "guac:record:{uuid}" maps a connection history record UUID to the tunnel
 *   UUID, so that a kill addressed by record UUID can find the tunnel.
 * - "guac:share:{key}" and "guac:token:{hash}" are hashes describing share
 *   keys and authentication tokens, so that either can be honoured by any
 *   replica.
 * - "guac:authfail:{address}" counts recent authentication failures per
 *   client address.
 *
 * Liveness is maintained by heartbeat rather than by explicit cleanup alone.
 * Each replica periodically refreshes the scores and expiries of its own
 * tunnels; anything a replica stops refreshing (because it crashed, for
 * example) ages out after the stale window, either by key expiry or by being
 * excluded from score-filtered reads and pruned by the seat script.
 *
 * Kills, share key revocations, and logouts are broadcast to every replica
 * over Redis pub/sub, on a dedicated subscription connection.
 *
 * Redis being unavailable is expected and handled per operation. Each method
 * either fails closed, throwing so that the caller falls back to replica-local
 * behaviour, or fails open, returning a value which is safe in the absence of
 * cluster state. Every failure marks the store unavailable and every success
 * marks it available again; see isAvailable().
 */
public class RedisClusterStore implements ClusterStore {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RedisClusterStore.class);

    /**
     * Seconds any single Redis command may take before it is abandoned. Seat
     * acquisition sits directly in the connect path, so this bounds how long a
     * user waits when Redis is degraded rather than cleanly down.
     */
    private static final long COMMAND_TIMEOUT_SECONDS = 2L;

    /**
     * The number of milliseconds after a failure before an unavailable store
     * reports itself available again, allowing callers to retry Redis rather
     * than staying on replica-local behaviour indefinitely.
     */
    private static final long RETRY_INTERVAL_MS = 10000L;

    /**
     * The classpath location of the Lua script which atomically checks every
     * applicable concurrency limit and claims a seat under each.
     */
    private static final String ACQUIRE_SEATS_SCRIPT =
            "/org/apache/guacamole/cluster/redis/acquire-seats.lua";

    /**
     * The classpath location of the Lua script which atomically increments
     * an address's authentication failure counter and re-arms its expiry.
     */
    private static final String RECORD_AUTH_FAILURE_SCRIPT =
            "/org/apache/guacamole/cluster/redis/record-auth-failure.lua";

    /**
     * The Lettuce client from which the command and pub/sub connections are
     * created. Creating the client does not itself connect to Redis.
     */
    private final RedisClient client;

    /**
     * The connection used for every ordinary Redis command, or null if no
     * connection has been established yet. Established on first use rather than in the constructor. Connecting
     * eagerly means a replica that starts while Redis is unreachable fails to
     * build its Guice injector at all, which takes the whole authentication
     * provider down with it -- the replica is then dead rather than degraded.
     */
    private volatile StatefulRedisConnection<String, String> connection;

    /**
     * The time at which the store last failed, in milliseconds since the
     * epoch, so that an unavailable store is re-probed after
     * RETRY_INTERVAL_MS rather than written off for the lifetime of the
     * replica.
     */
    private volatile long unavailableSince = 0L;

    /**
     * The dedicated connection on which this replica receives every cluster
     * broadcast (kills, share key revocations, and logouts), or null if it
     * has not yet subscribed. Pub/sub cannot share the command connection: a
     * subscribed Redis connection accepts only subscription commands.
     */
    private volatile StatefulRedisPubSubConnection<String, String> pubSubConnection;

    /**
     * Handler invoked when a kill is requested anywhere in the cluster, or null
     * if this replica has not registered one.
     */
    private volatile ClusterKillHandler killHandler;

    /**
     * Handler invoked when a share key is revoked anywhere in the cluster, or
     * null if this replica has not registered one.
     */
    private volatile ClusterShareRevocationHandler shareRevocationHandler;

    /**
     * Handler invoked when a session is logged out anywhere in the cluster, or
     * null if this replica has not registered one.
     */
    private volatile ClusterLogoutHandler logoutHandler;

    /**
     * The seat acquisition script, read from the classpath when this store is
     * created so that a missing script is reported at startup.
     */
    private final LuaScript acquireSeats = LuaScript.load(ACQUIRE_SEATS_SCRIPT);

    /**
     * The authentication failure script, read from the classpath when this
     * store is created so that a missing script is reported at startup.
     */
    private final LuaScript recordAuthFailure = LuaScript.load(RECORD_AUTH_FAILURE_SCRIPT);

    /**
     * The number of milliseconds after which a tunnel, route, or seat which
     * has not been refreshed by a heartbeat is considered dead. Used both as
     * the TTL of per-tunnel keys and as the score cutoff for sorted sets.
     */
    private final long staleWindowMs;

    /**
     * The identity of this replica within the cluster.
     */
    private final String nodeId;

    /**
     * Whether the most recent Redis operation succeeded. Starts true so that
     * Redis is tried before any failure has been observed.
     */
    private volatile boolean available = true;

    /**
     * Creates a store which connects to the given Redis server using the
     * JVM's default trust for any TLS connection. No connection is made until
     * the store is first used.
     *
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
        this(redisUri, staleWindowMs, nodeId, null);
    }

    /**
     * Creates a store which verifies the Redis server against a private
     * certificate authority.
     *
     * @param redisUri
     *     The Lettuce URI of the Redis server.
     *
     * @param staleWindowMs
     *     Milliseconds after which an unrefreshed entry is considered dead.
     *
     * @param nodeId
     *     The identity of this replica.
     *
     * @param caCertificatePath
     *     Path to a PEM certificate authority the Redis server's certificate
     *     is signed by, or null to use the JVM's default trust.
     */
    public RedisClusterStore(String redisUri, long staleWindowMs, String nodeId,
            String caCertificatePath) {

        // The timeout belongs on the URI; AbstractRedisClient.setDefaultTimeout
        // is deprecated, and this module compiles with -Werror.
        RedisURI parsedUri = RedisURI.create(redisUri);
        parsedUri.setTimeout(Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS));
        this.client = RedisClient.create(parsedUri);

        // Fail fast rather than queue. Lettuce's default is to buffer commands
        // while the connection is down and wait out a long command timeout, so
        // the first connect attempt after Redis becomes unreachable can block
        // for minutes instead of degrading. Rejecting immediately is what
        // makes the documented fallback to per-replica limits reachable.
        ClientOptions.Builder options = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);

        // Trust is scoped to this client rather than added to the JVM's
        // default truststore, which also governs every other outbound HTTPS
        // call the deployment makes -- SAML identity provider metadata
        // included.
        if (caCertificatePath != null && !caCertificatePath.isEmpty()) {

            File caCertificate = new File(caCertificatePath);
            if (!caCertificate.isFile() || !caCertificate.canRead())
                throw new IllegalArgumentException("The configured Redis "
                        + "certificate authority \"" + caCertificatePath + "\" is "
                        + "not a readable file. Clustering would fail every "
                        + "connection rather than start insecurely.");

            options.sslOptions(SslOptions.builder()
                    .trustManager(caCertificate)
                    .build());

        }

        client.setOptions(options.build());

        this.staleWindowMs = staleWindowMs;
        this.nodeId = nodeId;
    }

    /**
     * Returns the command interface, connecting if necessary.
     *
     * @return
     *     The synchronous command interface.
     *
     * @throws RedisException
     *     If a connection cannot be established.
     */
    private RedisCommands<String, String> commands() {

        // Fast path: reuse the established connection without locking
        StatefulRedisConnection<String, String> current = connection;
        if (current != null && current.isOpen())
            return current.sync();

        // Slow path: connect under the lock, re-checking first so that
        // concurrent callers do not each open a connection
        synchronized (this) {

            if (connection != null && connection.isOpen())
                return connection.sync();

            // Throws RedisException if Redis is unreachable; the caller's
            // catch block then records the store as unavailable
            connection = client.connect();
            return connection.sync();

        }

    }

    @Override
    public SeatResult acquireSeats(SeatRequest request) throws GuacamoleException {

        List<SeatKey> seatKeys = request.getKeys();

        // Translate the request into the script's KEYS and ARGV arrays. The
        // order of the keys is significant: the script reports a refusal as
        // the 1-based position of the key whose limit was reached
        String[] keys = new String[seatKeys.size()];
        String[] args = new String[seatKeys.size() + 2];

        args[0] = request.getTunnelUuid();
        args[1] = Long.toString(staleWindowMs);

        for (int i = 0; i < seatKeys.size(); i++) {
            keys[i] = seatKeys.get(i).getRedisKey();
            args[i + 2] = Integer.toString(seatKeys.get(i).getLimit());
        }

        // Check every limit and claim every seat in one atomic script, so
        // that two replicas cannot both take the last seat
        try {

            long result = acquireSeats.eval(commands(), keys, args);
            available = true;

            if (result == 0) {
                ClusterMetrics.counter("guacamole_cluster_seat_acquisitions_total",
                        "result", "SUCCESS");
                return SeatResult.SUCCESS;
            }

            // Map the position of the refusing key back to its failure result
            SeatResult failure = seatKeys.get((int) result - 1).getFailureResult();
            ClusterMetrics.counter("guacamole_cluster_seat_acquisitions_total",
                    "result", failure.name());
            return failure;

        }
        // Fail closed: the caller cannot tell whether a seat is free, so it
        // falls back to enforcing limits with replica-local state
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "acquireSeats");
            available = false;
            unavailableSince = System.currentTimeMillis();
            throw new GuacamoleServerException("Unable to acquire cluster seats.", e);
        }

    }

    @Override
    public void releaseSeats(SeatRequest request) throws GuacamoleException {

        try {

            // Remove the tunnel from every limit set it holds a seat in. Not
            // atomic, and need not be: a seat left behind by a partial
            // failure is pruned once it is older than the stale window
            RedisCommands<String, String> commands = commands();
            for (SeatKey key : request.getKeys())
                commands.zrem(key.getRedisKey(), request.getTunnelUuid());

            available = true;

        }
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "releaseSeats");
            available = false;
            unavailableSince = System.currentTimeMillis();
            throw new GuacamoleServerException("Unable to release cluster seats.", e);
        }

    }

        /**
     * Returns the per-user seat keys a tunnel may hold, as written by the
     * seat script for "max-connections-per-user" limits. Unlike the index
     * keys, a seat is only present when a per-user limit applied at connect
     * time, so callers must not create one that is absent.
     *
     * @param registration
     *     The tunnel whose per-user seat keys should be returned.
     *
     * @return
     *     The per-user seat key for the tunnel's connection, plus the one for
     *     its balancing group if it has one.
     */
    private List<String> seatKeys(TunnelRegistration registration) {

        List<String> keys = new ArrayList<String>(2);
        keys.add(ClusterKeys.userConnectionSeat(registration.getUsername(),
                registration.getConnectionIdentifier()));

        if (registration.getGroupIdentifier() != null)
            keys.add(ClusterKeys.userGroupSeat(registration.getUsername(),
                    registration.getGroupIdentifier()));

        return keys;

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
     * Returns the current time according to the Redis server. Every replica
     * scores and compares heartbeats using this clock rather than its own, so
     * that clock drift between replicas cannot make a live tunnel look stale
     * or a dead one look live.
     *
     * @param commands
     *     The synchronous Redis command interface to query.
     *
     * @return
     *     The current time according to the Redis server, in milliseconds
     *     since the epoch.
     *
     * @throws RedisException
     *     If the time cannot be read from the Redis server.
     */
    private long serverTimeMillis(RedisCommands<String, String> commands) {
        // TIME returns whole seconds and the microseconds within that second
        List<String> time = commands.time();
        return (Long.parseLong(time.get(0)) * 1000L)
                + (Long.parseLong(time.get(1)) / 1000L);
    }

    @Override
    public void registerTunnel(TunnelRegistration registration) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();
            long now = serverTimeMillis(commands);

            // Describe the tunnel in a hash, omitting optional fields which
            // are absent, as Redis hashes cannot hold null values
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

            if (registration.getRecordUuid() != null)
                record.put("recordUuid", registration.getRecordUuid());

            // Store the hash with a TTL of one stale window. The heartbeat
            // extends it; if this replica dies the hash simply expires
            String tunnelKey = ClusterKeys.tunnel(registration.getTunnelUuid());
            commands.hset(tunnelKey, record);
            commands.pexpire(tunnelKey, staleWindowMs);

            // Add the tunnel to each index, scored by server time so that
            // readers can filter out members which stopped heartbeating
            for (String key : indexKeys(registration))
                commands.zadd(key, (double) now, registration.getTunnelUuid());

            // Record which guacd instance hosts the connection, so that a
            // join arriving at any replica is sent to the same instance
            if (registration.getGuacdConnectionId() != null) {
                String routeKey = ClusterKeys.route(registration.getGuacdConnectionId());
                commands.psetex(routeKey, staleWindowMs, registration.getEndpoint().toKey());
            }

            // Lets a kill addressed by history record UUID find the cluster
            // state, which is keyed by seat token
            if (registration.getRecordUuid() != null)
                commands.psetex(ClusterKeys.record(registration.getRecordUuid()),
                        staleWindowMs, registration.getTunnelUuid());

            available = true;

        }
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "registerTunnel");
            available = false;
            unavailableSince = System.currentTimeMillis();
            // Fail closed: the caller decides how to proceed without cluster
            // visibility of this tunnel
            throw new GuacamoleServerException("Unable to register tunnel with cluster.", e);
        }

    }

    @Override
    public void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();

            // Remove every trace registerTunnel() wrote. Anything missed by a
            // failure part-way through ages out after the stale window, as
            // nothing will heartbeat it again
            for (String key : indexKeys(registration))
                commands.zrem(key, registration.getTunnelUuid());

            commands.del(ClusterKeys.tunnel(registration.getTunnelUuid()));

            if (registration.getGuacdConnectionId() != null)
                commands.del(ClusterKeys.route(registration.getGuacdConnectionId()));

            if (registration.getRecordUuid() != null)
                commands.del(ClusterKeys.record(registration.getRecordUuid()));

            available = true;

        }
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "unregisterTunnel");
            available = false;
            unavailableSince = System.currentTimeMillis();
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

            // For each of this replica's tunnels, re-score its index entries
            // with the current server time and push back the expiry of its
            // per-tunnel keys by another stale window. ZADD on an existing
            // member only updates its score
            for (TunnelRegistration registration : registrations) {

                for (String key : indexKeys(registration))
                    commands.zadd(key, (double) now, registration.getTunnelUuid());

                // Refresh per-user seats too, or the seat script prunes them
                // as stale. XX only updates a seat that was actually taken
                for (String key : seatKeys(registration))
                    commands.zadd(key, ZAddArgs.Builder.xx(), (double) now,
                            registration.getTunnelUuid());

                commands.pexpire(ClusterKeys.tunnel(registration.getTunnelUuid()), staleWindowMs);

                if (registration.getGuacdConnectionId() != null)
                    commands.pexpire(ClusterKeys.route(registration.getGuacdConnectionId()),
                            staleWindowMs);

                if (registration.getRecordUuid() != null)
                    commands.pexpire(ClusterKeys.record(registration.getRecordUuid()),
                            staleWindowMs);

            }

            available = true;

        }
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "heartbeat");
            available = false;
            unavailableSince = System.currentTimeMillis();
            throw new GuacamoleServerException("Unable to refresh cluster heartbeat.", e);
        }

    }

    @Override
    public GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException {

        try {

            // A missing route means the hosting replica stopped heartbeating
            // the connection, or it was never registered
            String value = commands().get(ClusterKeys.route(guacdConnectionId));
            available = true;

            if (value == null)
                return null;

            return GuacdEndpoint.fromKey(value);

        }
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "lookupRoute");
            available = false;
            unavailableSince = System.currentTimeMillis();
            // Fail closed: guessing an instance would open a new session
            // rather than join the intended one
            throw new GuacamoleServerException("Unable to look up guacd route.", e);
        }

    }

    @Override
    public long countTunnels(GuacdEndpoint endpoint) {

        try {

            RedisCommands<String, String> commands = commands();
            // Count only members heartbeated within the stale window, so that
            // tunnels left by a dead replica do not inflate the load
            long cutoff = serverTimeMillis(commands) - staleWindowMs;

            Long count = commands.zcount(ClusterKeys.guacdIndex(endpoint.toKey()),
                    io.lettuce.core.Range.from(
                            io.lettuce.core.Range.Boundary.excluding((double) cutoff),
                            io.lettuce.core.Range.Boundary.unbounded()));

            available = true;
            return count == null ? 0L : count;

        }
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "countTunnels");
            available = false;
            unavailableSince = System.currentTimeMillis();
            // Fail open: load balancing degrades to a random choice rather
            // than refusing new connections
            logger.warn("Unable to count tunnels for guacd \"{}\". Treating as unloaded.",
                    endpoint, e);
            return 0L;
        }

    }

    @Override
    public Collection<TunnelRegistration> listTunnels() {

        List<TunnelRegistration> tunnels = new ArrayList<TunnelRegistration>();

        try {

            RedisCommands<String, String> commands = commands();
            long cutoff = serverTimeMillis(commands) - staleWindowMs;

            // Score-filtered, so members left behind by a dead replica are
            // never listed even before anything prunes them
            List<String> seatTokens = commands.zrangebyscore(ClusterKeys.ALL_INDEX,
                    io.lettuce.core.Range.from(
                            io.lettuce.core.Range.Boundary.excluding((double) cutoff),
                            io.lettuce.core.Range.Boundary.unbounded()));

            // Read each tunnel's hash. This is one round trip per tunnel,
            // acceptable for an administrative view
            for (String seatToken : seatTokens) {

                Map<String, String> record = commands.hgetall(ClusterKeys.tunnel(seatToken));

                // The hash expires on its own TTL, so a member can outlive it
                if (record.isEmpty())
                    continue;

                String endpointKey = record.get("guacdEndpoint");
                String startTime = record.get("startTime");

                tunnels.add(new TunnelRegistration(
                        seatToken,
                        record.get("nodeId"),
                        record.get("guacdConnectionId"),
                        endpointKey != null ? GuacdEndpoint.fromKey(endpointKey) : null,
                        record.get("connIdentifier"),
                        record.get("groupIdentifier"),
                        record.get("sharingProfileId"),
                        record.get("username"),
                        record.get("remoteHost"),
                        startTime != null ? Long.parseLong(startTime) : 0L,
                        record.get("recordUuid")));

            }

            available = true;

        }

        // Fail open: the administrative view degrades to replica-local rather
        // than failing outright
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "listTunnels");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to list cluster tunnels. The active connection "
                    + "view will show only this replica's sessions.", e);
        }

        return tunnels;

    }

    @Override
    public String lookupSeatToken(String recordUuid) {

        try {
            // Resolve the history record UUID through the mapping written by
            // registerTunnel()
            String token = commands().get(ClusterKeys.record(recordUuid));
            available = true;
            return token;
        }

        // The caller degrades to a replica-local view rather than failing
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "lookupSeatToken");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to resolve record \"{}\" to a seat token.", recordUuid, e);
            return null;
        }

    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean isClustered() {
        return true;
    }

    @Override
    public boolean isAvailable() {

        // The flag is maintained by every operation: set on success, cleared
        // on failure
        if (available)
            return true;

        // Re-probe periodically. Without this a replica that saw one failure
        // would use replica-local limits forever, even after Redis returned.
        return System.currentTimeMillis() - unavailableSince > RETRY_INTERVAL_MS;

    }

    @Override
    public void onKillRequest(ClusterKillHandler handler) {
        this.killHandler = handler;
        subscribeIfNeeded();
    }

    @Override
    public void onShareRevoked(ClusterShareRevocationHandler handler) {
        this.shareRevocationHandler = handler;
        subscribeIfNeeded();
    }

    @Override
    public void onLogout(ClusterLogoutHandler handler) {
        this.logoutHandler = handler;
        subscribeIfNeeded();
    }

    /**
     * Opens the single pub/sub connection this replica uses for every cluster
     * message, subscribing to each channel. Kills, share key revocations, and
     * logouts all arrive here, dispatched by channel name, so registering a
     * second handler does not open a second connection. Handlers are read at
     * delivery time, so a handler registered after the subscription was made
     * still receives subsequent messages.
     *
     * If the subscription cannot be made, the failure is logged and this
     * replica continues without receiving broadcasts. The subscription is
     * attempted again the next time a handler is registered.
     */
    private synchronized void subscribeIfNeeded() {

        // Already subscribed to every channel
        if (pubSubConnection != null)
            return;

        try {

            // A separate connection, as a subscribed connection can issue
            // only subscription commands
            StatefulRedisPubSubConnection<String, String> pubSub = client.connectPubSub();

            // Route each message to the handler for its channel. Pub/sub is
            // fire-and-forget: a replica that is disconnected when a message
            // is published never receives it

            pubSub.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String payload) {
                    try {

                        if (ClusterKeys.KILL_CHANNEL.equals(channel)) {
                            ClusterKillHandler handler = killHandler;
                            if (handler != null)
                                handler.killLocalTunnel(payload);
                        }

                        else if (ClusterKeys.SHARE_REVOKE_CHANNEL.equals(channel)) {
                            ClusterShareRevocationHandler handler = shareRevocationHandler;
                            if (handler != null)
                                handler.shareRevoked(payload);
                        }

                        else if (ClusterKeys.LOGOUT_CHANNEL.equals(channel)) {
                            ClusterLogoutHandler handler = logoutHandler;
                            if (handler != null)
                                handler.loggedOut(payload);
                        }

                    }

                    // A handler that throws must not kill the subscriber
                    catch (Throwable e) {
                        logger.warn("Cluster message on \"{}\" could not be handled.",
                                channel, e);
                    }
                }

            });

            // Publish the connection only once subscribed, so that a failed
            // subscription is retried by the next handler registration
            pubSub.sync().subscribe(ClusterKeys.KILL_CHANNEL,
                    ClusterKeys.SHARE_REVOKE_CHANNEL,
                    ClusterKeys.LOGOUT_CHANNEL);
            pubSubConnection = pubSub;

        }

        // Without a subscription this replica simply never honours remote kills
        // or revocations; it must still serve connections
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "subscribeIfNeeded");
            logger.error("Unable to subscribe to cluster messages. Sessions on "
                    + "this replica cannot be terminated from another one, and "
                    + "share keys revoked elsewhere stay open here.", e);
        }

    }

    @Override
    public void publishShareRevocation(String shareKey) {

        try {
            // Every replica, including this one, receives the revocation and
            // closes any tunnel it opened from this share key
            commands().publish(ClusterKeys.SHARE_REVOKE_CHANNEL, shareKey);
            available = true;
        }

        // The key is already gone from the cluster, so it can no longer be
        // redeemed; only the closing of tunnels already opened from it is lost
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "publishShareRevocation");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to announce revocation of a share key. Tunnels "
                    + "opened from it on other replicas will survive until "
                    + "their session ends.", e);
        }

    }

    @Override
    public void requestKill(String recordUuid) throws GuacamoleException {

        try {
            // The replica hosting the tunnel recognises the record UUID and
            // closes it; every other replica ignores the message
            commands().publish(ClusterKeys.KILL_CHANNEL, recordUuid);
            available = true;
        }

        // Fail closed: the caller must report that the kill was not sent
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "requestKill");
            available = false;
            unavailableSince = System.currentTimeMillis();
            throw new GuacamoleServerException("Unable to request cluster kill.", e);
        }

    }

    @Override
    public boolean isTunnelLive(String seatToken) {

        try {
            // The tunnel hash exists only while its replica keeps it alive
            // by heartbeat, and is deleted when the tunnel closes
            boolean live = commands().exists(ClusterKeys.tunnel(seatToken)) > 0;
            available = true;
            return live;
        }

        // Reporting "gone" would claim a kill landed when that is unknown;
        // reporting "live" makes the caller wait out its own timeout and
        // report failure, which is the honest answer
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "isTunnelLive");
            available = false;
            unavailableSince = System.currentTimeMillis();
            return true;
        }

    }

    @Override
    public void putShareKey(String shareKey, SharedConnectionEntry entry) {

        try {

            // Store the share key as a hash so that any replica can redeem
            // it. The hash carries no expiry; it is deleted by
            // removeShareKey()
            Map<String, String> record = new HashMap<String, String>();
            record.put("seatToken", entry.getSeatToken());
            record.put("guacdConnectionId", entry.getGuacdConnectionId());
            record.put("connIdentifier", entry.getConnectionIdentifier());
            record.put("sharedBy", entry.getSharedBy());

            if (entry.getSharingProfileIdentifier() != null)
                record.put("sharingProfileId", entry.getSharingProfileIdentifier());

            commands().hset(ClusterKeys.shareKey(shareKey), record);
            available = true;

        }

        // A share key that cannot be stored simply never works elsewhere, which
        // is how an expired key already behaves. It must not fail the share.
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "putShareKey");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to publish share key to the cluster. It will "
                    + "work only on this replica.", e);
        }

    }

    @Override
    public SharedConnectionEntry getShareKey(String shareKey) {

        try {

            Map<String, String> record = commands().hgetall(ClusterKeys.shareKey(shareKey));
            available = true;

            if (record.isEmpty())
                return null;

            return new SharedConnectionEntry(
                    record.get("seatToken"),
                    record.get("guacdConnectionId"),
                    record.get("connIdentifier"),
                    record.get("sharingProfileId"),
                    record.get("sharedBy"));

        }

        // An unreadable key is an invalid key, which is how an expired key
        // already behaves
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "getShareKey");
            available = false;
            unavailableSince = System.currentTimeMillis();
            return null;
        }

    }

    @Override
    public void removeShareKey(String shareKey) {

        try {
            commands().del(ClusterKeys.shareKey(shareKey));
            available = true;
        }

        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "removeShareKey");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to remove share key from the cluster. It will "
                    + "stop working when its session ends.", e);
        }

    }

    @Override
    public List<String> selfCheck() {

        // Exercise each class of command this store depends on, recording a
        // description of every one which fails (typically because a Redis ACL
        // does not grant it) rather than stopping at the first failure. All
        // probe keys share one key under the "guac:" prefix, so the check
        // also verifies the ACL's key pattern
        List<String> failures = new ArrayList<String>();
        String key = ClusterKeys.selfCheckProbe();

        // TIME is checked first, and separately, because it is easily omitted
        // from an ACL: it belongs to Redis's @fast class, which none of the
        // data-type classes include, and both the seat script and
        // countTunnels depend on it.
        try {
            commands().time();
        }
        catch (RedisException e) {
            failures.add("server clock (TIME): " + e.getMessage());
        }

        try {
            commands().set(key, "probe");
            commands().get(key);
        }
        catch (RedisException e) {
            failures.add("string read/write (SET, GET): " + e.getMessage());
        }

        try {
            commands().expire(key, 60);
            commands().ttl(key);
        }
        catch (RedisException e) {
            failures.add("expiry (EXPIRE, TTL): " + e.getMessage());
        }

        try {
            commands().hset(key + ":h", "field", "value");
            commands().hgetall(key + ":h");
        }
        catch (RedisException e) {
            failures.add("hash read/write (HSET, HGETALL): " + e.getMessage());
        }

        try {
            commands().zadd(key + ":z", 1.0, "member");
            commands().zcount(key + ":z", io.lettuce.core.Range.from(
                    io.lettuce.core.Range.Boundary.excluding(0.0),
                    io.lettuce.core.Range.Boundary.unbounded()));
            commands().zrem(key + ":z", "member");
        }
        catch (RedisException e) {
            failures.add("sorted sets (ZADD, ZCOUNT, ZREM): " + e.getMessage());
        }

        try {
            commands().publish(ClusterKeys.KILL_CHANNEL, "selfcheck");
        }
        catch (RedisException e) {
            failures.add("publish (PUBLISH): " + e.getMessage());
        }

        // Exercises SCRIPT LOAD and EVALSHA together, the key pattern the ACL
        // applies to script keys, and TIME from inside a script, where a
        // missing TIME permission surfaces as a script error
        try {
            acquireSeats.eval(commands(), new String[] { key + ":z" },
                    new String[] { "selfcheck", Long.toString(staleWindowMs), "1" });
        }
        catch (RedisException e) {
            failures.add("scripting (SCRIPT LOAD, EVALSHA): " + e.getMessage());
        }

        // Remove every probe key so that the check leaves nothing behind
        try {
            commands().del(key, key + ":h", key + ":z");
        }
        catch (RedisException e) {
            failures.add("delete (DEL): " + e.getMessage());
        }

        return failures;

    }

    /**
     * Returns how many of this check's probe keys remain. Intended only for
     * tests.
     *
     * @return
     *     The number of probe keys still present.
     */
    public long probeKeyCountForTesting() {
        String key = ClusterKeys.selfCheckProbe();
        return commands().exists(key, key + ":h", key + ":z");
    }

    /**
     * Deletes every key in every database of the Redis server, including keys
     * not belonging to Guacamole. Intended only for tests run against a
     * dedicated, disposable Redis server.
     */
    public void flushForTesting() {
        commands().flushall();
    }

    @Override
    public int recordAuthenticationFailure(String address, int banDurationSeconds) {

        try {
            // Increment and re-arm the expiry atomically, so the counter can
            // never be left without an expiry
            long count = recordAuthFailure.eval(commands(),
                    new String[] { ClusterKeys.authFailure(address) },
                    new String[] { Integer.toString(banDurationSeconds) });
            available = true;
            return (int) count;
        }

        // -1 is "unknown", never "none": reporting zero here would unban every
        // address for as long as Redis is unreachable
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "recordAuthenticationFailure");
            available = false;
            unavailableSince = System.currentTimeMillis();
            return -1;
        }

    }

    @Override
    public int getAuthenticationFailures(String address) {

        String value;

        try {
            value = commands().get(ClusterKeys.authFailure(address));
            available = true;
        }

        // Fail to "unknown" (-1), never to zero, so that the caller falls
        // back to replica-local tracking rather than unbanning the address
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "getAuthenticationFailures");
            available = false;
            unavailableSince = System.currentTimeMillis();
            return -1;
        }

        // An absent counter has expired, or the address has never failed
        if (value == null)
            return 0;

        try {
            return Integer.parseInt(value);
        }

        // A counter that is not a number is corrupt rather than absent, so the
        // caller is routed to its fallback rather than told there are none
        catch (NumberFormatException e) {
            logger.warn("Authentication failure counter for \"{}\" is not a "
                    + "number. Falling back to replica-local tracking.", address);
            return -1;
        }

    }

    /**
     * Returns the remaining lifetime of an address's failure counter, in
     * seconds. Intended only for tests.
     *
     * @param address
     *     The address whose counter should be examined.
     *
     * @return
     *     The remaining lifetime of the counter, in seconds.
     */
    public long authFailureTtlForTesting(String address) {
        return commands().ttl(ClusterKeys.authFailure(address));
    }

    @Override
    public void putToken(String tokenHash, TokenIdentity identity, int timeoutSeconds) {

        try {

            Map<String, String> record = new HashMap<String, String>();
            record.put("username", identity.getUsername());
            record.put("authProvider", identity.getAuthProviderIdentifier());
            record.put("authenticatedTime",
                    Long.toString(identity.getAuthenticatedTime()));

            if (identity.getRemoteAddress() != null)
                record.put("remoteAddress", identity.getRemoteAddress());

            if (identity.getRemoteHostname() != null)
                record.put("remoteHostname", identity.getRemoteHostname());

            // Store the token under its hash, never the token itself, with an
            // expiry equal to the session timeout, which touchToken()
            // extends
            String key = ClusterKeys.token(tokenHash);
            commands().hset(key, record);
            commands().expire(key, timeoutSeconds);
            available = true;

        }

        // A token that cannot be published simply does not survive the loss of
        // this replica, which is the pre-cluster behaviour
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "putToken");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to publish session token to the cluster. This "
                    + "session will not survive the loss of this replica.", e);
        }

    }

    @Override
    public TokenIdentity getToken(String tokenHash) {

        try {

            Map<String, String> record = commands().hgetall(ClusterKeys.token(tokenHash));
            available = true;

            if (record.isEmpty())
                return null;

            return new TokenIdentity(
                    record.get("username"),
                    record.get("authProvider"),
                    record.get("remoteAddress"),
                    record.get("remoteHostname"),
                    Long.parseLong(record.get("authenticatedTime")));

        }

        // An unreadable token is simply not rehydratable, which is how an
        // expired token already behaves
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "getToken");
            available = false;
            unavailableSince = System.currentTimeMillis();
            return null;
        }

        catch (NumberFormatException e) {
            logger.warn("Session token record is corrupt and will be ignored.", e);
            return null;
        }

    }

    @Override
    public void removeToken(String tokenHash) {

        try {
            commands().del(ClusterKeys.token(tokenHash));
            available = true;
        }

        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "removeToken");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to remove session token from the cluster. It "
                    + "will expire on its own.", e);
        }

    }

    @Override
    public void touchToken(String tokenHash, int timeoutSeconds) {

        try {
            // Restart the idle timeout. EXPIRE on a missing key does nothing,
            // so a token removed elsewhere is not resurrected
            commands().expire(ClusterKeys.token(tokenHash), timeoutSeconds);
            available = true;
        }

        // Losing one refresh only shortens the idle window for this session
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "touchToken");
            available = false;
            unavailableSince = System.currentTimeMillis();
        }

    }

    /**
     * Returns the remaining lifetime of a session token, in seconds. Intended
     * only for tests.
     *
     * @param tokenHash
     *     The hash of the token to examine.
     *
     * @return
     *     The remaining lifetime of the token, in seconds.
     */
    public long tokenTtlForTesting(String tokenHash) {
        return commands().ttl(ClusterKeys.token(tokenHash));
    }

    @Override
    public void publishLogout(String tokenHash) {

        try {
            // Every replica which rebuilt a session from this token drops it
            commands().publish(ClusterKeys.LOGOUT_CHANNEL, tokenHash);
            available = true;
        }

        // The token is already gone from the cluster, so it can no longer be
        // rebuilt; only the dropping of sessions already rebuilt is lost
        catch (RedisException e) {
            ClusterMetrics.counter("guacamole_cluster_operation_failures_total",
                    "operation", "publishLogout");
            available = false;
            unavailableSince = System.currentTimeMillis();
            logger.warn("Unable to announce logout. A session rebuilt from this "
                    + "token on another replica will survive until it times "
                    + "out.", e);
        }

    }

    /**
     * Publishes this store's current state as gauges. Called immediately
     * before the metrics endpoint renders, so that a gauge is never staler
     * than the scrape that reads it.
     */
    @Override
    public void publishMetrics() {
        ClusterMetrics.gauge("guacamole_cluster_store_available", available ? 1 : 0);
        ClusterMetrics.gauge("guacamole_cluster_enabled", 1);
    }

    @Override
    public void shutdown() {

        // Close the subscription and command connections before shutting
        // down the client which owns their underlying resources
        StatefulRedisPubSubConnection<String, String> pubSub = pubSubConnection;
        if (pubSub != null)
            pubSub.close();

        StatefulRedisConnection<String, String> current = connection;
        if (current != null)
            current.close();

        client.shutdown();

    }

}
