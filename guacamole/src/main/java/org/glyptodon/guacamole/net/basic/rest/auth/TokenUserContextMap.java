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

package org.glyptodon.guacamole.net.basic.rest.auth;

import org.glyptodon.guacamole.net.auth.UserContext;

/**
 * Represents a mapping of auth token to user context for the REST 
 * authentication system.
 * 
 * @author James Muehlner
 */
public interface TokenUserContextMap {
    
    /**
     * Registers that a user has just logged in with the specified authToken and
     * UserContext.
     * 
     * @param authToken The authentication token for the logged in user.
     * @param userContext The UserContext for the logged in user.
     */
    public void put(String authToken, UserContext userContext);
    
    /**
     * Get the UserContext for a logged in user. If the auth token does not
     * represent a user who is currently logged in, returns null. 
     * 
     * @param authToken The authentication token for the logged in user.
     * @return The UserContext for the given auth token, if the auth token
     *         represents a currently logged in user, null otherwise.
     */
    public UserContext get(String authToken);
}
