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

import com.google.inject.Guice;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.NoOpClusterStore;
import com.google.inject.Injector;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.guacd.GuacdSelector;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.apache.guacamole.properties.GuacamoleProperty;
import org.apache.guacamole.protocols.ProtocolInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test which verifies the configuration handling of ClusterModule: that
 * clustering is off unless explicitly enabled, and that the bindings produced
 * while it is off are the no-op implementations which make an unclustered
 * deployment behave like unmodified Guacamole. The module is driven by an
 * in-memory Environment, so no guacamole.properties file, Redis server or
 * guacd is required.
 */
public class ClusterModuleTest {

    /**
     * Minimal Environment backed by an in-memory property map. Only the methods
     * the cluster module actually calls are implemented; everything else is
     * unreachable from this code path.
     */
    private static class MapEnvironment implements Environment {

        /**
         * The property values exposed by this environment, keyed by property
         * name. Properties absent from this map are treated as unset.
         */
        private final Map<String, String> properties;

        /**
         * Creates a new MapEnvironment which exposes the given property
         * values.
         *
         * @param properties
         *     The property values to expose, keyed by property name.
         */
        MapEnvironment(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public File getGuacamoleHome() {
            return new File(System.getProperty("java.io.tmpdir"));
        }

        @Override
        public Map<String, ProtocolInfo> getProtocols() {
            return new HashMap<String, ProtocolInfo>();
        }

        @Override
        public ProtocolInfo getProtocol(String name) {
            return null;
        }

        @Override
        public GuacamoleProxyConfiguration getDefaultGuacamoleProxyConfiguration() {
            return new GuacamoleProxyConfiguration("localhost", 4822,
                    EncryptionMethod.NONE);
        }

        @Override
        public <Type> Type getProperty(GuacamoleProperty<Type> property)
                throws GuacamoleException {
            return property.parseValue(properties.get(property.getName()));
        }

        @Override
        public <Type> Type getProperty(GuacamoleProperty<Type> property, Type defaultValue)
                throws GuacamoleException {
            Type value = getProperty(property);
            return value == null ? defaultValue : value;
        }

        @Override
        public <Type> Type getRequiredProperty(GuacamoleProperty<Type> property)
                throws GuacamoleException {
            Type value = getProperty(property);
            if (value == null)
                throw new GuacamoleException("Missing required property: "
                        + property.getName());
            return value;
        }

    }

    /**
     * Verifies that clustering is disabled when no cluster properties are
     * set, so that upgrading an existing deployment never enables cluster
     * coordination implicitly.
     *
     * @throws Exception
     *     If the environment cannot be read. Not expected.
     */
    @Test
    public void clusteringIsDisabledByDefault() throws Exception {
        Environment environment = new MapEnvironment(new HashMap<String, String>());
        assertFalse(ClusterModule.isEnabled(environment));
    }

    /**
     * Verifies that, with clustering disabled, the module binds a
     * NoOpClusterStore and still binds a usable GuacdSelector. Consumers
     * inject both unconditionally, so a missing binding here would break
     * every unclustered deployment at startup.
     *
     * @throws Exception
     *     If the injector cannot be created. Not expected.
     */
    @Test
    public void bindsTheNoOpStoreWhenDisabled() throws Exception {

        Environment environment = new MapEnvironment(new HashMap<String, String>());
        Injector injector = Guice.createInjector(new ClusterModule(environment));

        assertTrue(injector.getInstance(ClusterStore.class) instanceof NoOpClusterStore);
        assertNotNull(injector.getInstance(GuacdSelector.class));

    }

    /**
     * Verifies that setting the cluster-enabled property to true enables
     * clustering. Only the enablement check is exercised; the injector is
     * not created, as doing so would attempt to connect to Redis.
     *
     * @throws Exception
     *     If the environment cannot be read. Not expected.
     */
    @Test
    public void enablesClusteringWhenConfigured() throws Exception {

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("cluster-enabled", "true");
        properties.put("cluster-redis-uri", "redis://localhost:6379");
        properties.put("guacd-cluster-hosts", "guacd-a:4822,guacd-b:4822");

        assertTrue(ClusterModule.isEnabled(new MapEnvironment(properties)));

    }

}
