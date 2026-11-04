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
import org.apache.guacamole.cluster.ClusterLogoutHandler;
import org.apache.guacamole.cluster.TokenIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the cluster-wide session token storage of {@link RedisClusterStore},
 * which lets an authenticated session be rehydrated on any replica, together
 * with idle expiry and the propagation of logouts between replicas. Runs
 * against a real Redis started by Testcontainers, and is skipped when Docker
 * is unavailable.
 */
public class TokenStoreTest {

    /**
     * The stale window given to the store, in milliseconds. Index members
     * whose score is older than this are treated as left behind by a dead
     * replica.
     */
    private static final long STALE_WINDOW_MS = 30000L;

    /**
     * Skips this class rather than failing it when no Docker daemon is
     * reachable.
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
     * Verifies that every field of a stored token identity is read back
     * unchanged, as rehydrating the session on another replica needs all of
     * them.
     *
     * @throws Exception
     *     If the token cannot be stored or read.
     */
    @Test
    public void identityRoundTrips() throws Exception {

        store.putToken("hash-1", new TokenIdentity("alice", "postgresql",
                "10.0.0.1", "client.example.com", 1700000000000L), 3600);

        TokenIdentity identity = store.getToken("hash-1");
        assertEquals("alice", identity.getUsername());
        assertEquals("postgresql", identity.getAuthProviderIdentifier());
        assertEquals("10.0.0.1", identity.getRemoteAddress());
        assertEquals("client.example.com", identity.getRemoteHostname());
        assertEquals(1700000000000L, identity.getAuthenticatedTime());

    }

    /**
     * Verifies that a token which was never issued is reported as absent
     * (null), the same as an expired token.
     *
     * @throws Exception
     *     If the token cannot be read.
     */
    @Test
    public void unknownTokenIsNull() throws Exception {
        assertNull(store.getToken("never-issued"));
    }

    /**
     * Verifies that an identity stored without a remote hostname reads back
     * with a null hostname rather than failing.
     *
     * @throws Exception
     *     If the token cannot be stored or read.
     */
    @Test
    public void identityWithoutAHostnameRoundTrips() throws Exception {

        // The remote hostname is frequently unavailable
        store.putToken("hash-2", new TokenIdentity("bob", "postgresql",
                "10.0.0.2", null, 1700000000000L), 3600);

        assertNull(store.getToken("hash-2").getRemoteHostname());

    }

    /**
     * Verifies that a removed token can no longer be read, so that a logged
     * out session cannot be rehydrated elsewhere.
     *
     * @throws Exception
     *     If the token cannot be stored, removed or read.
     */
    @Test
    public void removedTokenIsGone() throws Exception {

        store.putToken("hash-3", new TokenIdentity("carol", "postgresql",
                "10.0.0.3", null, 1700000000000L), 3600);
        store.removeToken("hash-3");

        assertNull(store.getToken("hash-3"));

    }

    /**
     * Verifies that a token stored by one replica can be read by another,
     * which is what allows a session to survive the loss of the replica that
     * authenticated it.
     *
     * @throws Exception
     *     If the token cannot be stored or read.
     */
    @Test
    public void tokenIsVisibleToAnotherReplica() throws Exception {

        RedisClusterStore other = new RedisClusterStore(RedisTestSupport.redisUri(),
                STALE_WINDOW_MS, "node-2");

        try {
            store.putToken("hash-4", new TokenIdentity("dave", "postgresql",
                    "10.0.0.4", null, 1700000000000L), 3600);
            assertEquals("dave", other.getToken("hash-4").getUsername());
        }

        finally {
            other.shutdown();
        }

    }

    /**
     * Verifies that touching a token replaces its remaining lifetime with
     * the new idle timeout, so that an active session does not expire after
     * its original timeout.
     *
     * @throws Exception
     *     If the token cannot be stored or touched, or the wait is
     *     interrupted.
     */
    @Test
    public void idleExpiryIsRefreshedByTouch() throws Exception {

        store.putToken("hash-5", new TokenIdentity("erin", "postgresql",
                "10.0.0.5", null, 1700000000000L), 2);

        Thread.sleep(1100);
        store.touchToken("hash-5", 60);
        assertTrue(store.tokenTtlForTesting("hash-5") > 2,
                "touch must extend the idle window");

    }

    /**
     * Verifies that a logout published by one replica is delivered over
     * Redis pub/sub to the logout handler registered on another, so that the
     * session is invalidated on every replica holding it.
     *
     * @throws Exception
     *     If waiting for the logout is interrupted.
     */
    @Test
    public void logoutReachesAnotherReplica() throws Exception {

        RedisClusterStore other = new RedisClusterStore(RedisTestSupport.redisUri(),
                STALE_WINDOW_MS, "node-2");

        try {

            final CountDownLatch seen = new CountDownLatch(1);
            final AtomicReference<String> hash = new AtomicReference<String>();

            other.onLogout(new ClusterLogoutHandler() {

                @Override
                public void loggedOut(String tokenHash) {
                    hash.set(tokenHash);
                    seen.countDown();
                }

            });

            store.publishLogout("hash-logged-out");

            assertTrue(seen.await(10, TimeUnit.SECONDS), "logout never arrived");
            assertEquals("hash-logged-out", hash.get());

        }

        finally {
            other.shutdown();
        }

    }

    /**
     * Verifies that a token is no longer readable once its idle timeout has
     * elapsed without a touch.
     *
     * @throws Exception
     *     If the token cannot be stored or read, or the wait is interrupted.
     */
    @Test
    public void anExpiredTokenIsGone() throws Exception {

        store.putToken("hash-6", new TokenIdentity("frank", "postgresql",
                "10.0.0.6", null, 1700000000000L), 1);

        Thread.sleep(1100);
        assertNull(store.getToken("hash-6"));

    }

}
