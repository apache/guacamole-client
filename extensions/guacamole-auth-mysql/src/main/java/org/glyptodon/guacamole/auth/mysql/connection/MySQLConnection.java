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

package org.glyptodon.guacamole.auth.mysql.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import org.glyptodon.guacamole.auth.mysql.base.DirectoryObject;
import org.glyptodon.guacamole.auth.mysql.connectiongroup.MySQLRootConnectionGroup;
import org.glyptodon.guacamole.auth.mysql.socket.GuacamoleSocketService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * A MySQL based implementation of the Connection object.
 * @author James Muehlner
 */
public class MySQLConnection extends DirectoryObject<ConnectionModel>
    implements Connection {

    /**
     * Service for managing connections.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * Service for creating and tracking sockets.
     */
    @Inject
    private GuacamoleSocketService socketService;

    /**
     * Provider for lazy-loaded, permission-controlled configurations.
     */
    @Inject
    private Provider<MySQLGuacamoleConfiguration> configProvider;
    
    /**
     * The manually-set GuacamoleConfiguration, if any.
     */
    private GuacamoleConfiguration config = null;
    
    /**
     * Creates a new, empty MySQLConnection.
     */
    public MySQLConnection() {
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
    public String getParentIdentifier() {

        // Translate null parent to proper identifier
        String parentIdentifier = getModel().getParentIdentifier();
        if (parentIdentifier == null)
            return MySQLRootConnectionGroup.IDENTIFIER;

        return parentIdentifier;
        
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {

        // Translate root identifier back into null
        if (parentIdentifier != null
                && parentIdentifier.equals(MySQLRootConnectionGroup.IDENTIFIER))
            parentIdentifier = null;

        getModel().setParentIdentifier(parentIdentifier);

    }

    @Override
    public GuacamoleConfiguration getConfiguration() {

        // If configuration has been manually set, return that
        if (config != null)
            return config;

        // Otherwise, return permission-controlled configuration
        MySQLGuacamoleConfiguration restrictedConfig = configProvider.get();
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
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        return connectionService.connect(getCurrentUser(), this, info);
    }

    @Override
    public int getActiveConnections() {
        return socketService.getActiveConnections(this).size();
    }

}
