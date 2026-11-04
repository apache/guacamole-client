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

package org.apache.guacamole.auth.jdbc.cluster;

import com.google.inject.AbstractModule;
import org.apache.guacamole.cluster.ClusterHeartbeat;
import org.apache.guacamole.cluster.ClusterProperties;
import org.apache.guacamole.cluster.ClusterSecurityPolicy;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.metrics.ClusterMetrics;
import org.apache.guacamole.cluster.NoOpClusterStore;
import java.util.List;
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
 * Guice module which binds the services used for cluster coordination: the
 * shared ClusterStore, the ClusterHeartbeat which keeps this node registered
 * in that store, the pool of guacd endpoints available to this node, and the
 * GuacdSelector which chooses among them. When clustering is disabled, a
 * no-op store and a pool containing only the default guacd are bound instead,
 * so that every consumer can depend on these types unconditionally and behaves
 * exactly as unmodified Guacamole would.
 */
public class ClusterModule extends AbstractModule {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ClusterModule.class);

    /**
     * The interval, in milliseconds, at which this node refreshes its
     * registration in the cluster store, if not overridden by
     * configuration.
     */
    private static final long DEFAULT_HEARTBEAT_INTERVAL = 10000L;

    /**
     * The amount of time, in milliseconds, after which a node which has not
     * refreshed its registration is considered dead by the rest of the
     * cluster, if not overridden by configuration.
     */
    private static final long DEFAULT_STALE_WINDOW = 30000L;

    /**
     * The amount of time, in milliseconds, for which a guacd endpoint that
     * failed to accept a connection is skipped by the GuacdSelector, if not
     * overridden by configuration.
     */
    private static final long DEFAULT_CIRCUIT_BREAK = 15000L;

    /**
     * The port used for guacd endpoints resolved from DNS, and for entries in
     * the guacd host list which do not specify a port explicitly, if not
     * overridden by configuration. This is the standard guacd port.
     */
    private static final int DEFAULT_GUACD_PORT = 4822;

    /**
     * The amount of time, in milliseconds, for which the result of resolving
     * the guacd DNS name is cached before it is resolved again.
     */
    private static final long DNS_CACHE_MS = 5000L;

    /**
     * The Guacamole server environment from which all cluster configuration
     * properties are read.
     */
    private final Environment environment;

    /**
     * Creates a new ClusterModule which reads its configuration from the
     * given environment.
     *
     * @param environment
     *     The Guacamole server environment from which all cluster
     *     configuration properties should be read.
     */
    public ClusterModule(Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns whether cluster coordination has been enabled within the given
     * environment. This is a convenience wrapper around
     * {@link ClusterProperties#isEnabled(Environment)}.
     *
     * @param environment
     *     The Guacamole server environment to read the property from.
     *
     * @return
     *     true if cluster coordination is enabled, false otherwise.
     *
     * @throws GuacamoleException
     *     If the property cannot be read.
     */
    public static boolean isEnabled(Environment environment) throws GuacamoleException {
        return ClusterProperties.isEnabled(environment);
    }

    /**
     * Returns the identifier which this node should use when registering
     * itself in the cluster store. An explicitly configured identifier is
     * used as-is. Otherwise, an identifier is generated from the HOSTNAME
     * environment variable (or "guacamole" if unset) plus a random suffix, so
     * that two nodes sharing a hostname, or a node restarted under the same
     * hostname, never collide with each other's registration.
     *
     * @return
     *     The identifier this node should use within the cluster.
     *
     * @throws GuacamoleException
     *     If the configured node identifier property cannot be read.
     */
    private String resolveNodeId() throws GuacamoleException {

        // An explicitly configured identifier always wins
        String configured = environment.getProperty(ClusterProperties.CLUSTER_NODE_ID);
        if (configured != null)
            return configured;

        // Otherwise derive one from the hostname, made unique per process
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null)
            hostname = "guacamole";

        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);

    }

    /**
     * Builds the pool of guacd endpoints which this node may connect to. The
     * pool is taken from, in order of precedence, a DNS name resolving to all
     * guacd instances, an explicit list of guacd hosts, or the single default
     * guacd configured in guacamole.properties.
     *
     * @return
     *     The pool of guacd endpoints which this node may connect to.
     *
     * @throws GuacamoleException
     *     If any of the properties defining the pool cannot be read or
     *     parsed.
     */
    private GuacdPool buildPool() throws GuacamoleException {

        int port = environment.getProperty(ClusterProperties.GUACD_CLUSTER_PORT,
                DEFAULT_GUACD_PORT);

        EncryptionMethod encryptionMethod =
                environment.getDefaultGuacamoleProxyConfiguration().getEncryptionMethod();

        // A DNS name takes precedence, as it tracks guacd instances being
        // added or removed without a configuration change
        String dns = environment.getProperty(ClusterProperties.GUACD_CLUSTER_DNS);
        if (dns != null)
            return GuacdPool.fromDns(dns, port, encryptionMethod, DNS_CACHE_MS);

        // Otherwise use a static list of guacd hosts, if provided
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

            // The guacd pool and selector are bound whether or not clustering
            // is enabled, as tunnel creation always goes through them
            GuacdPool pool = buildPool();
            long circuitBreak = environment.getProperty(
                    ClusterProperties.GUACD_CIRCUIT_BREAK_DURATION, DEFAULT_CIRCUIT_BREAK);

            ClusterStore store;
            ClusterHeartbeat heartbeat;

            if (isEnabled(environment)) {

                // Read the Redis connection and timing configuration
                String uri = environment.getRequiredProperty(
                        ClusterProperties.CLUSTER_REDIS_URI);

                long staleWindow = environment.getProperty(
                        ClusterProperties.CLUSTER_STALE_WINDOW, DEFAULT_STALE_WINDOW);

                long interval = environment.getProperty(
                        ClusterProperties.CLUSTER_HEARTBEAT_INTERVAL,
                        DEFAULT_HEARTBEAT_INTERVAL);

                boolean allowInsecure = environment.getProperty(
                        ClusterProperties.CLUSTER_ALLOW_INSECURE_REDIS, false);

                // Before the store is built, so that a refused URI is never
                // connected to
                String posture = ClusterSecurityPolicy.check(uri, allowInsecure);

                // Connect to Redis and begin advertising this node
                store = new RedisClusterStore(uri, staleWindow, resolveNodeId(),
                        environment.getProperty(ClusterProperties.CLUSTER_REDIS_CA_CERT));
                heartbeat = new ClusterHeartbeat(store, interval);
                heartbeat.start();

                logger.info("Cluster coordination is ENABLED. {}", posture);

                // Log at ERROR and continue rather than refusing to start: a
                // permission problem is fixable without a redeploy, and
                // refusing would turn a degraded cluster into an outage
                List<String> denied = store.selfCheck();
                ClusterMetrics.gauge(
                        "guacamole_cluster_selfcheck_denied_operations", denied.size());
                if (!denied.isEmpty())
                    logger.error("Cluster coordination is enabled but Redis "
                            + "denied or failed {} of the operations it depends "
                            + "on: {}. Clustering will appear to work while "
                            + "silently falling back to per-replica behaviour. "
                            + "The Redis user must be allowed every listed command "
                            + "on keys and channels matching \"guac:*\".",
                            denied.size(), denied);

            }
            // Clustering disabled: bind a store which coordinates nothing and
            // a heartbeat which is never started
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
