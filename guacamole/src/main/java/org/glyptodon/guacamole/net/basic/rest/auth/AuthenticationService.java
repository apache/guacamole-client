package org.glyptodon.guacamole.net.basic.rest.auth;

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

import com.google.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;

/**
 * A service for performing authentication checks in REST endpoints.
 * 
 * @author James Muehlner
 */
public class AuthenticationService {
    
    /**
     * The map of auth tokens to users for the REST endpoints.
     */
    @Inject
    private TokenUserContextMap tokenUserMap;
    
    /**
     * Finds the UserContext for a given auth token, if the auth token represents
     * a currently logged in user. Throws an unauthorized error otherwise.
     * 
     * @param authToken The auth token to check against the map of logged in users.
     * @return The userContext that corresponds to the provided auth token.
     * @throws WebApplicationException If the auth token does not correspond to
     *                                 any logged in user.
     */
    public UserContext getUserContextFromAuthToken(String authToken) 
            throws WebApplicationException {
        
        // Try to get the userContext from the map of logged in users.
        UserContext userContext = tokenUserMap.get(authToken);
       
        // Authentication failed.
        if(userContext == null)
            throw new HTTPException(Status.UNAUTHORIZED, "Permission Denied.");
        
        return userContext;
    }
    
}
