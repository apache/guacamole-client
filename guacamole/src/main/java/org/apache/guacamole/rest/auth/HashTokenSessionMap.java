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

package org.apache.guacamole.rest.auth;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HashMap-based implementation of the TokenSessionMap with support for
 * session timeouts.
 *
 * @author James Muehlner
 */
public class HashTokenSessionMap implements TokenSessionMap {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HashTokenSessionMap.class);

    /**
     * Executor service which runs the period session eviction task.
     */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    
    /**
     * Keeps track of the authToken to GuacamoleSession mapping.
     */
    private final ConcurrentMap<String, GuacamoleSession> sessionMap =
            new ConcurrentHashMap<String, GuacamoleSession>();

    /**
     * The session timeout for the Guacamole REST API, in minutes.
     */
    private final IntegerGuacamoleProperty API_SESSION_TIMEOUT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "api-session-timeout"; }

    };

    /**
     * Create a new HashTokenSessionMap configured using the given environment.
     *
     * @param environment
     *     The environment to use when configuring the token session map.
     */
    public HashTokenSessionMap(Environment environment) {
        
        int sessionTimeoutValue;

        // Read session timeout from guacamole.properties
        try {
            sessionTimeoutValue = environment.getProperty(API_SESSION_TIMEOUT, 60);
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
     * which are beyond the session timeout.
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

            // Get start time of session check time
            long sessionCheckStart = System.currentTimeMillis();

            logger.debug("Checking for expired sessions...");

            // For each session, remove sesions which have expired
            Iterator<Map.Entry<String, GuacamoleSession>> entries = sessionMap.entrySet().iterator();
            while (entries.hasNext()) {

                Map.Entry<String, GuacamoleSession> entry = entries.next();
                GuacamoleSession session = entry.getValue();

                // Do not expire sessions which are active
                if (session.hasTunnels())
                    continue;

                // Get elapsed time since last access
                long age = sessionCheckStart - session.getLastAccessedTime();

                // If session is too old, evict it and check the next one
                if (age >= sessionTimeout) {
                    logger.debug("Session \"{}\" has timed out.", entry.getKey());
                    entries.remove();
                    session.invalidate();
                }

            }

            // Log completion and duration
            logger.debug("Session check completed in {} ms.",
                    System.currentTimeMillis() - sessionCheckStart);
            
        }

    }

    @Override
    public GuacamoleSession get(String authToken) {
        
        // There are no null auth tokens
        if (authToken == null)
            return null;

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

        // There are no null auth tokens
        if (authToken == null)
            return null;

        // Attempt to retrieve only if non-null
        return sessionMap.remove(authToken);

    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }

}
