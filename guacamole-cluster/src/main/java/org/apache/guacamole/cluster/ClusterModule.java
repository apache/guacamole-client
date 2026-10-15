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

import com.google.inject.AbstractModule;
import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.guacd.GuacdPool;
import org.apache.guacamole.cluster.guacd.GuacdSelector;
import org.apache.guacamole.cluster.redis.RedisClusterStore;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice bindings for cluster coordination. When clustering is disabled, a
 * no-op store and a single-endpoint pool are bound, so every consumer can
 * depend on these types unconditionally.
 */
public class ClusterModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(ClusterModule.class);

    private static final long DEFAULT_HEARTBEAT_INTERVAL = 10000L;
    private static final long DEFAULT_STALE_WINDOW = 30000L;
    private static final long DEFAULT_CIRCUIT_BREAK = 15000L;
    private static final int DEFAULT_GUACD_PORT = 4822;
    private static final long DNS_CACHE_MS = 5000L;

    private final Environment environment;

    public ClusterModule(Environment environment) {
        this.environment = environment;
    }

    /**
     * @param environment
     *     The Guacamole server environment.
     *
     * @return
     *     true if cluster coordination is enabled.
     *
     * @throws GuacamoleException
     *     If the property cannot be read.
     */
    public static boolean isEnabled(Environment environment) throws GuacamoleException {
        return environment.getProperty(ClusterProperties.CLUSTER_ENABLED, false);
    }

    private String resolveNodeId() throws GuacamoleException {

        String configured = environment.getProperty(ClusterProperties.CLUSTER_NODE_ID);
        if (configured != null)
            return configured;

        String hostname = System.getenv("HOSTNAME");
        if (hostname == null)
            hostname = "guacamole";

        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);

    }

    private GuacdPool buildPool() throws GuacamoleException {

        int port = environment.getProperty(ClusterProperties.GUACD_CLUSTER_PORT,
                DEFAULT_GUACD_PORT);

        EncryptionMethod encryptionMethod =
                environment.getDefaultGuacamoleProxyConfiguration().getEncryptionMethod();

        String dns = environment.getProperty(ClusterProperties.GUACD_CLUSTER_DNS);
        if (dns != null)
            return GuacdPool.fromDns(dns, port, encryptionMethod, DNS_CACHE_MS);

        String hosts = environment.getProperty(ClusterProperties.GUACD_CLUSTER_HOSTS);
        if (hosts != null)
            return GuacdPool.fromHostList(hosts, port, encryptionMethod);

        // Fall back to the single configured guacd, which makes an unconfigured
        // pool behave exactly like unmodified Guacamole
        GuacamoleProxyConfiguration fallback =
                environment.getDefaultGuacamoleProxyConfiguration();

        return GuacdPool.fromHostList(
                fallback.getHostname() + ":" + fallback.getPort(),
                fallback.getPort(), fallback.getEncryptionMethod());

    }

    @Override
    protected void configure() {

        try {

            GuacdPool pool = buildPool();
            long circuitBreak = environment.getProperty(
                    ClusterProperties.GUACD_CIRCUIT_BREAK_DURATION, DEFAULT_CIRCUIT_BREAK);

            ClusterStore store;
            ClusterHeartbeat heartbeat;

            if (isEnabled(environment)) {

                String uri = environment.getRequiredProperty(
                        ClusterProperties.CLUSTER_REDIS_URI);

                long staleWindow = environment.getProperty(
                        ClusterProperties.CLUSTER_STALE_WINDOW, DEFAULT_STALE_WINDOW);

                long interval = environment.getProperty(
                        ClusterProperties.CLUSTER_HEARTBEAT_INTERVAL,
                        DEFAULT_HEARTBEAT_INTERVAL);

                store = new RedisClusterStore(uri, staleWindow, resolveNodeId());
                heartbeat = new ClusterHeartbeat(store, interval);
                heartbeat.start();

                logger.info("Cluster coordination is ENABLED against \"{}\".", uri);

            }
            else {
                store = new NoOpClusterStore();
                heartbeat = new ClusterHeartbeat(store, DEFAULT_HEARTBEAT_INTERVAL);
                logger.debug("Cluster coordination is disabled.");
            }

            bind(ClusterStore.class).toInstance(store);
            bind(ClusterHeartbeat.class).toInstance(heartbeat);
            bind(GuacdPool.class).toInstance(pool);
            bind(GuacdSelector.class)
                    .toInstance(new GuacdSelector(store, pool, circuitBreak));

        }
        catch (GuacamoleException e) {
            addError(new GuacamoleServerException(
                    "Unable to configure cluster coordination.", e));
        }

    }

}
