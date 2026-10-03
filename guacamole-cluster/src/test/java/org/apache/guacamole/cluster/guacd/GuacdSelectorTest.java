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

package org.apache.guacamole.cluster.guacd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.NoOpClusterStore;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuacdSelectorTest {

    private static final GuacdEndpoint A =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    private static final GuacdEndpoint B =
            new GuacdEndpoint("guacd-b", 4822, EncryptionMethod.NONE);

    private static final GuacdEndpoint C =
            new GuacdEndpoint("guacd-c", 4822, EncryptionMethod.NONE);

    /**
     * Cluster store with fixed per-endpoint load and a fixed route table.
     */
    private static class FakeStore extends NoOpClusterStore {

        private final Map<String, Long> load = new HashMap<String, Long>();
        private final Map<String, GuacdEndpoint> routes = new HashMap<String, GuacdEndpoint>();

        void setLoad(GuacdEndpoint endpoint, long count) {
            load.put(endpoint.toKey(), count);
        }

        void setRoute(String connectionId, GuacdEndpoint endpoint) {
            routes.put(connectionId, endpoint);
        }

        @Override
        public long countTunnels(GuacdEndpoint endpoint) {
            Long count = load.get(endpoint.toKey());
            return count == null ? 0L : count;
        }

        @Override
        public GuacdEndpoint lookupRoute(String guacdConnectionId) {
            return routes.get(guacdConnectionId);
        }

    }

    private GuacdPool poolOf(final List<GuacdEndpoint> endpoints) {
        return new GuacdPool() {

            @Override
            public List<GuacdEndpoint> getCandidates() {
                return endpoints;
            }

        };
    }

    @Test
    public void selectsTheLeastLoadedEndpoint() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 10L);
        store.setLoad(B, 2L);
        store.setLoad(C, 7L);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B, C)), 15000L);

        assertEquals(B, selector.selectForNew());

    }

    @Test
    public void routesAJoinToTheOwningEndpoint() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 100L);
        store.setRoute("$abc", A);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        // Load is irrelevant: the join must go where the connection lives
        assertEquals(A, selector.selectForJoin("$abc"));

    }

    @Test
    public void failsClosedWhenAJoinTargetIsGone() {

        FakeStore store = new FakeStore();

        final GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        assertThrows(GuacamoleException.class, new org.junit.jupiter.api.function.Executable() {

            @Override
            public void execute() throws Throwable {
                selector.selectForJoin("$vanished");
            }

        });

    }

    @Test
    public void skipsCircuitBrokenEndpoints() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 0L);
        store.setLoad(B, 5L);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        // A is least loaded, but has just failed
        selector.markFailed(A);

        assertEquals(B, selector.selectForNew());

    }

    @Test
    public void recoversAfterTheCircuitBreakExpires() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 0L);
        store.setLoad(B, 5L);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 50L);

        selector.markFailed(A);
        Thread.sleep(120L);

        assertEquals(A, selector.selectForNew());

    }

    @Test
    public void usesAllEndpointsWhenEveryOneIsCircuitBroken() throws Exception {

        FakeStore store = new FakeStore();

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        selector.markFailed(A);
        selector.markFailed(B);

        // Refusing to connect at all would be worse than trying a suspect host
        GuacdEndpoint selected = selector.selectForNew();
        assertTrue(A.equals(selected) || B.equals(selected));

    }

    @Test
    public void failsWhenThePoolIsEmpty() {

        final GuacdSelector selector = new GuacdSelector(new FakeStore(),
                poolOf(java.util.Collections.<GuacdEndpoint>emptyList()), 15000L);

        assertThrows(GuacamoleException.class, new org.junit.jupiter.api.function.Executable() {

            @Override
            public void execute() throws Throwable {
                selector.selectForNew();
            }

        });

    }

}
