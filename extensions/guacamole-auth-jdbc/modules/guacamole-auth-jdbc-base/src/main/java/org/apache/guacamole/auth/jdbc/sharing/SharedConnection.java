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

package org.apache.guacamole.auth.jdbc.sharing;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.activeconnection.TrackedActiveConnection;
import org.apache.guacamole.auth.jdbc.connectiongroup.RootConnectionGroup;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * A Connection which joins an active connection, limited by restrictions
 * defined by a sharing profile.
 *
 * @author Michael Jumper
 */
public class SharedConnection implements Connection {

    /**
     * Service for establishing tunnels to Guacamole connections.
     */
    @Inject
    private GuacamoleTunnelService tunnelService;

    /**
     * Randomly-generated unique identifier, guaranteeing this shared connection
     * does not duplicate the identifying information of the underlying
     * connection being shared.
     */
    private final String identifier = UUID.randomUUID().toString();

    /**
     * The user that successfully authenticated to obtain access to this
     * SharedConnection.
     */
    private SharedConnectionUser user;

    /**
     * The active connection being shared.
     */
    private TrackedActiveConnection activeConnection;

    /**
     * The sharing profile which dictates the level of access provided to a user
     * of the shared connection.
     */
    private ModeledSharingProfile sharingProfile;

    /**
     * Creates a new SharedConnection which can be used to join the connection
     * described by the given SharedConnectionDefinition.
     *
     * @param user
     *     The user that successfully authenticated to obtain access to this
     *     SharedConnection.
     *
     * @param definition
     *     The SharedConnectionDefinition dictating the connection being shared
     *     and any associated restrictions.
     */
    public void init(SharedConnectionUser user, SharedConnectionDefinition definition) {
        this.user = user;
        this.activeConnection = definition.getActiveConnection();
        this.sharingProfile = definition.getSharingProfile();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        throw new UnsupportedOperationException("Shared connections are immutable.");
    }

    @Override
    public String getName() {
        return sharingProfile.getName();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Shared connections are immutable.");
    }

    @Override
    public String getParentIdentifier() {
        return RootConnectionGroup.IDENTIFIER;
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {
        throw new UnsupportedOperationException("Shared connections are immutable.");
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {
        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(activeConnection.getConnection().getConfiguration().getProtocol());
        return config;
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) {
        throw new UnsupportedOperationException("Shared connections are immutable.");
    }

    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info)
            throws GuacamoleException {
        return tunnelService.getGuacamoleTunnel(user, activeConnection,
                sharingProfile, info);
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.<String, String>emptyMap();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        // Do nothing - no attributes supported
    }

    @Override
    public List<? extends ConnectionRecord> getHistory()
            throws GuacamoleException {
        return Collections.<ConnectionRecord>emptyList();
    }

    @Override
    public Set<String> getSharingProfileIdentifiers()
            throws GuacamoleException {
        return Collections.<String>emptySet();
    }

    @Override
    public int getActiveConnections() {
        return 0;
    }

}
