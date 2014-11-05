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

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
@Singleton
public class BasicTokenSessionMap implements TokenSessionMap {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(BasicTokenSessionMap.class);

    /**
     * Executor service which runs the period session eviction task.
     */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    
    /**
     * Keeps track of the authToken to GuacamoleSession mapping.
     */
    private final Map<String, GuacamoleSession> sessionMap =
            Collections.synchronizedMap(new LinkedHashMap<String, GuacamoleSession>(16, 0.75f, true));

    /**
     * Create a new BasicTokenGuacamoleSessionMap and initialize the session timeout value.
     */
    public BasicTokenSessionMap() {
        
        int sessionTimeoutValue;

        // Read session timeout from guacamole.properties
        try {
            sessionTimeoutValue = GuacamoleProperties.getProperty(BasicGuacamoleProperties.API_SESSION_TIMEOUT, 60);
        }
        catch (GuacamoleException e) {
            logger.error("Unable to read guacamole.properties: {}", e.getMessage());
            logger.debug("Error while reading session timeout value.", e);
            sessionTimeoutValue = 60;
        }
        
        // Check for expired sessions every minute
        logger.info("Sessions will expire after {} minutes of inactivity.", sessionTimeoutValue);
        executor.scheduleAtFixedRate(new SessionEvictionTask(sessionTimeoutValue * 60000l), 1, 1, TimeUnit.MINUTES);
        
    }

    /**
     * Task which iterates through all active sessions, evicting those sessions
     * which are beyond the session timeout. This is a fairly easy thing to do,
     * since the session storage structure guarantees that sessions are always
     * in descending order of age.
     */
    private class SessionEvictionTask implements Runnable {

        /**
         * The maximum allowed age of any session, in milliseconds.
         */
        private final long sessionTimeout;

        /**
         * Creates a new task which automatically evicts sessions which are
         * older than the specified timeout.
         * 
         * @param sessionTimeout The maximum age of any session, in
         *                       milliseconds.
         */
        public SessionEvictionTask(long sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }
        
        @Override
        public void run() {

            // Get current time
            long now = System.currentTimeMillis();

            logger.debug("Checking for expired sessions...");
            
            // For each session, remove sesions which have expired
            Iterator<Map.Entry<String, GuacamoleSession>> entries = sessionMap.entrySet().iterator();
            while (entries.hasNext()) {

                Map.Entry<String, GuacamoleSession> entry = entries.next();
                GuacamoleSession session = entry.getValue();

                // Get elapsed time since last access
                long age = now - session.getLastAccessedTime();

                // If session is too old, evict it and check the next one
                if (age >= sessionTimeout) {
                    logger.debug("Session \"{}\" has timed out.", entry.getKey());
                    entries.remove();
                    session.invalidate();
                }

                // Otherwise, no other sessions can possibly be old enough
                else
                    break;
                
            }

            logger.debug("Session check complete.");
            
        }

    }

    @Override
    public GuacamoleSession get(String authToken) {
        
        // Update the last access time and return the GuacamoleSession
        GuacamoleSession session = sessionMap.get(authToken);
        if (session != null)
            session.access();

        return session;

    }

    @Override
    public void put(String authToken, GuacamoleSession session) {
        sessionMap.put(authToken, session);
    }

    @Override
    public GuacamoleSession remove(String authToken) {
        return sessionMap.remove(authToken);
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }

}
