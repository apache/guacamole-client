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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.basic.GuacamoleSession;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic, HashMap-based implementation of the TokenSessionMap with support
 * for session timeouts.
 * 
 * @author James Muehlner
 */
public class BasicTokenSessionMap implements TokenSessionMap {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(BasicTokenSessionMap.class);
    
    /**
     * The last time a user with a specific auth token accessed the API. 
     */
    private final Map<String, Long> lastAccessTimeMap = new HashMap<String, Long>();
    
    /**
     * Keeps track of the authToken to GuacamoleSession mapping.
     */
    private final Map<String, GuacamoleSession> sessionMap = new HashMap<String, GuacamoleSession>();
    
    /**
     * The session timeout configuration for an API session, in milliseconds.
     */
    private final long SESSION_TIMEOUT;
    
    /**
     * Create a new BasicTokenGuacamoleSessionMap and initialize the session timeout value.
     */
    public BasicTokenSessionMap() {
        
        // Set up the SESSION_TIMEOUT value, with a one hour default.
        long sessionTimeoutValue;
        try {
            sessionTimeoutValue = GuacamoleProperties.getProperty(BasicGuacamoleProperties.API_SESSION_TIMEOUT, 3600000l);
        }
        catch (GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while reading API_SESSION_TIMEOUT property. Defaulting to 1 hour.", e);
            sessionTimeoutValue = 3600000l;
        }
        
        SESSION_TIMEOUT = sessionTimeoutValue;
        
    }
    
    /**
     * Evict an authentication token from the map of logged in users and last
     * access times.
     * 
     * @param authToken The authentication token to evict.
     */
    private void evict(String authToken) {
        sessionMap.remove(authToken);
        lastAccessTimeMap.remove(authToken);
    }
    
    /**
     * Log that the user represented by this auth token has just used the API.
     * 
     * @param authToken The authentication token to record access time for.
     */
    private void logAccessTime(String authToken) {
        lastAccessTimeMap.put(authToken, new Date().getTime());
    }
    
    /**
     * Check if a session has timed out.
     * @param authToken The auth token for the session.
     * @return True if the session has timed out, false otherwise.
     */
    private boolean sessionHasTimedOut(String authToken) {

        if (!lastAccessTimeMap.containsKey(authToken))
            return true;
        
        long lastAccessTime = lastAccessTimeMap.get(authToken);
        long currentTime = new Date().getTime();
        
        return currentTime - lastAccessTime > SESSION_TIMEOUT;

    }

    @Override
    public GuacamoleSession get(String authToken) {
        
        // If the session has timed out, evict the token and force the user to log in again
        if (sessionHasTimedOut(authToken)) {
            evict(authToken);
            return null;
        }
        
        // Update the last access time and return the GuacamoleSession
        logAccessTime(authToken);
        return sessionMap.get(authToken);

    }

    @Override
    public void put(String authToken, GuacamoleSession session) {
        
        // Update the last access time, and create the token/GuacamoleSession mapping
        logAccessTime(authToken);
        sessionMap.put(authToken, session);

    }
    
}
