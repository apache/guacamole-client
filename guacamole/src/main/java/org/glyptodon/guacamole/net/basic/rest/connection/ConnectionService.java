package org.glyptodon.guacamole.net.basic.rest.connection;

import java.util.ArrayList;
import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.Directory;

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
     * Converts a Connection Directory to a list of APIConnection objects for 
     * exposing with the REST endpoints.
     * 
     * @param connectionDirectory The Connection Directory to convert for REST endpoint use.
     * @return A List of APIConnection objects for use with the REST endpoint.
     * @throws GuacamoleException If an error occurs while converting the 
     *                            connection directory.
     */
    public List<APIConnection> convertConnectionList(Directory<String, Connection> connectionDirectory) 
            throws GuacamoleException {
        List<APIConnection> restConnections = new ArrayList<APIConnection>();
        
        for(String connectionID : connectionDirectory.getIdentifiers()) {
            restConnections.add(new APIConnection(connectionDirectory.get(connectionID)));
        }
            
        return restConnections;
    }
}
