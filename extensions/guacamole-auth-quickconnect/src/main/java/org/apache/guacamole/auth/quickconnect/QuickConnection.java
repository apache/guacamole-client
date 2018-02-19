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

package org.apache.guacamole.auth.quickconnect;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.InetGuacamoleSocket;
import org.apache.guacamole.net.SSLGuacamoleSocket;
import org.apache.guacamole.net.SimpleGuacamoleTunnel;
import org.apache.guacamole.net.auth.AbstractConnection;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * A type of Connection specific to this authentication extension.
 */
public class QuickConnection extends AbstractConnection {

    /**
     * Backing configuration, containing all sensitive information.
     */
    private GuacamoleConfiguration config;

    /**
     * Number of active connections.
     */
    private int activeConnections;

    /**
     * Empty connection constructor.
     */
    public QuickConnection() {

    }

    /**
     * Constructor that takes a name, identifier, and GuacamoleConfiguration
     * and builds a QuickConnection from it.
     *
     * @param name
     *     The name of the connection.
     *
     * @param identifier
     *     The unique identifier of this connection within this
     *     authentication module.
     *
     * @param config
     *     The GuacamoleConfiguration object to store in this
     *     QuickConnection.
     */
    public QuickConnection(String name, String identifier,
            GuacamoleConfiguration config) {

        setName(name);

        setIdentifier(identifier);

        setConfiguration(config);
        this.config = config;

        this.activeConnections = 0;

    }

    /**
     * Constructs a QuickConnection from a generic Connection
     * object, copying over the relevant data and initializing
     * the rest.
     *
     * @param object
     *     The generic Connection object to be copied.
     */
    public QuickConnection(Connection object) {

        setName(object.getName());
        setIdentifier(object.getIdentifier());
        setParentIdentifier(object.getParentIdentifier());
        setConfiguration(object.getConfiguration());
        this.config = object.getConfiguration();
        this.activeConnections = 0;

    }

    @Override
    public int getActiveConnections() {
        return activeConnections;
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.<String, String>emptyMap();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        // Do nothing - there are no attributes
    }

    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info)
            throws GuacamoleException {

        // Retrieve proxy configuration from environment
        Environment environment = new LocalEnvironment();
        GuacamoleProxyConfiguration proxyConfig = environment.getDefaultGuacamoleProxyConfiguration();

        // Get guacd connection parameters
        String hostname = proxyConfig.getHostname();
        int port = proxyConfig.getPort();

        GuacamoleSocket socket;

        // Determine socket type based on required encryption method
        switch (proxyConfig.getEncryptionMethod()) {

            // If guacd requires SSL, use it
            case SSL:
                socket = new ConfiguredGuacamoleSocket(
                    new SSLGuacamoleSocket(hostname, port),
                    config, info
                );
                break;

            // Connect directly via TCP if encryption is not enabled
            case NONE:
                socket = new ConfiguredGuacamoleSocket(
                    new InetGuacamoleSocket(hostname, port),
                    config, info
                );
                break;

            // Abort if encryption method is unknown
            default:
                throw new GuacamoleServerException("Unimplemented encryption method.");

        }

        return new SimpleGuacamoleTunnel(socket);

    }

    @Override
    public List<ConnectionRecord> getHistory() throws GuacamoleException {
        return Collections.<ConnectionRecord>emptyList();
    }

    @Override
    public Date getLastActive() {
        return null;
    }

}
