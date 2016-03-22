/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.apache.guacamole.rest.user;

import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.apache.guacamole.net.auth.User;

/**
 * A simple User to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
     * Map of all associated attributes by attribute identifier.
     */
    private Map<String, String> attributes;

    /**
     * Construct a new empty APIUser.
     */
    public APIUser() {}
    
    /**
     * Construct a new APIUser from the provided User.
     * @param user The User to construct the APIUser from.
     */
    public APIUser(User user) {

        // Set user information
        this.username = user.getIdentifier();
        this.password = user.getPassword();

        // Associate any attributes
        this.attributes = user.getAttributes();

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

    /**
     * Returns a map of all attributes associated with this user. Each entry
     * key is the attribute identifier, while each value is the attribute
     * value itself.
     *
     * @return
     *     The attribute map for this user.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Sets the map of all attributes associated with this user. Each entry key
     * is the attribute identifier, while each value is the attribute value
     * itself.
     *
     * @param attributes
     *     The attribute map for this user.
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

}
