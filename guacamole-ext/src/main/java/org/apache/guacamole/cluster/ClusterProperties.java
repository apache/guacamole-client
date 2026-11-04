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

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.LongGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Every guacamole.properties key read by the cluster module.
 */
public class ClusterProperties {

    /**
     * A property which configures whether cluster coordination is active.
     * When false, every code path behaves exactly as it does in a
     * non-clustered Guacamole deployment.
     */
    public static final BooleanGuacamoleProperty CLUSTER_ENABLED =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-enabled";
        }

    };

    /**
     * A property which specifies the Lettuce URI of the Redis server holding
     * cluster state, for example "redis://host:6379" or "rediss://host:6379"
     * for TLS.
     */
    public static final StringGuacamoleProperty CLUSTER_REDIS_URI =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-redis-uri";
        }

    };

    /**
     * A property which configures whether cluster state may be held in a
     * Redis which is unauthenticated, unencrypted, or both. Defaults to false.
     *
     * The keyspace holds session identity, so an open Redis exposes who is
     * logged in and from where to anything that can reach the port. Setting
     * this to true is appropriate for a private test environment and nowhere
     * else.
     */
    public static final BooleanGuacamoleProperty CLUSTER_ALLOW_INSECURE_REDIS =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-allow-insecure-redis";
        }

    };

    /**
     * A property which specifies the path to a PEM certificate authority the
     * Redis server's certificate is signed by.
     *
     * Needed only for a privately issued certificate. Trust is applied to the
     * Redis client alone, never to the JVM's default truststore, which also
     * governs SAML metadata retrieval and every other outbound HTTPS call.
     */
    public static final StringGuacamoleProperty CLUSTER_REDIS_CA_CERT =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-redis-ca-cert";
        }

    };

    /**
     * A property which configures whether the Prometheus metrics endpoint is
     * served. Defaults to false.
     */
    public static final BooleanGuacamoleProperty CLUSTER_METRICS_ENABLED =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-metrics-enabled";
        }

    };

    /**
     * A property which specifies the bearer token required by the metrics
     * endpoint.
     *
     * The endpoint cannot require a Guacamole session, because a scraper does
     * not have one, and it cannot be open, because a reverse proxy in front of
     * Guacamole will typically expose every path of this application.
     * Enabling metrics without a token is refused at startup.
     */
    public static final StringGuacamoleProperty CLUSTER_METRICS_TOKEN =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-metrics-token";
        }

    };

    /**
     * A property which specifies the identity of this replica within the
     * cluster. Defaults to the HOSTNAME environment variable plus a random
     * suffix.
     */
    public static final StringGuacamoleProperty CLUSTER_NODE_ID =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-node-id";
        }

    };

    /**
     * A property which specifies the number of milliseconds between
     * heartbeats refreshing the tunnels owned by this replica.
     */
    public static final LongGuacamoleProperty CLUSTER_HEARTBEAT_INTERVAL =
            new LongGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-heartbeat-interval";
        }

    };

    /**
     * A property which specifies the number of milliseconds after which an
     * unrefreshed cluster entry is considered dead.
     */
    public static final LongGuacamoleProperty CLUSTER_STALE_WINDOW =
            new LongGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-stale-window";
        }

    };

    /**
     * A property which specifies a comma-separated list of "host" or
     * "host:port" guacd instances to which connections may be distributed.
     */
    public static final StringGuacamoleProperty GUACD_CLUSTER_HOSTS =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-cluster-hosts";
        }

    };

    /**
     * A property which specifies a hostname resolving to every available
     * guacd instance, such as a Kubernetes headless Service.
     */
    public static final StringGuacamoleProperty GUACD_CLUSTER_DNS =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-cluster-dns";
        }

    };

    /**
     * A property which specifies the port on which pooled guacd instances
     * listen.
     */
    public static final IntegerGuacamoleProperty GUACD_CLUSTER_PORT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-cluster-port";
        }

    };

    /**
     * A property which specifies the number of milliseconds a guacd instance
     * is skipped after a failed connection.
     */
    public static final LongGuacamoleProperty GUACD_CIRCUIT_BREAK_DURATION =
            new LongGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-circuit-break-duration";
        }

    };

    /**
     * Returns whether cluster coordination is enabled.
     *
     * This lives here rather than on ClusterModule so that it can be read
     * without Guice on the classpath. ClusterModule extends AbstractModule, so
     * merely naming that class drags Guice into any extension that asks the
     * question -- guacamole-auth-ban among them.
     *
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
        return environment.getProperty(CLUSTER_ENABLED, false);
    }

}
