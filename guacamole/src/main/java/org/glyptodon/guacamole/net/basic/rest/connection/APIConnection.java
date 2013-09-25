package org.glyptodon.guacamole.net.basic.rest.connection;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * A simple connection to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
public class APIConnection implements Connection {

    /**
     * The name of this connection.
     */
    private String name;
    
    /**
     * The identifier of this connection.
     */
    private String identifier;
    
    /**
     * The configuration associated with this connection.
     */
    private GuacamoleConfiguration configuration;
    
    /**
     * The history records associated with this connection.
     */
    private List<? extends ConnectionRecord> history;
    
    /**
     * Create an empty APIConnection.
     */
    public APIConnection() {}
    
    /**
     * Create an APIConnection from a Connection record.
     * @param connection The connection to create this APIConnection from.
     * @throws GuacamoleException If a problem is encountered while
     *                            instantiating this new APIConnection.
     */
    public APIConnection(Connection connection) 
            throws GuacamoleException {
        this.name = connection.getName();
        this.identifier = connection.getIdentifier();
        this.configuration = connection.getConfiguration();
        this.history = connection.getHistory();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String getIdentifier() {
        return identifier;
    }
    
    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void setConfiguration(GuacamoleConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<? extends ConnectionRecord> getHistory() {
        return history;
    }

    /**
     * Set the history records for this connection.
     * @param history The history records for this connection.
     */
    public void setHistory(List<? extends ConnectionRecord> history) {
        this.history = history;
    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported.");
    }
    
}
