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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.SeatKey;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.DockerClientFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeatAcquisitionTest {

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

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisClusterStore store;

    @BeforeEach
    public void setUp() {
        client = RedisClient.create(RedisTestSupport.redisUri());
        connection = client.connect();
        connection.sync().flushall();
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "test-node");
    }

    @AfterEach
    public void tearDown() {
        store.shutdown();
        connection.close();
        client.shutdown();
    }

    /**
     * Builds a request for a single connection limited to the given number of
     * concurrent uses, with no per-user or group limits.
     */
    private SeatRequest connectionRequest(String uuid, String connectionId, int limit) {
        return new SeatRequest(uuid, Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex(connectionId), limit,
                        SeatResult.CONNECTION_LIMIT)));
    }

    @Test
    public void grantsUpToTheLimitThenRefuses() throws Exception {

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 2)));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t2", "c", 2)));
        assertEquals(SeatResult.CONNECTION_LIMIT,
                store.acquireSeats(connectionRequest("t3", "c", 2)));

    }

    @Test
    public void treatsZeroLimitAsUnlimited() throws Exception {

        for (int i = 0; i < 50; i++)
            assertEquals(SeatResult.SUCCESS,
                    store.acquireSeats(connectionRequest("t" + i, "c", 0)));

    }

    @Test
    public void reportsWhichLimitFailed() throws Exception {

        // Connection allows 5, but this user allows only 1
        List<SeatKey> keys = Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex("c"), 5, SeatResult.CONNECTION_LIMIT),
                new SeatKey(ClusterKeys.userConnectionSeat("alice", "c"), 1,
                        SeatResult.USER_CONNECTION_LIMIT));

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(new SeatRequest("t1", keys)));
        assertEquals(SeatResult.USER_CONNECTION_LIMIT,
                store.acquireSeats(new SeatRequest("t2", keys)));

    }

    @Test
    public void isIdempotentForTheSameTunnel() throws Exception {

        // A retry after a failover must not count the member it already added
        SeatRequest request = connectionRequest("t1", "c", 1);
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));

        // ...and the seat must still be held exactly once
        assertEquals(1L, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

    @Test
    public void ignoresStaleMembers() throws Exception {

        String key = ClusterKeys.connectionIndex("c");

        // Simulate a tunnel from a dead replica, last seen well outside the window
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(key, (double) ancient, "dead-tunnel");

        // The stale member must neither block nor survive
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 1)));
        assertEquals(1L, connection.sync().zcard(key));

    }

    @Test
    public void releaseFreesTheSeat() throws Exception {

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 1)));
        store.releaseSeats(connectionRequest("t1", "c", 1));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t2", "c", 1)));

    }

    @Test
    public void releaseIsSafeToRepeat() throws Exception {

        store.releaseSeats(connectionRequest("never-acquired", "c", 1));
        store.releaseSeats(connectionRequest("never-acquired", "c", 1));
        assertEquals(0L, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

    @Test
    public void neverExceedsTheLimitUnderConcurrency() throws Exception {

        final int limit = 5;
        final int threads = 200;

        ExecutorService executor = Executors.newFixedThreadPool(32);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final AtomicInteger granted = new AtomicInteger();
        final List<Throwable> errors = new ArrayList<Throwable>();

        for (int i = 0; i < threads; i++) {
            final String uuid = UUID.randomUUID().toString();
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        start.await();
                        if (store.acquireSeats(connectionRequest(uuid, "c", limit))
                                == SeatResult.SUCCESS)
                            granted.incrementAndGet();
                    }
                    catch (Throwable e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    }
                    finally {
                        done.countDown();
                    }
                }

            });
        }

        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "acquisition threads did not finish");
        executor.shutdownNow();

        synchronized (errors) {
            assertTrue(errors.isEmpty(), "errors during acquisition: " + errors);
        }

        assertEquals(limit, granted.get());
        assertEquals((long) limit, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

}
