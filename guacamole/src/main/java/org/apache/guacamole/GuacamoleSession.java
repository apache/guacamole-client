/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.tunnel.UserTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains Guacamole-specific user information which is tied to the current
 * session, such as the UserContext and current clipboard state.
 */
public class GuacamoleSession {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(GuacamoleSession.class);

    /**
     * The user associated with this session.
     */
    private AuthenticatedUser authenticatedUser;
    
    /**
     * All UserContexts associated with this session. Each
     * AuthenticationProvider may provide its own UserContext.
     */
    private List<UserContext> userContexts;

    /**
     * All currently-active tunnels, indexed by tunnel UUID.
     */
    private final Map<String, UserTunnel> tunnels =
            new ConcurrentHashMap<String, UserTunnel>();

    /**
     * The last time this session was accessed.
     */
    private long lastAccessedTime;
    
    /**
     * Creates a new Guacamole session associated with the given
     * AuthenticatedUser and UserContexts.
     *
     * @param environment
     *     The environment of the Guacamole server associated with this new
     *     session.
     *
     * @param authenticatedUser
     *     The authenticated user to associate this session with.
     *
     * @param userContexts
     *     The List of UserContexts to associate with this session.
     *
     * @throws GuacamoleException
     *     If an error prevents the session from being created.
     */
    public GuacamoleSession(Environment environment,
            AuthenticatedUser authenticatedUser,
            List<UserContext> userContexts)
            throws GuacamoleException {
        this.lastAccessedTime = System.currentTimeMillis();
        this.authenticatedUser = authenticatedUser;
        this.userContexts = userContexts;
    }

    /**
     * Returns the authenticated user associated with this session.
     *
     * @return
     *     The authenticated user associated with this session.
     */
    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    /**
     * Replaces the authenticated user associated with this session with the
     * given authenticated user.
     *
     * @param authenticatedUser
     *     The authenticated user to associated with this session.
     */
    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Returns a list of all UserContexts associated with this session. Each
     * AuthenticationProvider currently loaded by Guacamole may provide its own
     * UserContext for any successfully-authenticated user.
     *
     * @return
     *     An unmodifiable list of all UserContexts associated with this
     *     session.
     */
    public List<UserContext> getUserContexts() {
        return Collections.unmodifiableList(userContexts);
    }

    /**
     * Returns the UserContext associated with this session that originated
     * from the AuthenticationProvider with the given identifier. If no such
     * UserContext exists, an exception is thrown.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider that created the
     *     UserContext being retrieved.
     *
     * @return
     *     The UserContext that was created by the AuthenticationProvider
     *     having the given identifier.
     *
     * @throws GuacamoleException
     *     If no such UserContext exists.
     */
    public UserContext getUserContext(String authProviderIdentifier)
            throws GuacamoleException {

        // Locate and return the UserContext associated with the
        // AuthenticationProvider having the given identifier, if any
        for (UserContext userContext : getUserContexts()) {

            // Get AuthenticationProvider associated with current UserContext
            AuthenticationProvider authProvider = userContext.getAuthenticationProvider();

            // If AuthenticationProvider identifier matches, done
            if (authProvider.getIdentifier().equals(authProviderIdentifier))
                return userContext;

        }

        throw new GuacamoleResourceNotFoundException("Session not associated "
                + "with authentication provider \"" + authProviderIdentifier + "\".");


    }

    /**
     * Replaces all UserContexts associated with this session with the given
     * List of UserContexts.
     *
     * @param userContexts
     *     The List of UserContexts to associate with this session.
     */
    public void setUserContexts(List<UserContext> userContexts) {
        this.userContexts = userContexts;
    }
    
    /**
     * Returns whether this session has any associated active tunnels.
     *
     * @return true if this session has any associated active tunnels,
     *         false otherwise.
     */
    public boolean hasTunnels() {
        return !tunnels.isEmpty();
    }

    /**
     * Returns a map of all active tunnels associated with this session, where
     * each key is the String representation of the tunnel's UUID. Changes to
     * this map immediately affect the set of tunnels associated with this
     * session. A tunnel need not be present here to be used by the user
     * associated with this session, but tunnels not in this set will not
     * be taken into account when determining whether a session is in use.
     *
     * @return A map of all active tunnels associated with this session.
     */
    public Map<String, UserTunnel> getTunnels() {
        return tunnels;
    }

    /**
     * Associates the given tunnel with this session, such that it is taken
     * into account when determining session activity.
     *
     * @param tunnel The tunnel to associate with this session.
     */
    public void addTunnel(UserTunnel tunnel) {
        tunnels.put(tunnel.getUUID().toString(), tunnel);
    }

    /**
     * Disassociates the tunnel having the given UUID from this session.
     *
     * @param uuid The UUID of the tunnel to disassociate from this session.
     * @return true if the tunnel existed and was removed, false otherwise.
     */
    public boolean removeTunnel(String uuid) {
        return tunnels.remove(uuid) != null;
    }

    /**
     * Updates this session, marking it as accessed.
     */
    public void access() {
        lastAccessedTime = System.currentTimeMillis();
    }

    /**
     * Returns the time this session was last accessed, as the number of
     * milliseconds since midnight January 1, 1970 GMT. Session access must
     * be explicitly marked through calls to the access() function.
     *
     * @return The time this session was last accessed.
     */
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    /**
     * Closes all associated tunnels and prevents any further use of this
     * session.
     */
    public void invalidate() {

        // Close all associated tunnels, if possible
        for (GuacamoleTunnel tunnel : tunnels.values()) {
            try {
                tunnel.close();
            }
            catch (GuacamoleException e) {
                logger.debug("Unable to close tunnel.", e);
            }
        }

        // Invalidate all user contextx
        for (UserContext userContext : userContexts)
            userContext.invalidate();

        // Invalidate the authenticated user object
        authenticatedUser.invalidate();

    }
    
}
