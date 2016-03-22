/*
 * Copyright (C) 2015 Glyptodon LLC
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

package org.apache.guacamole.rest.auth;

import java.util.Collections;
import java.util.List;

/**
 * A simple object which describes the result of an authentication operation,
 * including the resulting token.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class APIAuthenticationResult {

    /**
     * The unique token generated for the user that authenticated.
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
     * The identifiers of all data sources available to this user.
     */
    private final List<String> availableDataSources;

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
     * account associated with the current session.
     *
     * @return
     *     The unique identifier of the data source associated with the user
     *     account associated with the current session.
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Returns the identifiers of all data sources available to the user
     * associated with the current session.
     *
     * @return
     *     The identifiers of all data sources available to the user associated
     *     with the current session.
     */
    public List<String> getAvailableDataSources() {
        return availableDataSources;
    }

    /**
     * Create a new APIAuthenticationResult object containing the given data.
     *
     * @param authToken
     *     The unique token generated for the user that authenticated, to be
     *     used for the duration of their session.
     *
     * @param username
     *     The username of the user owning the given token.
     *
     * @param dataSource
     *     The unique identifier of the AuthenticationProvider which
     *     authenticated the user.
     *
     * @param availableDataSources
     *     The unique identifier of all AuthenticationProviders to which the
     *     user now has access.
     */
    public APIAuthenticationResult(String authToken, String username,
            String dataSource, List<String> availableDataSources) {
        this.authToken = authToken;
        this.username = username;
        this.dataSource = dataSource;
        this.availableDataSources = Collections.unmodifiableList(availableDataSources);
    }

}
