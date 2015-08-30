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

package org.glyptodon.guacamole.net.auth.simple;

import java.util.Map;
import java.util.UUID;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AbstractAuthenticatedUser;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.token.StandardTokens;
import org.glyptodon.guacamole.token.TokenFilter;

/**
 * Provides means of retrieving a set of named GuacamoleConfigurations for a
 * given Credentials object. This is a simple AuthenticationProvider
 * implementation intended to be easily extended. It is useful for simple
 * authentication situations where access to web-based administration and
 * complex users and permissions are not required.
 *
 * The interface provided by SimpleAuthenticationProvider is similar to that of
 * the AuthenticationProvider interface of older Guacamole releases.
 *
 * @author Michael Jumper
 */
public abstract class SimpleAuthenticationProvider
    implements AuthenticationProvider {

    /**
     * Given an arbitrary credentials object, returns a Map containing all
     * configurations authorized by those credentials. The keys of this Map
     * are Strings which uniquely identify each configuration.
     *
     * @param credentials The credentials to use to retrieve authorized
     *                    configurations.
     * @return A Map of all configurations authorized by the given credentials,
     *         or null if the credentials given are not authorized.
     * @throws GuacamoleException If an error occurs while retrieving
     *                            configurations.
     */
    public abstract Map<String, GuacamoleConfiguration>
            getAuthorizedConfigurations(Credentials credentials)
            throws GuacamoleException;

    /**
     * AuthenticatedUser which contains its own predefined set of authorized
     * configurations.
     *
     * @author Michael Jumper
     */
    private class SimpleAuthenticatedUser extends AbstractAuthenticatedUser {

        /**
         * The credentials provided when this AuthenticatedUser was
         * authenticated.
         */
        private final Credentials credentials;

        /**
         * The GuacamoleConfigurations that this AuthenticatedUser is
         * authorized to use.
         */
        private final Map<String, GuacamoleConfiguration> configs;

        /**
         * Creates a new SimpleAuthenticatedUser associated with the given
         * credentials and having access to the given Map of
         * GuacamoleConfigurations.
         *
         * @param credentials
         *     The credentials provided by the user when they authenticated.
         *
         * @param configs
         *     A Map of all GuacamoleConfigurations for which this user has
         *     access. The keys of this Map are Strings which uniquely identify
         *     each configuration.
         */
        public SimpleAuthenticatedUser(Credentials credentials, Map<String, GuacamoleConfiguration> configs) {

            // Store credentials and configurations
            this.credentials = credentials;
            this.configs = configs;

            // Pull username from credentials if it exists
            String username = credentials.getUsername();
            if (username != null && !username.isEmpty())
                setIdentifier(username);

            // Otherwise generate a random username
            else
                setIdentifier(UUID.randomUUID().toString());

        }

        /**
         * Returns a Map containing all GuacamoleConfigurations that this user
         * is authorized to use. The keys of this Map are Strings which
         * uniquely identify each configuration.
         *
         * @return
         *     A Map of all configurations for which this user is authorized.
         */
        public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations() {
            return configs;
        }

        @Override
        public AuthenticationProvider getAuthenticationProvider() {
            return SimpleAuthenticationProvider.this;
        }

        @Override
        public Credentials getCredentials() {
            return credentials;
        }

    }

    /**
     * Given an arbitrary credentials object, returns a Map containing all
     * configurations authorized by those credentials, filtering those
     * configurations using a TokenFilter and the standard credential tokens
     * (like ${GUAC_USERNAME} and ${GUAC_PASSWORD}). The keys of this Map
     * are Strings which uniquely identify each configuration.
     *
     * @param credentials
     *     The credentials to use to retrieve authorized configurations.
     *
     * @return
     *     A Map of all configurations authorized by the given credentials, or
     *     null if the credentials given are not authorized.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving configurations.
     */
    private Map<String, GuacamoleConfiguration>
            getFilteredAuthorizedConfigurations(Credentials credentials)
            throws GuacamoleException {

        // Get configurations
        Map<String, GuacamoleConfiguration> configs =
                getAuthorizedConfigurations(credentials);

        // Return as unauthorized if not authorized to retrieve configs
        if (configs == null)
            return null;

        // Build credential TokenFilter
        TokenFilter tokenFilter = new TokenFilter();
        StandardTokens.addStandardTokens(tokenFilter, credentials);

        // Filter each configuration
        for (GuacamoleConfiguration config : configs.values())
            tokenFilter.filterValues(config.getParameters());

        return configs;

    }

    /**
     * Given a user who has already been authenticated, returns a Map
     * containing all configurations for which that user is authorized,
     * filtering those configurations using a TokenFilter and the standard
     * credential tokens (like ${GUAC_USERNAME} and ${GUAC_PASSWORD}). The keys
     * of this Map are Strings which uniquely identify each configuration.
     *
     * @param authenticatedUser
     *     The user whose authorized configurations are to be retrieved.
     *
     * @return
     *     A Map of all configurations authorized for use by the given user, or
     *     null if the user is not authorized to use any configurations.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving configurations.
     */
    private Map<String, GuacamoleConfiguration>
            getFilteredAuthorizedConfigurations(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Pull cached configurations, if any
        if (authenticatedUser instanceof SimpleAuthenticatedUser && authenticatedUser.getAuthenticationProvider() == this)
            return ((SimpleAuthenticatedUser) authenticatedUser).getAuthorizedConfigurations();

        // Otherwise, pull using credentials
        return getFilteredAuthorizedConfigurations(authenticatedUser.getCredentials());

    }

    @Override
    public AuthenticatedUser authenticateUser(final Credentials credentials)
            throws GuacamoleException {

        // Get configurations
        Map<String, GuacamoleConfiguration> configs =
                getFilteredAuthorizedConfigurations(credentials);

        // Return as unauthorized if not authorized to retrieve configs
        if (configs == null)
            return null;

        return new SimpleAuthenticatedUser(credentials, configs);

    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Get configurations
        Map<String, GuacamoleConfiguration> configs =
                getFilteredAuthorizedConfigurations(authenticatedUser);

        // Return as unauthorized if not authorized to retrieve configs
        if (configs == null)
            return null;

        // Return user context restricted to authorized configs
        return new SimpleUserContext(this, authenticatedUser.getIdentifier(), configs);

    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleException {

        // Simply return the given user, updating nothing
        return authenticatedUser;

    }

    @Override
    public UserContext updateUserContext(UserContext context,
        AuthenticatedUser authorizedUser) throws GuacamoleException {

        // Simply return the given context, updating nothing
        return context;
        
    }

}
