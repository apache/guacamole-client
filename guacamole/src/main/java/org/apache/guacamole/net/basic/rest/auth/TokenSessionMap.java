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

package org.apache.guacamole.net.basic.rest.auth;

import org.apache.guacamole.net.basic.GuacamoleSession;

/**
 * Represents a mapping of auth token to Guacamole session for the REST 
 * authentication system.
 * 
 * @author James Muehlner
 */
public interface TokenSessionMap {
    
    /**
     * Registers that a user has just logged in with the specified authToken and
     * GuacamoleSession.
     * 
     * @param authToken The authentication token for the logged in user.
     * @param session The GuacamoleSession for the logged in user.
     */
    public void put(String authToken, GuacamoleSession session);
    
    /**
     * Get the GuacamoleSession for a logged in user. If the auth token does not
     * represent a user who is currently logged in, returns null. 
     * 
     * @param authToken The authentication token for the logged in user.
     * @return The GuacamoleSession for the given auth token, if the auth token
     *         represents a currently logged in user, null otherwise.
     */
    public GuacamoleSession get(String authToken);

    /**
     * Removes the GuacamoleSession associated with the given auth token.
     *
     * @param authToken The token to remove.
     * @return The GuacamoleSession for the given auth token, if the auth token
     *         represents a currently logged in user, null otherwise.
     */
    public GuacamoleSession remove(String authToken);
    
    /**
     * Shuts down this session map, disallowing future sessions and reclaiming
     * any resources.
     */
    public void shutdown();
    
}
