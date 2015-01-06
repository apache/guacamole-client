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

package net.sourceforge.guacamole.net.auth.mysql;

import org.glyptodon.guacamole.net.auth.Credentials;

/**
 * Represents an authenticated user via their database ID and corresponding
 * credentials.
 *
 * @author Michael Jumper 
 */
public class AuthenticatedUser {

    /**
     * The database ID of this user.
     */
    private final int userID;

    /**
     * The credentials given when this user authenticated.
     */
    private final Credentials credentials;

    /**
     * Creates a new AuthenticatedUser associated with the given database ID
     * and credentials.
     *
     * @param userID
     *     The database ID of the user this object should represent.
     *
     * @param credentials 
     *     The credentials given by the user when they authenticated.
     */
    public AuthenticatedUser(int userID, Credentials credentials) {
        this.userID = userID;
        this.credentials = credentials;
    }

    /**
     * Returns the ID of this user.
     *
     * @return 
     *     The ID of this user.
     */
    public int getUserID() {
        return userID;
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

}
