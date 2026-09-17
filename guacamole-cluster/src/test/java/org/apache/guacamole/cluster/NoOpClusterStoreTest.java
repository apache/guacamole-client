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

public class NoOpClusterStoreTest {

    private static final GuacdEndpoint ENDPOINT =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    @Test
    public void alwaysGrantsSeats() throws Exception {

        NoOpClusterStore store = new NoOpClusterStore();

        SeatRequest request = new SeatRequest("t1", Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex("c"), 1,
                        SeatResult.CONNECTION_LIMIT)));

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));

    }

    @Test
    public void reportsUnavailableSoTheServiceFallsBackToLocalCounters() {

        // RestrictedGuacamoleTunnelService checks isAvailable() before every
        // acquire. False here is what routes it to the in-memory path, which
        // is the documented behavior both when clustering is disabled and when
        // Redis is unreachable.
        assertFalse(new NoOpClusterStore().isAvailable());

    }

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

    @Test
    public void hasNoRoutesAndNoLoad() throws Exception {
        NoOpClusterStore store = new NoOpClusterStore();
        assertNull(store.lookupRoute("$abc"));
        assertEquals(0L, store.countTunnels(ENDPOINT));
    }

}
