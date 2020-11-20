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

package org.glyptodon.guacamole.auth.json.user;

import com.google.inject.Inject;
import org.apache.guacamole.net.auth.AbstractAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * An implementation of AuthenticatedUser specific to the
 * JSONAuthenticationProvider, providing access to the decrypted contents of
 * the JSON provided during authentication.
 *
 * @author Michael Jumper
 */
public class AuthenticatedUser extends AbstractAuthenticatedUser {

    /**
     * Reference to the authentication provider associated with this
     * authenticated user.
     */
    @Inject
    private AuthenticationProvider authProvider;

    /**
     * The credentials provided when this user was authenticated.
     */
    private Credentials credentials;

    /**
     * The UserData object derived from the data submitted when this user was
     * authenticated.
     */
    private UserData userData;

    /**
     * Initializes this AuthenticatedUser using the given credentials and
     * UserData object. The provided UserData object MUST have been derived
     * from the data submitted when the user authenticated.
     *
     * @param credentials
     *     The credentials provided when this user was authenticated.
     *
     * @param userData
     *     The UserData object derived from the data submitted when this user
     *     was authenticated.
     */
    public void init(Credentials credentials, UserData userData) {
        this.credentials = credentials;
        this.userData = userData;
        setIdentifier(userData.getUsername());
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the UserData object derived from the data submitted when this
     * user was authenticated.
     *
     * @return
     *     The UserData object derived from the data submitted when this user
     *     was authenticated.
     */
    public UserData getUserData() {
        return userData;
    }

}
