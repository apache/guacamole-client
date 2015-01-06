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

package org.glyptodon.guacamole.token;

import org.glyptodon.guacamole.net.auth.Credentials;

/**
 * Utility class which provides access to standardized token names, as well as
 * facilities for generating those tokens from common objects.
 *
 * @author Michael Jumper
 */
public class StandardTokens {

    /**
     * The name of the username token added via addStandardTokens().
     */
    private static final String USERNAME_TOKEN = "GUAC_USERNAME";

    /**
     * The name of the password token added via addStandardTokens().
     */
    private static final String PASSWORD_TOKEN = "GUAC_PASSWORD";

    /**
     * This utility class should not be instantiated.
     */
    private StandardTokens() {}

    /**
     * Adds the standard username (GUAC_USERNAME) and password (GUAC_PASSWORD)
     * tokens to the given TokenFilter using the values from the given
     * Credentials object. If either the username or password are not set
     * within the given credentials, the corresponding token(s) will remain
     * unset.
     *
     * @param filter
     *     The TokenFilter to add standard username/password tokens to.
     *
     * @param credentials
     *     The Credentials containing the username/password to add.
     *
     */
    public static void addStandardTokens(TokenFilter filter, Credentials credentials) {

        // Add username token
        String username = credentials.getUsername();
        if (username != null)
            filter.setToken(USERNAME_TOKEN, username);
        
        // Add password token
        String password = credentials.getPassword();
        if (password != null)
            filter.setToken(PASSWORD_TOKEN, password);
        
    }
    

}
