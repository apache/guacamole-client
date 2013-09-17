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

/**
 * A simple object to represent an auth token in the API.
 * 
 * @author James Muehlner
 */
public class APIAuthToken {
    
    /**
     * The auth token.
     */
    private String authToken;

    /**
     * Get the auth token.
     * @return The auth token. 
     */
    public String getAuthToken() {
        return authToken;
    }
    
    /**
     * Create a new APIAuthToken Object with the given auth token.
     * 
     * @param authToken The auth token to create the new APIAuthToken with. 
     */
    public APIAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
