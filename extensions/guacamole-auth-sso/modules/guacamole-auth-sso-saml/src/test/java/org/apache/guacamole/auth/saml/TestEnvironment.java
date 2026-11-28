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

package org.apache.guacamole.auth.saml;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.properties.GuacamoleProperty;
import org.apache.guacamole.protocols.ProtocolInfo;

/**
 * In-memory {@link Environment} implementation for unit tests, backed by a
 * configurable {@code GUACAMOLE_HOME} directory and property map.
 */
public class TestEnvironment implements Environment {

    /**
     * The temporary GUACAMOLE_HOME directory for this test environment.
     */
    private final File guacamoleHome;

    /**
     * In-memory guacamole.properties key-value pairs.
     */
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Creates a new TestEnvironment using the given directory as
     * GUACAMOLE_HOME.
     *
     * @param guacamoleHome
     *     The directory to expose as GUACAMOLE_HOME.
     */
    public TestEnvironment(File guacamoleHome) {
        this.guacamoleHome = guacamoleHome;
    }

    /**
     * Sets a raw property value in this test environment.
     *
     * @param name
     *     The property name.
     *
     * @param value
     *     The raw string value, or null to remove the property.
     *
     * @return
     *     This TestEnvironment instance for chaining.
     */
    public TestEnvironment setProperty(String name, String value) {
        if (value == null)
            properties.remove(name);
        else
            properties.put(name, value);
        return this;
    }

    @Override
    public File getGuacamoleHome() {
        return guacamoleHome;
    }

    @Override
    public Map<String, ProtocolInfo> getProtocols() {
        return Collections.emptyMap();
    }

    @Override
    public ProtocolInfo getProtocol(String name) {
        return null;
    }

    @Override
    public <Type> Type getProperty(GuacamoleProperty<Type> property)
            throws GuacamoleException {
        return property.parseValue(properties.get(property.getName()));
    }

    @Override
    public <Type> Type getProperty(GuacamoleProperty<Type> property,
            Type defaultValue) throws GuacamoleException {
        Type value = getProperty(property);
        return value != null ? value : defaultValue;
    }

    @Override
    public <Type> Type getRequiredProperty(GuacamoleProperty<Type> property)
            throws GuacamoleException {
        Type value = getProperty(property);
        if (value == null)
            throw new GuacamoleServerException("Property \""
                    + property.getName() + "\" is required.");
        return value;
    }

    @Override
    public GuacamoleProxyConfiguration getDefaultGuacamoleProxyConfiguration() {
        return new GuacamoleProxyConfiguration("localhost", 4822, false);
    }

}
