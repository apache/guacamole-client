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


package org.apache.guacamole.metrics;

import com.google.inject.servlet.ServletModule;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterProperties;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.rest.auth.AuthenticationService;

/**
 * Guice servlet module which binds the cluster metrics endpoint at
 * "/metrics". The endpoint is always bound, but answers only when the
 * "cluster-metrics-enabled" property is true, and only to requests carrying
 * the bearer token configured by "cluster-metrics-token". Startup is refused
 * if the endpoint is enabled without a token, so that it can never be served
 * unguarded.
 */
public class MetricsModule extends ServletModule {

    /**
     * The Guacamole server environment, from which the metrics-related
     * properties are read.
     */
    private final Environment environment;

    /**
     * Creates a new MetricsModule which configures the metrics endpoint using
     * the properties of the given environment.
     *
     * @param environment
     *     The Guacamole server environment, from which the metrics-related
     *     properties are read.
     */
    public MetricsModule(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void configureServlets() {

        try {

            // Read whether the endpoint is enabled and the token guarding it
            boolean enabled = environment.getProperty(
                    ClusterProperties.CLUSTER_METRICS_ENABLED, false);

            String token = environment.getProperty(
                    ClusterProperties.CLUSTER_METRICS_TOKEN);

            // Fail closed: an enabled endpoint without a token is a
            // configuration error, not an open endpoint
            if (enabled && (token == null || token.isEmpty()))
                throw new GuacamoleServerException("\"cluster-metrics-enabled\" "
                        + "is true but \"cluster-metrics-token\" is unset. The "
                        + "metrics endpoint is reachable wherever the web "
                        + "application is, so it is never served "
                        + "unguarded.");

            // getProvider defers resolution until after the injector exists,
            // which is what lets the endpoint sample the web application's own
            // store rather than building a second one
            serve("/metrics").with(new MetricsServlet(enabled, token,
                    getProvider(AuthenticationService.class)));

        }

        // Surface property errors as Guice configuration errors, which abort
        // creation of the injector and therefore startup
        catch (GuacamoleException e) {
            addError(e);
        }

    }

}
