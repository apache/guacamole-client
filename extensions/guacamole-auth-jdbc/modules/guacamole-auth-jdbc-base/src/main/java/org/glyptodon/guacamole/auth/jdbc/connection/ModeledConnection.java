/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.jdbc.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import org.glyptodon.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.jdbc.base.GroupedDirectoryObject;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * An implementation of the Connection object which is backed by a database
 * model.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class ModeledConnection extends GroupedDirectoryObject<ConnectionModel>
    implements Connection {

    /**
     * Service for managing connections.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * Service for creating and tracking tunnels.
     */
    @Inject
    private GuacamoleTunnelService tunnelService;

    /**
     * Provider for lazy-loaded, permission-controlled configurations.
     */
    @Inject
    private Provider<ModeledGuacamoleConfiguration> configProvider;
    
    /**
     * The manually-set GuacamoleConfiguration, if any.
     */
    private GuacamoleConfiguration config = null;

    /**
     * Creates a new, empty ModeledConnection.
     */
    public ModeledConnection() {
    }

    @Override
    public String getName() {
        return getModel().getName();
    }

    @Override
    public void setName(String name) {
        getModel().setName(name);
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {

        // If configuration has been manually set, return that
        if (config != null)
            return config;

        // Otherwise, return permission-controlled configuration
        ModeledGuacamoleConfiguration restrictedConfig = configProvider.get();
        restrictedConfig.init(getCurrentUser(), getModel());
        return restrictedConfig;

    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) {

        // Store manually-set configuration internally
        this.config = config;

        // Update model
        getModel().setProtocol(config.getProtocol());
        
    }

    @Override
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException {
        return connectionService.retrieveHistory(getCurrentUser(), this);
    }

    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info) throws GuacamoleException {
        return connectionService.connect(getCurrentUser(), this, info);
    }

    @Override
    public int getActiveConnections() {
        return tunnelService.getActiveConnections(this).size();
    }

}
