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
import org.apache.guacamole.cluster.ClusterShareRevocationHandler;
import org.apache.guacamole.cluster.SharedConnectionEntry;
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
 * Tests the cluster-wide share key storage of {@link RedisClusterStore},
 * which lets a share link issued on one replica be redeemed on any other,
 * together with the propagation of share revocations between replicas. Runs
 * against a real Redis started by Testcontainers, and is skipped when Docker
 * is unavailable.
 */
public class ShareKeyTest {

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
     * Verifies that every field of a stored share key is read back
     * unchanged, as another replica needs all of them to join the shared
     * connection.
     *
     * @throws Exception
     *     If the share key cannot be stored or read.
     */
    @Test
    public void storedKeyRoundTrips() throws Exception {

        store.putShareKey("key-1", new SharedConnectionEntry("seat-1", "$abc",
                "conn-1", "profile-1", "alice"));

        SharedConnectionEntry entry = store.getShareKey("key-1");
        assertEquals("seat-1", entry.getSeatToken());
        assertEquals("$abc", entry.getGuacdConnectionId());
        assertEquals("conn-1", entry.getConnectionIdentifier());
        assertEquals("profile-1", entry.getSharingProfileIdentifier());
        assertEquals("alice", entry.getSharedBy());

    }

    /**
     * Verifies that a share key which was never issued is reported as
     * absent (null), the same as an expired key.
     *
     * @throws Exception
     *     If the share key cannot be read.
     */
    @Test
    public void unknownKeyIsNull() throws Exception {
        assertNull(store.getShareKey("never-issued"));
    }

    /**
     * Verifies that a share key stored without a sharing profile reads back
     * with a null profile rather than failing or inventing one.
     *
     * @throws Exception
     *     If the share key cannot be stored or read.
     */
    @Test
    public void keyWithoutASharingProfileRoundTrips() throws Exception {

        // Sharing without a profile is legal; the field is simply absent
        store.putShareKey("key-2", new SharedConnectionEntry("seat-2", "$def",
                "conn-1", null, "alice"));

        assertNull(store.getShareKey("key-2").getSharingProfileIdentifier());

    }

    /**
     * Verifies that a removed share key can no longer be read, so that a
     * revoked link stops working on every replica.
     *
     * @throws Exception
     *     If the share key cannot be stored, removed or read.
     */
    @Test
    public void removedKeyIsGone() throws Exception {

        store.putShareKey("key-3", new SharedConnectionEntry("seat-3", "$ghi",
                "conn-1", null, "alice"));
        store.removeShareKey("key-3");

        assertNull(store.getShareKey("key-3"));

    }

    /**
     * Verifies that a share revocation published by one replica is delivered
     * over Redis pub/sub to the revocation handler registered on another,
     * so that users joined through a revoked key can be disconnected
     * wherever they are.
     *
     * @throws Exception
     *     If waiting for the revocation is interrupted.
     */
    @Test
    public void revocationReachesAnotherReplica() throws Exception {

        RedisClusterStore other = new RedisClusterStore(RedisTestSupport.redisUri(),
                STALE_WINDOW_MS, "node-2");

        try {

            final CountDownLatch revoked = new CountDownLatch(1);
            final AtomicReference<String> seen = new AtomicReference<String>();

            other.onShareRevoked(new ClusterShareRevocationHandler() {

                @Override
                public void shareRevoked(String shareKey) {
                    seen.set(shareKey);
                    revoked.countDown();
                }

            });

            store.publishShareRevocation("key-revoked");

            assertTrue(revoked.await(10, TimeUnit.SECONDS),
                    "revocation never arrived");
            assertEquals("key-revoked", seen.get());

        }

        finally {
            other.shutdown();
        }

    }

}
