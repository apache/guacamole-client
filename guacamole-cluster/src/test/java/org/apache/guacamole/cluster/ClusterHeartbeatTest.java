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

public class ClusterHeartbeatTest {

    private static final GuacdEndpoint ENDPOINT =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    /**
     * Cluster store which records every heartbeat it receives and counts down a
     * latch, so tests can wait for real scheduler activity rather than sleeping.
     */
    private static class RecordingStore extends NoOpClusterStore {

        private final List<Collection<TunnelRegistration>> beats =
                new ArrayList<Collection<TunnelRegistration>>();

        private final CountDownLatch latch;

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

        boolean await() throws InterruptedException {
            return latch.await(10, TimeUnit.SECONDS);
        }

        Collection<TunnelRegistration> lastBeat() {
            synchronized (beats) {
                return beats.get(beats.size() - 1);
            }
        }

    }

    private TunnelRegistration registration(String uuid) {
        return new TunnelRegistration(uuid, "node-1", "$" + uuid, ENDPOINT,
                "conn-1", null, null, "alice", null, System.currentTimeMillis());
    }

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
