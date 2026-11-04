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

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ClusterHeartbeat.class);

    /**
     * The cluster store against which the heartbeat scores of all locally
     * owned tunnels are refreshed.
     */
    private final ClusterStore store;

    /**
     * The number of milliseconds to wait between the end of one heartbeat and
     * the start of the next.
     */
    private final long intervalMs;

    /**
     * All tunnels currently owned by this replica, keyed by tunnel UUID. Only
     * tunnels in this map are heartbeated; a tunnel removed from it will age
     * out of the cluster indexes once its last heartbeat becomes stale.
     */
    private final Map<String, TunnelRegistration> registrations =
            new ConcurrentHashMap<String, TunnelRegistration>();

    /**
     * Single-threaded executor which runs the periodic heartbeat. Its thread
     * is a daemon so that a forgotten shutdown can never prevent the JVM from
     * exiting.
     */
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
     * Creates a new ClusterHeartbeat which refreshes the tunnels registered
     * with it against the given store at the given interval. Heartbeating
     * does not begin until start() is invoked.
     *
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

    /**
     * Adds the given tunnel to the set of tunnels heartbeated by this replica.
     * If a tunnel with the same UUID is already registered, it is replaced.
     *
     * @param registration
     *     The registration of the locally owned tunnel to heartbeat.
     */
    public void add(TunnelRegistration registration) {
        registrations.put(registration.getTunnelUuid(), registration);
    }

    /**
     * Removes the tunnel having the given UUID from the set of tunnels
     * heartbeated by this replica. If no such tunnel is registered, this
     * function has no effect.
     *
     * @param tunnelUuid
     *     The UUID of the tunnel to stop heartbeating.
     */
    public void remove(String tunnelUuid) {
        registrations.remove(tunnelUuid);
    }

    /**
     * Returns a live view of the registrations of all tunnels currently
     * heartbeated by this replica.
     *
     * @return
     *     A live view of all registrations currently heartbeated by this
     *     replica.
     */
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

                // Catch everything, including errors, as any throwable
                // escaping a scheduled task cancels all future runs
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

    /**
     * Stops heartbeating immediately, interrupting any heartbeat currently in
     * progress. Tunnels registered with this heartbeat will subsequently age
     * out of the cluster indexes unless another heartbeat refreshes them.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

}
