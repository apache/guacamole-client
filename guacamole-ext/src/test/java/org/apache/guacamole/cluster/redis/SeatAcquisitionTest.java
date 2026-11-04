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

/**
 * Tests atomic seat acquisition and release in {@link RedisClusterStore},
 * which enforces connection, per-user and balancing group concurrency
 * limits across every replica through a single Lua script. Runs against a
 * real Redis started by Testcontainers, and is skipped when Docker is
 * unavailable.
 */
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


    /**
     * The stale window given to the store, in milliseconds. Index members
     * whose score is older than this are treated as left behind by a dead
     * replica.
     */
    private static final long STALE_WINDOW_MS = 30000L;

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
     * The store under test, acting as replica "test-node".
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
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "test-node");
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
     * Builds a request for a single connection limited to the given number of
     * concurrent uses, with no per-user or group limits.
     *
     * @param uuid
     *     The seat token (tunnel UUID) requesting the seat.
     *
     * @param connectionId
     *     The identifier of the connection whose seat is requested.
     *
     * @param limit
     *     The maximum number of concurrent uses of the connection, where zero
     *     means unlimited.
     *
     * @return
     *     A seat request against the connection index alone, which reports
     *     {@link SeatResult#CONNECTION_LIMIT} when the limit is reached.
     */
    private SeatRequest connectionRequest(String uuid, String connectionId, int limit) {
        return new SeatRequest(uuid, Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex(connectionId), limit,
                        SeatResult.CONNECTION_LIMIT)));
    }

    /**
     * Verifies that seats are granted until the limit is reached and the
     * next distinct tunnel is refused with the connection limit result.
     *
     * @throws Exception
     *     If a seat cannot be acquired.
     */
    @Test
    public void grantsUpToTheLimitThenRefuses() throws Exception {

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 2)));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t2", "c", 2)));
        assertEquals(SeatResult.CONNECTION_LIMIT,
                store.acquireSeats(connectionRequest("t3", "c", 2)));

    }

    /**
     * Verifies that a limit of zero imposes no limit at all, matching the
     * meaning of zero in the standalone concurrency limits.
     *
     * @throws Exception
     *     If a seat cannot be acquired.
     */
    @Test
    public void treatsZeroLimitAsUnlimited() throws Exception {

        for (int i = 0; i < 50; i++)
            assertEquals(SeatResult.SUCCESS,
                    store.acquireSeats(connectionRequest("t" + i, "c", 0)));

    }

    /**
     * Verifies that, when a request carries several limits, the refusal
     * reports the result of the specific limit that was reached, so the user
     * is told why the connection was refused.
     *
     * @throws Exception
     *     If a seat cannot be acquired.
     */
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

    /**
     * Verifies that acquiring the same seats again for the same tunnel
     * succeeds without consuming a second seat, so that a retried
     * acquisition cannot lock its own tunnel out.
     *
     * @throws Exception
     *     If a seat cannot be acquired.
     */
    @Test
    public void isIdempotentForTheSameTunnel() throws Exception {

        // A retry after a failover must not count the member it already added
        SeatRequest request = connectionRequest("t1", "c", 1);
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));

        // ...and the seat must still be held exactly once
        assertEquals(1L, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

    /**
     * Verifies that a member whose score is outside the stale window neither
     * blocks a new acquisition nor survives it, since the script prunes such
     * members before checking the limit.
     *
     * @throws Exception
     *     If a seat cannot be acquired.
     */
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

    /**
     * Verifies that releasing a held seat immediately makes it available to
     * another tunnel.
     *
     * @throws Exception
     *     If a seat cannot be acquired or released.
     */
    @Test
    public void releaseFreesTheSeat() throws Exception {

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 1)));
        store.releaseSeats(connectionRequest("t1", "c", 1));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t2", "c", 1)));

    }

    /**
     * Verifies that releasing a seat which was never acquired, repeatedly,
     * neither fails nor leaves anything behind, since release may run more
     * than once during tunnel teardown.
     *
     * @throws Exception
     *     If a seat cannot be released.
     */
    @Test
    public void releaseIsSafeToRepeat() throws Exception {

        store.releaseSeats(connectionRequest("never-acquired", "c", 1));
        store.releaseSeats(connectionRequest("never-acquired", "c", 1));
        assertEquals(0L, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

    /**
     * Verifies that many threads racing to acquire the same connection's
     * seats are granted exactly the limit and no more, proving that the
     * check and the insert happen atomically inside the script.
     *
     * @throws Exception
     *     If waiting for the acquisition threads is interrupted.
     */
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
