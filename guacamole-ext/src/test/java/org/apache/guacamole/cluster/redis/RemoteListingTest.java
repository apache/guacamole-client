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
import java.util.Collection;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the cluster-wide tunnel listing of {@link RedisClusterStore}, which
 * backs the administrative active-connection view. Runs against a real Redis
 * started by Testcontainers, and is skipped when Docker is unavailable.
 */
public class RemoteListingTest {

    /**
     * The stale window given to the store, in milliseconds. Index members
     * whose score is older than this are treated as left behind by a dead
     * replica.
     */
    private static final long STALE_WINDOW_MS = 30000L;

    /**
     * The guacd instance every registered tunnel in these tests is routed to.
     */
    private static final GuacdEndpoint GUACD_A =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    /**
     * Skips this class rather than failing it when no Docker daemon is
     * reachable. The image build runs the full test suite inside a container
     * that has no daemon of its own.
     */
    @BeforeAll
    public static void requireDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Redis-backed tests.");
    }

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
     * Returns a registration for a tunnel owned by replica "node-2" on
     * connection "conn-1", so that every listed entry is visibly one this
     * replica did not create itself.
     *
     * @param seat
     *     The seat token (tunnel UUID) of the tunnel.
     *
     * @param user
     *     The username of the user owning the tunnel.
     *
     * @param recordUuid
     *     The UUID of the connection history record for the tunnel.
     *
     * @return
     *     A new registration for the described tunnel.
     */
    private TunnelRegistration registration(String seat, String user, String recordUuid) {
        return new TunnelRegistration(seat, "node-2", "$" + seat, GUACD_A,
                "conn-1", null, null, user, "10.0.0.5",
                System.currentTimeMillis(), recordUuid);
    }

    /**
     * Verifies that every registered, unexpired tunnel is listed, regardless
     * of which user owns it.
     *
     * @throws Exception
     *     If the tunnels cannot be registered.
     */
    @Test
    public void listsEveryLiveTunnel() throws Exception {

        store.registerTunnel(registration("seat-1", "alice", "rec-1"));
        store.registerTunnel(registration("seat-2", "bob", "rec-2"));

        Collection<TunnelRegistration> listed = store.listTunnels();
        assertEquals(2, listed.size());

    }

    /**
     * Verifies that a listed tunnel is rebuilt from Redis with every field
     * the administrative view displays or acts upon, including the owning
     * replica and the history record UUID used to address a kill.
     *
     * @throws Exception
     *     If the tunnel cannot be registered.
     */
    @Test
    public void listedEntriesCarryTheFieldsTheAdminViewNeeds() throws Exception {

        store.registerTunnel(registration("seat-1", "alice", "rec-1"));

        TunnelRegistration listed = store.listTunnels().iterator().next();
        assertEquals("seat-1", listed.getTunnelUuid());
        assertEquals("rec-1", listed.getRecordUuid());
        assertEquals("alice", listed.getUsername());
        assertEquals("conn-1", listed.getConnectionIdentifier());
        assertEquals("node-2", listed.getNodeId());
        assertEquals("10.0.0.5", listed.getRemoteHost());

    }

    /**
     * Verifies that an index member whose score is outside the stale window,
     * and whose tunnel hash no longer exists, is not listed. Such members are
     * left behind when a replica dies without unregistering its tunnels.
     *
     * @throws Exception
     *     If the tunnel cannot be registered.
     */
    @Test
    public void omitsStaleTunnelsLeftByDeadReplicas() throws Exception {

        store.registerTunnel(registration("seat-1", "alice", "rec-1"));

        // A tunnel whose replica died: still a member of the index, but its
        // hash has expired and its score is far outside the window
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(ClusterKeys.ALL_INDEX, (double) ancient, "seat-dead");

        Collection<TunnelRegistration> listed = store.listTunnels();
        assertEquals(1, listed.size());
        assertEquals("seat-1", listed.iterator().next().getTunnelUuid());

    }

    /**
     * Verifies that listing an empty cluster returns an empty collection
     * rather than null or an error.
     *
     * @throws Exception
     *     If the listing fails.
     */
    @Test
    public void returnsEmptyRatherThanFailingWhenThereIsNothing() throws Exception {
        assertTrue(store.listTunnels().isEmpty());
    }

}
