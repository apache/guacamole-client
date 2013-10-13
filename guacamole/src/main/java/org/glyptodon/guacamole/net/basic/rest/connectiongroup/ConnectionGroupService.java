package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

import java.util.ArrayList;
import java.util.List;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;

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
 * A service for performing useful manipulations on REST ConnectionGroups.
 * 
 * @author James Muehlner
 */
public class ConnectionGroupService {
    
    /**
     * Converts a list of ConnectionGroup to a list of APIConnectionGroup
     * objects for exposing with the REST endpoints.
     * 
     * @param connectionGroups The ConnectionGroup to convert for REST endpoint use.
     * @return A List of APIConnectionGroup objects for use with the REST endpoint.
     */
    public List<APIConnectionGroup> convertConnectionGroupList(
            List<? extends ConnectionGroup> connectionGroups) {
        List<APIConnectionGroup> restConnectionGroups = new ArrayList<APIConnectionGroup>();
        
        for(ConnectionGroup connectionGroup : connectionGroups) {
            restConnectionGroups.add(new APIConnectionGroup(connectionGroup));
        }
            
        return restConnectionGroups;
    }
}
