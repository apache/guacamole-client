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
import org.apache.guacamole.cluster.ClusterKeys;
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


    private static final long STALE_WINDOW_MS = 30000L;

    private static final GuacdEndpoint GUACD_A =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    private static final GuacdEndpoint GUACD_B =
            new GuacdEndpoint("guacd-b", 4822, EncryptionMethod.NONE);

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisClusterStore store;

    @BeforeEach
    public void setUp() {
        client = RedisClient.create(RedisTestSupport.redisUri());
        connection = client.connect();
        connection.sync().flushall();
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "node-1");
    }

    @AfterEach
    public void tearDown() {
        store.shutdown();
        connection.close();
        client.shutdown();
    }

    private TunnelRegistration registration(String uuid, GuacdEndpoint endpoint,
            String guacdConnectionId) {
        return new TunnelRegistration(uuid, "node-1", guacdConnectionId, endpoint,
                "conn-1", "group-1", null, "alice", "10.0.0.5",
                System.currentTimeMillis());
    }

    @Test
    public void registeredTunnelIsRoutableByGuacdConnectionId() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));
        assertEquals(GUACD_A, store.lookupRoute("$abc"));

    }

    @Test
    public void unknownRouteReturnsNull() throws Exception {
        assertNull(store.lookupRoute("$nonexistent"));
    }

    @Test
    public void registeredTunnelAppearsInEveryIndex() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));

        assertEquals(1L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
        assertEquals(1L, connection.sync().zcard(ClusterKeys.userIndex("alice")));
        assertEquals(1L, connection.sync().zcard(ClusterKeys.guacdIndex(GUACD_A.toKey())));

    }

    @Test
    public void storesTheFullTunnelRecord() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));

        assertEquals("node-1", connection.sync().hget(ClusterKeys.tunnel("t1"), "nodeId"));
        assertEquals("conn-1",
                connection.sync().hget(ClusterKeys.tunnel("t1"), "connIdentifier"));
        assertEquals(GUACD_A.toKey(),
                connection.sync().hget(ClusterKeys.tunnel("t1"), "guacdEndpoint"));

    }

    @Test
    public void countsLiveTunnelsPerGuacdInstance() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$a1"));
        store.registerTunnel(registration("t2", GUACD_A, "$a2"));
        store.registerTunnel(registration("t3", GUACD_B, "$b1"));

        assertEquals(2L, store.countTunnels(GUACD_A));
        assertEquals(1L, store.countTunnels(GUACD_B));

    }

    @Test
    public void ignoresStaleTunnelsWhenCounting() throws Exception {

        String key = ClusterKeys.guacdIndex(GUACD_A.toKey());
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(key, (double) ancient, "dead-tunnel");

        store.registerTunnel(registration("t1", GUACD_A, "$a1"));

        assertEquals(1L, store.countTunnels(GUACD_A));

    }

    @Test
    public void unregisterRemovesTunnelAndRoute() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);
        store.unregisterTunnel(record);

        assertNull(store.lookupRoute("$abc"));
        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
        assertEquals(0L, store.countTunnels(GUACD_A));

    }

    @Test
    public void unregisterIsSafeToRepeat() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);
        store.unregisterTunnel(record);
        store.unregisterTunnel(record);

        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));

    }

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

    @Test
    public void heartbeatOfAnEmptyCollectionIsHarmless() throws Exception {
        store.heartbeat(Collections.<TunnelRegistration>emptyList());
        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
    }

}
