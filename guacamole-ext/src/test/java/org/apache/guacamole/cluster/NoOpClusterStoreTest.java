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

package org.apache.guacamole.cluster;

import java.util.Arrays;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for NoOpClusterStore, the store bound when clustering is disabled
 * or Redis cannot be used. These tests verify that it never enforces limits
 * itself, reports itself as unavailable and unclustered so callers fall back to
 * the standard in-memory behaviour, and holds no routes or load.
 */
public class NoOpClusterStoreTest {

    /**
     * An arbitrary guacd endpoint used to query tunnel counts from the store.
     */
    private static final GuacdEndpoint ENDPOINT =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    /**
     * Verifies that acquiring seats always succeeds, even when the same request
     * exceeding a limit of one is made twice.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void alwaysGrantsSeats() throws Exception {

        NoOpClusterStore store = new NoOpClusterStore();

        SeatRequest request = new SeatRequest("t1", Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex("c"), 1,
                        SeatResult.CONNECTION_LIMIT)));

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));

    }

    /**
     * Verifies that the store reports itself as unavailable, so that the tunnel
     * service enforces limits with its in-memory counters instead.
     */
    @Test
    public void reportsUnavailableSoTheServiceFallsBackToLocalCounters() {

        // RestrictedGuacamoleTunnelService checks isAvailable() before every
        // acquire. False here is what routes it to the in-memory path, which
        // is the documented behavior both when clustering is disabled and when
        // Redis is unreachable.
        assertFalse(new NoOpClusterStore().isAvailable());

    }

    /**
     * Verifies that the store reports itself as not clustered, so that share
     * key joins take the standard, non-clustered path.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void disabledClusteringIsNotClustered() throws Exception {

        // selectGuacdEndpoint consults this before routing a join through the
        // cluster route table. False sends it down the upstream path instead;
        // a true here would fail every share-key join closed, because a no-op
        // store has no routes to find.
        assertFalse(new NoOpClusterStore().isClustered());

    }

    /**
     * Verifies that a connection limit of one is not enforced by the store when
     * clustering is disabled, leaving enforcement to the in-memory counters.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void disabledClusteringGrantsEverySeatRegardlessOfLimit() throws Exception {

        // With cluster-enabled=false the bound store is NoOpClusterStore. A
        // max-connections of 1 must not be enforced here at all -- enforcement
        // belongs to the in-memory counters, exactly as upstream.
        NoOpClusterStore store = new NoOpClusterStore();

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(
                SeatRequestBuilder.forConnection("t1", "alice", "conn-1", 1, 1)));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(
                SeatRequestBuilder.forConnection("t2", "alice", "conn-1", 1, 1)));

    }

    /**
     * Verifies that the store finds no route for any tunnel and reports zero
     * tunnels on any guacd endpoint.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void hasNoRoutesAndNoLoad() throws Exception {
        NoOpClusterStore store = new NoOpClusterStore();
        assertNull(store.lookupRoute("$abc"));
        assertEquals(0L, store.countTunnels(ENDPOINT));
    }

}
