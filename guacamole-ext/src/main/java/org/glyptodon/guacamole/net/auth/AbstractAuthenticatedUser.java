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

package org.glyptodon.guacamole.net.auth;


/**
 * Basic implementation of an AuthenticatedUser which uses the username to
 * determine equality. Username comparison is case-sensitive.
 *
 * @author Michael Jumper
 */
public abstract class AbstractAuthenticatedUser implements AuthenticatedUser {

    /**
     * The name of this user.
     */
    private String username;

    @Override
    public String getIdentifier() {
        return username;
    }

    @Override
    public void setIdentifier(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        if (username == null) return 0;
        return username.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        // Not equal if null or not a User
        if (obj == null) return false;
        if (!(obj instanceof AbstractAuthenticatedUser)) return false;

        // Get username
        String objUsername = ((AbstractAuthenticatedUser) obj).username;

        // If null, equal only if this username is null
        if (objUsername == null) return username == null;

        // Otherwise, equal only if strings are identical
        return objUsername.equals(username);

    }

}
