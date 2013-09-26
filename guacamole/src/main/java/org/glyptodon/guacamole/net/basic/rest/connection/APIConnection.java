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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;

/**
 * A simple connection to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
public class APIConnection {

    /**
     * The name of this connection.
     */
    private String name;
    
    /**
     * The identifier of this connection.
     */
    private String identifier;
    
    /**
     * The history records associated with this connection.
     */
    private List<? extends ConnectionRecord> history;

    /**
     * Map of all associated parameter values, indexed by parameter name.
     */
    private Map<String, String> parameters = new HashMap<String, String>();
    
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
        this.history = connection.getHistory();
    }

    /**
     * Returns the name of this connection.
     * @return The name of this connection.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this connection.
     * @param name The name of this connection.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the unique identifier for this connection.
     * @return The unique identifier for this connection.
     */
    public String getIdentifier() {
        return identifier;
    }
    /**
     * Sets the unique identifier for this connection.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the history records associated with this connection.
     * @return The history records associated with this connection.
     */
    public List<? extends ConnectionRecord> getHistory() {
        return history;
    }

    /**
     * Returns the parameter map for this connection.
     * @return The parameter map for this connection.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameter map for this connection.
     * @param parameters The parameter map for this connection.
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
}
