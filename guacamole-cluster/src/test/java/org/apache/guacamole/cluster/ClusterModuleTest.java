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

import com.google.inject.Guice;
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

public class ClusterModuleTest {

    /**
     * Minimal Environment backed by an in-memory property map. Only the methods
     * the cluster module actually calls are implemented; everything else is
     * unreachable from this code path.
     */
    private static class MapEnvironment implements Environment {

        private final Map<String, String> properties;

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

    @Test
    public void clusteringIsDisabledByDefault() throws Exception {
        Environment environment = new MapEnvironment(new HashMap<String, String>());
        assertFalse(ClusterModule.isEnabled(environment));
    }

    @Test
    public void bindsTheNoOpStoreWhenDisabled() throws Exception {

        Environment environment = new MapEnvironment(new HashMap<String, String>());
        Injector injector = Guice.createInjector(new ClusterModule(environment));

        assertTrue(injector.getInstance(ClusterStore.class) instanceof NoOpClusterStore);
        assertNotNull(injector.getInstance(GuacdSelector.class));

    }

    @Test
    public void enablesClusteringWhenConfigured() throws Exception {

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("cluster-enabled", "true");
        properties.put("cluster-redis-uri", "redis://localhost:6379");
        properties.put("guacd-cluster-hosts", "guacd-a:4822,guacd-b:4822");

        assertTrue(ClusterModule.isEnabled(new MapEnvironment(properties)));

    }

}
