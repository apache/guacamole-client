/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.basic.rest.user;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.glyptodon.guacamole.net.auth.User;

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
