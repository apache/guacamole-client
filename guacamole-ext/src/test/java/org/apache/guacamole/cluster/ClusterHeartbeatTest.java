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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ClusterHeartbeat, which periodically refreshes the cluster-
 * wide heartbeat of every tunnel owned by this replica. The scheduler runs for
 * real at a short interval against an in-memory store, and tests wait on
 * latches for observed heartbeats rather than sleeping for a fixed time.
 */
public class ClusterHeartbeatTest {

    /**
     * The guacd endpoint recorded in every tunnel registration created by these
     * tests.
     */
    private static final GuacdEndpoint ENDPOINT =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    /**
     * Cluster store which records every heartbeat it receives and counts down a
     * latch, so tests can wait for real scheduler activity rather than sleeping.
     */
    private static class RecordingStore extends NoOpClusterStore {

        /**
         * A copy of the registrations passed to each heartbeat, in the order
         * the heartbeats were received. Access is synchronized on this list, as
         * the heartbeat runs on the scheduler thread.
         */
        private final List<Collection<TunnelRegistration>> beats =
                new ArrayList<Collection<TunnelRegistration>>();

        /**
         * Latch counted down once per received heartbeat, released after the
         * expected number of heartbeats.
         */
        private final CountDownLatch latch;

        /**
         * Creates a new RecordingStore which releases its latch after the given
         * number of heartbeats.
         *
         * @param expectedBeats
         *     The number of heartbeats to wait for before await() returns.
         */
        RecordingStore(int expectedBeats) {
            this.latch = new CountDownLatch(expectedBeats);
        }

        @Override
        public void heartbeat(Collection<TunnelRegistration> registrations) {
            synchronized (beats) {
                beats.add(new ArrayList<TunnelRegistration>(registrations));
            }
            latch.countDown();
        }

        /**
         * Waits for the expected number of heartbeats, giving up after ten
         * seconds.
         *
         * @return
         *     True if the expected number of heartbeats were received, false if
         *     the wait timed out.
         *
         * @throws InterruptedException
         *     If the current thread is interrupted while waiting.
         */
        boolean await() throws InterruptedException {
            return latch.await(10, TimeUnit.SECONDS);
        }

        /**
         * Returns the registrations passed to the most recent heartbeat.
         *
         * @return
         *     A copy of the registrations passed to the most recent heartbeat.
         */
        Collection<TunnelRegistration> lastBeat() {
            synchronized (beats) {
                return beats.get(beats.size() - 1);
            }
        }

    }

    /**
     * Creates a registration for a tunnel owned by "node-1" on the test guacd
     * endpoint, using the given UUID to derive its connection ID and recording
     * name.
     *
     * @param uuid
     *     The UUID of the tunnel to register.
     *
     * @return
     *     A new registration for the tunnel having the given UUID.
     */
    private TunnelRegistration registration(String uuid) {
        return new TunnelRegistration(uuid, "node-1", "$" + uuid, ENDPOINT,
                "conn-1", null, null, "alice", null, System.currentTimeMillis(),
                "record-" + uuid);
    }

    /**
     * Verifies that a tunnel added before the scheduler starts is included in
     * the heartbeats sent to the store.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void heartbeatsRegisteredTunnels() throws Exception {

        RecordingStore store = new RecordingStore(2);
        ClusterHeartbeat heartbeat = new ClusterHeartbeat(store, 50L);

        heartbeat.add(registration("t1"));
        heartbeat.start();

        try {
            assertTrue(store.await(), "heartbeat did not fire");
            assertEquals(1, store.lastBeat().size());
        }
        finally {
            heartbeat.shutdown();
        }

    }

    /**
     * Verifies that a tunnel removed before the scheduler starts is not
     * included in any heartbeat, so it will age out of the cluster indexes.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void removedTunnelsAreNoLongerHeartbeated() throws Exception {

        RecordingStore store = new RecordingStore(2);
        ClusterHeartbeat heartbeat = new ClusterHeartbeat(store, 50L);

        heartbeat.add(registration("t1"));
        heartbeat.remove("t1");
        heartbeat.start();

        try {
            assertTrue(store.await(), "heartbeat did not fire");
            assertEquals(0, store.lastBeat().size());
        }
        finally {
            heartbeat.shutdown();
        }

    }

    /**
     * Verifies that the scheduler keeps attempting heartbeats after the store
     * throws. A periodic task that propagates an exception is silently
     * cancelled, which would let every tunnel on this replica age out while it
     * is still in use.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void storeFailureDoesNotStopTheScheduler() throws Exception {

        final CountDownLatch attempts = new CountDownLatch(3);

        ClusterStore failing = new NoOpClusterStore() {

            @Override
            public void heartbeat(Collection<TunnelRegistration> registrations)
                    throws GuacamoleException {
                attempts.countDown();
                throw new GuacamoleException("Redis is down.");
            }

        };

        ClusterHeartbeat heartbeat = new ClusterHeartbeat(failing, 50L);
        heartbeat.add(registration("t1"));
        heartbeat.start();

        try {
            assertTrue(attempts.await(10, TimeUnit.SECONDS),
                    "scheduler stopped after a store failure");
        }
        finally {
            heartbeat.shutdown();
        }

    }

}
