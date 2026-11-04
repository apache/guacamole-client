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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the cluster-wide authentication failure counters of
 * {@link RedisClusterStore}, which let every replica enforce one shared ban
 * per client address rather than each replica counting failures on its own.
 * Runs against a real Redis started by Testcontainers, and is skipped when
 * Docker is unavailable.
 */
public class AuthFailureTest {

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
     * The store under test, acting as replica "node-1".
     */
    private RedisClusterStore store;

    /**
     * Creates the store under test and clears the Redis keyspace, so that no
     * counter left by an earlier test is visible to this one.
     */
    @BeforeEach
    public void setUp() {
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "node-1");
        store.flushForTesting();
    }

    /**
     * Shuts down the store under test, releasing its Redis connections.
     */
    @AfterEach
    public void tearDown() {
        store.shutdown();
    }

    /**
     * Verifies that each recorded failure for an address increments its
     * counter, and that the new count is returned to the caller.
     *
     * @throws Exception
     *     If the store fails unexpectedly.
     */
    @Test
    public void failuresAccumulate() throws Exception {

        assertEquals(1, store.recordAuthenticationFailure("10.0.0.1", 300));
        assertEquals(2, store.recordAuthenticationFailure("10.0.0.1", 300));
        assertEquals(3, store.recordAuthenticationFailure("10.0.0.1", 300));

    }

    /**
     * Verifies that failures from one address do not count against another.
     *
     * @throws Exception
     *     If the store fails unexpectedly.
     */
    @Test
    public void addressesAreCountedIndependently() throws Exception {

        store.recordAuthenticationFailure("10.0.0.1", 300);
        store.recordAuthenticationFailure("10.0.0.1", 300);
        store.recordAuthenticationFailure("10.0.0.2", 300);

        assertEquals(2, store.getAuthenticationFailures("10.0.0.1"));
        assertEquals(1, store.getAuthenticationFailures("10.0.0.2"));

    }

    /**
     * Verifies that an address with no recorded failures reports zero rather
     * than -1, which the store reserves for "unknown" when Redis cannot be
     * reached.
     *
     * @throws Exception
     *     If the store fails unexpectedly.
     */
    @Test
    public void unknownAddressHasNoFailures() throws Exception {
        assertEquals(0, store.getAuthenticationFailures("10.0.0.99"));
    }

    /**
     * Verifies that failures recorded through one replica are counted by
     * another, so that a client cannot evade a ban by having its attempts
     * spread across replicas by the load balancer.
     *
     * @throws Exception
     *     If the store fails unexpectedly.
     */
    @Test
    public void countIsVisibleToAnotherReplica() throws Exception {

        // Two replicas share one count
        RedisClusterStore other = new RedisClusterStore(RedisTestSupport.redisUri(),
                STALE_WINDOW_MS, "node-2");

        try {
            store.recordAuthenticationFailure("10.0.0.5", 300);
            store.recordAuthenticationFailure("10.0.0.5", 300);
            assertEquals(3, other.recordAuthenticationFailure("10.0.0.5", 300));
        }

        finally {
            other.shutdown();
        }

    }

    /**
     * Verifies that a failure counter carries an expiry equal to the ban
     * duration, ages out once that duration passes, and that a failure after
     * expiry starts a fresh count.
     *
     * @throws Exception
     *     If the store fails unexpectedly, or the sleep is interrupted.
     */
    @Test
    public void theWindowIsRefreshedByEachFailure() throws Exception {

        // A one-second ban duration, re-armed by a second failure
        store.recordAuthenticationFailure("10.0.0.6", 1);
        long firstTtl = store.authFailureTtlForTesting("10.0.0.6");
        assertTrue(firstTtl > 0, "the counter must carry an expiry");

        Thread.sleep(1100);
        assertEquals(0, store.getAuthenticationFailures("10.0.0.6"),
                "the counter must age out once the ban duration passes");

        assertEquals(1, store.recordAuthenticationFailure("10.0.0.6", 1),
                "a failure after expiry starts a fresh count");

    }

}
