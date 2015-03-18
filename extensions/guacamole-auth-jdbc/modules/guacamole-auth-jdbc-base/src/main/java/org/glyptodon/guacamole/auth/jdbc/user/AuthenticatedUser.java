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

package org.glyptodon.guacamole.auth.jdbc.user;

import javax.servlet.http.HttpServletRequest;
import org.glyptodon.guacamole.net.auth.Credentials;

/**
 * Associates a user with the credentials they used to authenticate.
 *
 * @author Michael Jumper 
 */
public class AuthenticatedUser {

    /**
     * The user that authenticated.
     */
    private final ModeledUser user;

    /**
     * The credentials given when this user authenticated.
     */
    private final Credentials credentials;

    /**
     * The host from which this user authenticated.
     */
    private final String remoteHost;

    /**
     * Derives the remote host of the authenticating user from the given
     * credentials object.
     *
     * @param credentials
     *     The credentials to derive the remote host from.
     *
     * @return
     *     The remote host from which the user with the given credentials is
     *     authenticating.
     */
    private static String getRemoteHost(Credentials credentials) {
        HttpServletRequest request = credentials.getRequest();
        return request.getRemoteAddr();
    }
    
    /**
     * Creates a new AuthenticatedUser associating the given user with their
     * corresponding credentials.
     *
     * @param user
     *     The user this object should represent.
     *
     * @param credentials 
     *     The credentials given by the user when they authenticated.
     */
    public AuthenticatedUser(ModeledUser user, Credentials credentials) {
        this.user = user;
        this.credentials = credentials;
        this.remoteHost = getRemoteHost(credentials);
    }

    /**
     * Returns the user that authenticated.
     *
     * @return 
     *     The user that authenticated.
     */
    public ModeledUser getUser() {
        return user;
    }

    /**
     * Returns the credentials given during authentication by this user.
     *
     * @return 
     *     The credentials given during authentication by this user.
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the host from which this user authenticated.
     *
     * @return
     *     The host from which this user authenticated.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

}
