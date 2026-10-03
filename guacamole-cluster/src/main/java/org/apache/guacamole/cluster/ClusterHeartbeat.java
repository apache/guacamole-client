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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically refreshes the cluster-wide heartbeat score of every tunnel owned
 * by this replica. A replica that stops heartbeating has its tunnels aged out of
 * every cluster index automatically, which is what frees seats and clears the
 * admin view after a crash.
 */
public class ClusterHeartbeat {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHeartbeat.class);

    private final ClusterStore store;
    private final long intervalMs;

    private final Map<String, TunnelRegistration> registrations =
            new ConcurrentHashMap<String, TunnelRegistration>();

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "guacamole-cluster-heartbeat");
                    thread.setDaemon(true);
                    return thread;
                }

            });

    /**
     * @param store
     *     The cluster store to refresh against.
     *
     * @param intervalMs
     *     Milliseconds between heartbeats. Must be well below the stale window.
     */
    public ClusterHeartbeat(ClusterStore store, long intervalMs) {
        this.store = store;
        this.intervalMs = intervalMs;
    }

    public void add(TunnelRegistration registration) {
        registrations.put(registration.getTunnelUuid(), registration);
    }

    public void remove(String tunnelUuid) {
        registrations.remove(tunnelUuid);
    }

    public Collection<TunnelRegistration> getRegistrations() {
        return registrations.values();
    }

    /**
     * Begins heartbeating. A failure to reach the store is logged and the
     * schedule continues; throwing out of the scheduled task would silently
     * cancel all future heartbeats.
     */
    public void start() {
        executor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    store.heartbeat(registrations.values());
                }
                catch (Throwable e) {
                    logger.warn("Cluster heartbeat failed. Tunnels owned by this node "
                            + "may age out of the cluster if this persists.", e);
                }
            }

        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

}
