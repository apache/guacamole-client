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

import org.apache.guacamole.cluster.metrics.ClusterMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts that a degraded operation is counted, not merely logged. Needs no
 * Docker: an unreachable Redis is what produces the degradation.
 */
public class MetricsRecordedTest {

    /**
     * A Redis URI on reserved port 1, where nothing listens, so that every
     * store operation degrades.
     */
    private static final String UNREACHABLE = "redis://127.0.0.1:1";

    /**
     * Clears every metric recorded by earlier tests, since
     * {@link ClusterMetrics} is static and shared across the JVM.
     */
    @BeforeEach
    public void setUp() {
        ClusterMetrics.reset();
    }

    /**
     * Verifies that a read which fails because Redis is unreachable
     * increments the operation failure counter, labelled with the operation
     * that degraded.
     */
    @Test
    public void aDegradedReadIsCounted() {

        RedisClusterStore store = new RedisClusterStore(UNREACHABLE, 30000L, "node-1");

        try {

            store.getAuthenticationFailures("10.0.0.1");

            String text = ClusterMetrics.render();
            assertTrue(text.contains(
                    "guacamole_cluster_operation_failures_total"
                            + "{operation=\"getAuthenticationFailures\"} 1"), text);

        }

        finally {
            store.shutdown();
        }

    }

    /**
     * Verifies that, after a failed operation marks the store unavailable,
     * publishing metrics reports the store availability gauge as 0.
     */
    @Test
    public void availabilityIsPublishedAsAGauge() {

        RedisClusterStore store = new RedisClusterStore(UNREACHABLE, 30000L, "node-1");

        try {

            store.getAuthenticationFailures("10.0.0.1");
            store.publishMetrics();

            assertTrue(ClusterMetrics.render().contains(
                    "guacamole_cluster_store_available 0"), ClusterMetrics.render());

        }

        finally {
            store.shutdown();
        }

    }

    /**
     * Verifies that failures provoked by the startup self-check are not
     * recorded as operation failures.
     */
    @Test
    public void theSelfCheckDoesNotCountAsRuntimeDegradation() {

        RedisClusterStore store = new RedisClusterStore(UNREACHABLE, 30000L, "node-1");

        try {

            // selfCheck deliberately provokes every failure it can find, and
            // reports them through its return value. Counting them here too
            // would make a startup probe indistinguishable from a cluster
            // degrading under load
            store.selfCheck();

            assertFalse(ClusterMetrics.render().contains(
                    "guacamole_cluster_operation_failures_total"),
                    ClusterMetrics.render());

        }

        finally {
            store.shutdown();
        }

    }

}
