package org.glyptodon.guacamole.net.basic.rest.auth;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic, HashMap-based implementation of the TokenUserContextMap with support
 * for session timeouts.
 * 
 * @author James Muehlner
 */
public class BasicTokenUserContextMap implements TokenUserContextMap {

    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(BasicTokenUserContextMap.class);
    
    /**
     * The last time a user with a specific auth token accessed the API. 
     */
    private Map<String, Long> lastAccessTimeMap = new HashMap<String, Long>();
    
    /**
     * Keeps track of the authToken to UserContext mapping.
     */
    private Map<String, UserContext> userContextMap = new HashMap<String, UserContext>();
    
    /**
     * The session timeout configuration for an API session.
     */
    private final long SESSION_TIMEOUT;
    
    /**
     * Create a new BasicTokenUserContextMap and initialize the session timeout value.
     */
    public BasicTokenUserContextMap() {
        
        // Set up the authToken => userContext hashmap
        super();
        
        // Set up the SESSION_TIMEOUT value, with a one hour default.
        long sessionTimeoutValue = 3600000l;
        try {
            sessionTimeoutValue = GuacamoleProperties.getProperty(BasicGuacamoleProperties.API_SESSION_TIMEOUT, 3600000l);
        } catch (GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while reading API_SESSION_TIMEOUT property.", e);
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
        userContextMap.remove(authToken);
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
    
    private boolean sessionHasTimedOut(String authToken) {
        if(!lastAccessTimeMap.containsKey(authToken))
            return true;
        
        long lastAccessTime = lastAccessTimeMap.get(authToken);
        long currentTime = new Date().getTime();
        
        return currentTime - lastAccessTime > SESSION_TIMEOUT;
    }

    @Override
    public UserContext get(String authToken) {
        
        // If the session has timed out, evict the token and force the user to log in again
        if(sessionHasTimedOut(authToken)) {
            evict(authToken);
            return null;
        }
        
        // Update the last access time and return the UserContext
        logAccessTime(authToken);
        return userContextMap.get(authToken);
    }

    @Override
    public void put(String authToken, UserContext userContext) {
        
        // Update the last access time, and create the token/UserContext mapping
        logAccessTime(authToken);
        userContextMap.put(authToken, userContext);
    }
    
}
