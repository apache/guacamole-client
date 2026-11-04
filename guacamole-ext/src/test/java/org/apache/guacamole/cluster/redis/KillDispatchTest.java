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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.guacamole.cluster.ClusterKillHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests delivery of cluster-wide kill requests, which
 * {@link RedisClusterStore} broadcasts over Redis pub/sub so that a tunnel can
 * be terminated from any replica regardless of which one holds it. Two stores
 * act as two replicas sharing a real Redis started by Testcontainers; skipped
 * when Docker is unavailable.
 */
public class KillDispatchTest {

    /**
     * The number of milliseconds after which an unrefreshed cluster entry is
     * considered dead. Its value is irrelevant to these tests; it only needs
     * to be long enough that nothing expires while a test runs.
     */
    private static final long STALE_WINDOW_MS = 30000L;

    /**
     * Skips this class rather than failing it when no Docker daemon is
     * reachable, since every test here needs the shared Redis container
     * provided by {@link RedisTestSupport}.
     */
    @BeforeAll
    public static void requireDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Redis-backed tests.");
    }

    /**
     * The replica ("node-1") which issues kill requests.
     */
    private RedisClusterStore publisher;

    /**
     * A second replica ("node-2") which listens for kill requests.
     */
    private RedisClusterStore subscriber;

    /**
     * Creates the two replicas, both connected to the shared Redis container.
     */
    @BeforeEach
    public void setUp() {
        publisher = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "node-1");
        subscriber = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "node-2");
    }

    /**
     * Shuts down both replicas, closing their pub/sub subscriptions.
     */
    @AfterEach
    public void tearDown() {
        publisher.shutdown();
        subscriber.shutdown();
    }

    /**
     * Verifies that a kill requested on one replica is delivered, with the
     * same connection record UUID, to the kill handler registered on another.
     *
     * @throws Exception
     *     If the request cannot be published, or waiting is interrupted.
     */
    @Test
    public void killRequestReachesAnotherReplica() throws Exception {

        final CountDownLatch received = new CountDownLatch(1);
        final AtomicReference<String> seen = new AtomicReference<String>();

        subscriber.onKillRequest(new ClusterKillHandler() {

            @Override
            public void killLocalTunnel(String recordUuid) {
                seen.set(recordUuid);
                received.countDown();
            }

        });

        publisher.requestKill("record-uuid-1");

        assertTrue(received.await(10, TimeUnit.SECONDS), "kill request never arrived");
        assertEquals("record-uuid-1", seen.get());

    }

    /**
     * Verifies that a kill request is also delivered to the replica which
     * published it, since that replica may itself own the tunnel.
     *
     * @throws Exception
     *     If the request cannot be published, or waiting is interrupted.
     */
    @Test
    public void everyReplicaIsNotifiedIncludingThePublisher() throws Exception {

        // The owner may be the replica that issued the kill. Delivery is
        // broadcast; deciding who owns the tunnel is the handler's job.
        final CountDownLatch received = new CountDownLatch(1);

        publisher.onKillRequest(new ClusterKillHandler() {

            @Override
            public void killLocalTunnel(String recordUuid) {
                received.countDown();
            }

        });

        publisher.requestKill("record-uuid-2");
        assertTrue(received.await(10, TimeUnit.SECONDS),
                "publisher did not receive its own request");

    }

}
