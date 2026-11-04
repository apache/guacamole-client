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

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.Collections;
import java.util.Arrays;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.SeatKey;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatResult;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.DockerClientFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests tunnel registration, routing, load counting, heartbeat and
 * unregistration in {@link RedisClusterStore}, which together let any replica
 * find the guacd instance serving a connection and spread new connections by
 * load. Runs against a real Redis started by Testcontainers, and is skipped
 * when Docker is unavailable.
 */
public class TunnelRegistrationTest {

    /**
     * Skips this class rather than failing it when no Docker daemon is
     * reachable. The image build runs the full test suite inside a container
     * that has no daemon of its own, and a Redis-backed test cannot be
     * meaningfully run there.
     */
    @BeforeAll
    public static void requireDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Redis-backed tests.");
    }


    /**
     * The stale window given to the store, in milliseconds. Index members
     * whose score is older than this are treated as left behind by a dead
     * replica.
     */
    private static final long STALE_WINDOW_MS = 30000L;

    /**
     * The first of two guacd instances tunnels are registered against.
     */
    private static final GuacdEndpoint GUACD_A =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    /**
     * The second of two guacd instances tunnels are registered against, used
     * to verify that load is counted per instance.
     */
    private static final GuacdEndpoint GUACD_B =
            new GuacdEndpoint("guacd-b", 4822, EncryptionMethod.NONE);

    /**
     * The raw Lettuce client used to inspect and manipulate Redis directly,
     * independently of the store under test.
     */
    private RedisClient client;

    /**
     * The raw connection used to inspect and manipulate Redis directly,
     * independently of the store under test.
     */
    private StatefulRedisConnection<String, String> connection;

    /**
     * The store under test, acting as replica "node-1".
     */
    private RedisClusterStore store;

    /**
     * Opens a raw connection to the shared Redis container, flushes it so
     * that no state leaks between tests, and creates a fresh store under
     * test.
     */
    @BeforeEach
    public void setUp() {
        client = RedisClient.create(RedisTestSupport.redisUri());
        connection = client.connect();
        connection.sync().flushall();
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "node-1");
    }

    /**
     * Shuts down the store under test and closes the raw connection and
     * client, releasing every connection opened by {@link #setUp()}.
     */
    @AfterEach
    public void tearDown() {
        store.shutdown();
        connection.close();
        client.shutdown();
    }

    /**
     * Returns a registration for a tunnel owned by this replica ("node-1"),
     * established for user "alice" on connection "conn-1" through balancing
     * group "group-1", with a history record UUID derived from the seat
     * token.
     *
     * @param uuid
     *     The seat token (tunnel UUID) of the tunnel.
     *
     * @param endpoint
     *     The guacd instance serving the tunnel.
     *
     * @param guacdConnectionId
     *     The connection ID assigned to the tunnel by guacd.
     *
     * @return
     *     A new registration for the described tunnel.
     */
    private TunnelRegistration registration(String uuid, GuacdEndpoint endpoint,
            String guacdConnectionId) {
        return new TunnelRegistration(uuid, "node-1", guacdConnectionId, endpoint,
                "conn-1", "group-1", null, "alice", "10.0.0.5",
                System.currentTimeMillis(), "record-" + uuid);
    }

    /**
     * Verifies that, once registered, a tunnel's guacd connection ID resolves
     * to the guacd instance serving it, which is how a join from another
     * replica reaches the right guacd.
     *
     * @throws Exception
     *     If the tunnel cannot be registered or the route looked up.
     */
    @Test
    public void registeredTunnelIsRoutableByGuacdConnectionId() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));
        assertEquals(GUACD_A, store.lookupRoute("$abc"));

    }

    /**
     * Verifies that looking up a guacd connection ID that was never
     * registered returns null rather than failing.
     *
     * @throws Exception
     *     If the route cannot be looked up.
     */
    @Test
    public void unknownRouteReturnsNull() throws Exception {
        assertNull(store.lookupRoute("$nonexistent"));
    }

    /**
     * Verifies that registration adds the tunnel to the cluster-wide, per-user
     * and per-guacd indexes.
     *
     * @throws Exception
     *     If the tunnel cannot be registered.
     */
    @Test
    public void registeredTunnelAppearsInEveryIndex() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));

        assertEquals(1L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
        assertEquals(1L, connection.sync().zcard(ClusterKeys.userIndex("alice")));
        assertEquals(1L, connection.sync().zcard(ClusterKeys.guacdIndex(GUACD_A.toKey())));

    }

    /**
     * Verifies that registration stores the tunnel's details in its hash,
     * including the owning replica, connection identifier and guacd
     * endpoint.
     *
     * @throws Exception
     *     If the tunnel cannot be registered.
     */
    @Test
    public void storesTheFullTunnelRecord() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));

        assertEquals("node-1", connection.sync().hget(ClusterKeys.tunnel("t1"), "nodeId"));
        assertEquals("conn-1",
                connection.sync().hget(ClusterKeys.tunnel("t1"), "connIdentifier"));
        assertEquals(GUACD_A.toKey(),
                connection.sync().hget(ClusterKeys.tunnel("t1"), "guacdEndpoint"));

    }

    /**
     * Verifies that live tunnels are counted separately for each guacd
     * instance, which is the load figure used to select a guacd.
     *
     * @throws Exception
     *     If a tunnel cannot be registered.
     */
    @Test
    public void countsLiveTunnelsPerGuacdInstance() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$a1"));
        store.registerTunnel(registration("t2", GUACD_A, "$a2"));
        store.registerTunnel(registration("t3", GUACD_B, "$b1"));

        assertEquals(2L, store.countTunnels(GUACD_A));
        assertEquals(1L, store.countTunnels(GUACD_B));

    }

    /**
     * Verifies that a per-guacd index member whose score is outside the stale
     * window is not counted as load, so a guacd is not avoided because of
     * tunnels belonging to a dead replica.
     *
     * @throws Exception
     *     If the tunnel cannot be registered.
     */
    @Test
    public void ignoresStaleTunnelsWhenCounting() throws Exception {

        String key = ClusterKeys.guacdIndex(GUACD_A.toKey());
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(key, (double) ancient, "dead-tunnel");

        store.registerTunnel(registration("t1", GUACD_A, "$a1"));

        assertEquals(1L, store.countTunnels(GUACD_A));

    }

    /**
     * Verifies that unregistering a tunnel removes its route, its index
     * membership and its contribution to guacd load.
     *
     * @throws Exception
     *     If the tunnel cannot be registered or unregistered.
     */
    @Test
    public void unregisterRemovesTunnelAndRoute() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);
        store.unregisterTunnel(record);

        assertNull(store.lookupRoute("$abc"));
        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
        assertEquals(0L, store.countTunnels(GUACD_A));

    }

    /**
     * Verifies that unregistering the same tunnel twice neither fails nor
     * leaves anything behind, since teardown may run more than once.
     *
     * @throws Exception
     *     If the tunnel cannot be registered or unregistered.
     */
    @Test
    public void unregisterIsSafeToRepeat() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);
        store.unregisterTunnel(record);
        store.unregisterTunnel(record);

        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));

    }

    /**
     * Verifies that a heartbeat moves the tunnel's index scores forward and
     * keeps its route key from expiring, so a long-lived tunnel is never
     * mistaken for one left behind by a dead replica.
     *
     * @throws Exception
     *     If the tunnel cannot be registered or the heartbeat fails.
     */
    @Test
    public void heartbeatRefreshesScoresAndExpiry() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);

        // Age the entry to just inside the stale window, then heartbeat
        String key = ClusterKeys.guacdIndex(GUACD_A.toKey());
        long aged = System.currentTimeMillis() - (STALE_WINDOW_MS - 1000L);
        connection.sync().zadd(key, (double) aged, "t1");

        store.heartbeat(Collections.singletonList(record));

        Double refreshed = connection.sync().zscore(key, "t1");
        assertEquals(true, refreshed.longValue() > aged,
                "heartbeat must move the score forward");

        // The route key must not be allowed to expire while the tunnel lives
        assertEquals(true, connection.sync().ttl(ClusterKeys.route("$abc")) > 0);

    }

    /**
     * Verifies that a tunnel which first acquires a seat and is then
     * registered under the same seat token occupies a single member of the
     * connection index, rather than counting twice against the connection's
     * limit.
     *
     * @throws Exception
     *     If the seat cannot be acquired or the tunnel registered.
     */
    @Test
    public void registrationAndSeatShareOneMemberPerConnection() throws Exception {

        // A seat taken for a tunnel, then that same tunnel registered, must
        // occupy exactly one member of the connection index -- not two. If
        // these ever diverge, every connection counts double against its own
        // max-connections limit.
        String token = "seat-token-1";

        store.acquireSeats(new SeatRequest(token, Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex("conn-1"), 5,
                        SeatResult.CONNECTION_LIMIT))));

        store.registerTunnel(new TunnelRegistration(token, "node-1", "$abc",
                GUACD_A, "conn-1", null, null, "alice", "10.0.0.5",
                System.currentTimeMillis(), "record-" + token));

        assertEquals(1L, connection.sync().zcard(ClusterKeys.connectionIndex("conn-1")));

    }

    /**
     * Verifies that registration stores the history record UUID and a
     * pointer from that UUID back to the seat token, so a session listed by
     * record UUID can be addressed by a kill.
     *
     * @throws Exception
     *     If the tunnel cannot be registered.
     */
    @Test
    public void recordUuidIsStoredAndResolvesToTheSeatToken() throws Exception {

        // The admin UI identifies a session by its record UUID, but every
        // cluster key is built from the seat token. Without this pointer a
        // remote session could be listed and never killed.
        store.registerTunnel(new TunnelRegistration("seat-1", "node-1", "$abc",
                GUACD_A, "conn-1", null, null, "alice", "10.0.0.5",
                System.currentTimeMillis(), "record-uuid-1"));

        assertEquals("record-uuid-1",
                connection.sync().hget(ClusterKeys.tunnel("seat-1"), "recordUuid"));
        assertEquals("seat-1", store.lookupSeatToken("record-uuid-1"));

    }

    /**
     * Verifies that a tunnel without a history record UUID is still
     * registered and routable, and simply has no record pointer.
     *
     * @throws Exception
     *     If the tunnel cannot be registered or the route looked up.
     */
    @Test
    public void tunnelWithNoRecordUuidIsStillRegistered() throws Exception {

        // getUUID() is null when the history row was never inserted. Such a
        // tunnel still holds seats and still needs its guacd route, so it must
        // register -- it is simply not addressable by the admin UI.
        store.registerTunnel(new TunnelRegistration("seat-2", "node-1", "$def",
                GUACD_A, "conn-1", null, null, "alice", "10.0.0.5",
                System.currentTimeMillis(), null));

        assertEquals(GUACD_A, store.lookupRoute("$def"));
        assertNull(store.lookupSeatToken("record-uuid-missing"));

    }

    /**
     * Verifies that unregistering a tunnel also removes the pointer from its
     * history record UUID to its seat token.
     *
     * @throws Exception
     *     If the tunnel cannot be registered or unregistered.
     */
    @Test
    public void unregisterRemovesTheRecordPointer() throws Exception {

        TunnelRegistration record = new TunnelRegistration("seat-3", "node-1", "$ghi",
                GUACD_A, "conn-1", null, null, "alice", "10.0.0.5",
                System.currentTimeMillis(), "record-uuid-3");

        store.registerTunnel(record);
        store.unregisterTunnel(record);

        assertNull(store.lookupSeatToken("record-uuid-3"));

    }

    /**
     * Verifies that a heartbeat for no tunnels does nothing and does not
     * fail, as happens on a replica with no active connections.
     *
     * @throws Exception
     *     If the heartbeat fails.
     */
    @Test
    public void heartbeatOfAnEmptyCollectionIsHarmless() throws Exception {
        store.heartbeat(Collections.<TunnelRegistration>emptyList());
        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
    }

}
