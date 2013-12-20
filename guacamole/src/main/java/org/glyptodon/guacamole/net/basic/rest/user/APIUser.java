package org.glyptodon.guacamole.net.basic.rest.user;

import org.codehaus.jackson.map.annotate.JsonSerialize;
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
 * A simple User to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class APIUser {
    
    /**
     * The username of this user.
     */
    private String username;
    
    /**
     * The password of this user.
     */
    private String password;
    
    /**
     * Construct a new APIUser from the provided User.
     * @param user The User to construct the APIUser from.
     */
    public APIUser(User user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
    }

    /**
     * Returns the username for this user.
     * @return The username for this user. 
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the username for this user.
     * @param username The username for this user.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the password for this user.
     * @return The password for this user.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password for this user.
     * @param password The password for this user.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
