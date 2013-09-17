package org.glyptodon.guacamole.net.basic.rest.connection;

import java.util.ArrayList;
import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;

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

/**
 * A service for performing useful manipulations on REST Connections.
 * 
 * @author James Muehlner
 */
public class ConnectionService {
    
    /**
     * Converts a list of org.glyptodon.guacamole.net.auth.Connection to
     * Connection objects for exposure with the REST endpoints.
     * 
     * @param connections The org.glyptodon.guacamole.net.auth.Connection to
     *                    convert for REST endpoint use.
     * @return A List of Connection objects for use with the REST endpoint.
     * @throws GuacamoleException If an error occurs while converting the 
     *                            connections.
     */
    public List<Connection> convertConnectionList(List<? extends org.glyptodon.guacamole.net.auth.Connection> connections) 
            throws GuacamoleException {
        List<Connection> restConnections = new ArrayList<Connection>();
        
        for(org.glyptodon.guacamole.net.auth.Connection connection : connections) {
            restConnections.add(new Connection(connection));
        }
            
        return restConnections;
    }
}
