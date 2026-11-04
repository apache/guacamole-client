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

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the startup self-check of the Redis permission surface performed by
 * {@link RedisClusterStore#selfCheck()}, which probes every class of Redis
 * command the store depends on and reports each one that fails. Runs against
 * a real Redis started by Testcontainers, and is skipped when Docker is
 * unavailable.
 */
public class SelfCheckTest {

    /**
     * The stale window given to the store, in milliseconds. Index members
     * whose score is older than this are treated as left behind by a dead
     * replica.
     */
    private static final long STALE_WINDOW_MS = 30000L;

    /**
     * Every probe the self-check runs. A Redis that cannot be reached at all
     * must report all of them rather than throwing.
     */
    private static final int PROBE_COUNT = 8;

    /**
     * Skips this class rather than failing it when no Docker daemon is
     * reachable, since a Redis-backed test cannot be meaningfully run
     * without one.
     */
    @BeforeAll
    public static void requireDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Redis-backed tests.");
    }

    /**
     * The store under test, connected to an unrestricted Redis.
     */
    private RedisClusterStore store;

    /**
     * Creates a fresh store under test and flushes the shared Redis so that
     * no state leaks between tests.
     */
    @BeforeEach
    public void setUp() {
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "node-1");
        store.flushForTesting();
    }

    /**
     * Shuts down the store under test.
     */
    @AfterEach
    public void tearDown() {
        store.shutdown();
    }

    /**
     * Verifies that a Redis with no ACL restrictions reports no failures,
     * so that a correctly provisioned deployment starts without warnings.
     */
    @Test
    public void anUnrestrictedRedisPassesEveryCheck() {
        List<String> failures = store.selfCheck();
        assertTrue(failures.isEmpty(), "unexpected failures: " + failures);
    }

    /**
     * Verifies that the self-check deletes every probe key it created.
     */
    @Test
    public void theCheckLeavesNothingBehind() {

        store.selfCheck();

        // A self-check runs on every replica start. If it left its probe key
        // behind, every restart would add one more key nothing owns.
        assertEquals(0L, store.probeKeyCountForTesting());

    }

    /**
     * Verifies that a Redis which cannot be reached at all causes every
     * probe to be reported as a failure, rather than the self-check throwing
     * and aborting startup.
     */
    @Test
    public void anUnreachableRedisReportsEveryProbeRatherThanThrowing() {

        RedisClusterStore unreachable = new RedisClusterStore(
                "redis://127.0.0.1:1", STALE_WINDOW_MS, "node-1");

        try {

            List<String> failures = unreachable.selfCheck();

            // Startup must survive a Redis that is simply down: the check
            // reports, it does not throw
            assertEquals(PROBE_COUNT, failures.size(),
                    "every probe should be reported: " + failures);

        }

        finally {
            unreachable.shutdown();
        }

    }

}
