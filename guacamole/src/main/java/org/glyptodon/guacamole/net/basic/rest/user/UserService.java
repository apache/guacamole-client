package org.glyptodon.guacamole.net.basic.rest.user;

import java.util.ArrayList;
import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;

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
 * A service for performing useful manipulations on REST Users.
 * 
 * @author James Muehlner
 */
public class UserService {
    
    /**
     * Converts a user directory to a list of APIUser objects for 
     * exposing with the REST endpoints.
     * 
     * @param userDirectory The user directory to convert for REST endpoint use.
     * @return A List of APIUser objects for use with the REST endpoint.
     * @throws GuacamoleException If an error occurs while converting the 
     *                            user directory.
     */
    public List<APIUser> convertUserList(Directory<String, User> userDirectory) 
            throws GuacamoleException {
        List<APIUser> restUsers = new ArrayList<APIUser>();
        
        for(String username : userDirectory.getIdentifiers()) {
            restUsers.add(new APIUser(userDirectory.get(username)));
        }
            
        return restUsers;
    }
    
}
