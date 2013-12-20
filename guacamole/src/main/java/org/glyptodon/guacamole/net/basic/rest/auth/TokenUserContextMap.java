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

import org.glyptodon.guacamole.net.auth.UserContext;

/**
 * Represents a mapping of auth token to user context for the REST 
 * authentication system.
 * 
 * @author James Muehlner
 */
public interface TokenUserContextMap {
    
    /**
     * Registers that a user has just logged in with the specified authToken and
     * UserContext.
     * 
     * @param authToken The authentication token for the logged in user.
     * @param userContext The UserContext for the logged in user.
     */
    public void put(String authToken, UserContext userContext);
    
    /**
     * Get the UserContext for a logged in user. If the auth token does not
     * represent a user who is currently logged in, returns null. 
     * 
     * @param authToken The authentication token for the logged in user.
     * @return The UserContext for the given auth token, if the auth token
     *         represents a currently logged in user, null otherwise.
     */
    public UserContext get(String authToken);
}
