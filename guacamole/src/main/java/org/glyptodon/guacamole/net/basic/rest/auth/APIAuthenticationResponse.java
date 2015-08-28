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

package org.glyptodon.guacamole.net.basic.rest.auth;

/**
 * A simple object to represent an auth token/username pair in the API.
 * 
 * @author James Muehlner
 */
public class APIAuthenticationResponse {
    
    /**
     * The auth token.
     */
    private final String authToken;
    
    
    /**
     * The username of the user that authenticated.
     */
    private final String username;

    /**
     * The unique identifier of the data source from which this user account
     * came. Although this user account may exist across several data sources
     * (AuthenticationProviders), this will be the unique identifier of the
     * AuthenticationProvider that authenticated this user for the current
     * session.
     */
    private final String dataSource;

    /**
     * Returns the unique authentication token which identifies the current
     * session.
     *
     * @return
     *     The user's authentication token.
     */
    public String getAuthToken() {
        return authToken;
    }
    
    /**
     * Returns the user identified by the authentication token associated with
     * the current session.
     *
     * @return
     *      The user identified by this authentication token.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the unique identifier of the data source associated with the user
     * account associated with this auth token.
     * 
     * @return 
     *     The unique identifier of the data source associated with the user
     *     account associated with this auth token.
     */
    public String getDataSource() {
        return dataSource;
    }
    
    /**
     * Create a new APIAuthToken Object with the given auth token.
     *
     * @param dataSource
     *     The unique identifier of the AuthenticationProvider which
     *     authenticated the user.
     *
     * @param authToken
     *     The auth token to create the new APIAuthToken with.
     *
     * @param username
     *     The username of the user owning the given token.
     */
    public APIAuthenticationResponse(String dataSource, String authToken, String username) {
        this.dataSource = dataSource;
        this.authToken = authToken;
        this.username = username;
    }

}
