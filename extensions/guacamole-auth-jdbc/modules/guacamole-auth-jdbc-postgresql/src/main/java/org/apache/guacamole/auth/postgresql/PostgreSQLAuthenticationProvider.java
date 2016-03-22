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

package org.apache.guacamole.auth.postgresql;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.auth.jdbc.JDBCAuthenticationProviderModule;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.user.AuthenticationProviderService;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a PostgreSQL-based implementation of the AuthenticationProvider
 * functionality.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class PostgreSQLAuthenticationProvider implements AuthenticationProvider {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLAuthenticationProvider.class);

    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;

    /**
     * Creates a new PostgreSQLAuthenticationProvider that reads and writes
     * authentication data to a PostgreSQL database defined by properties in
     * guacamole.properties.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public PostgreSQLAuthenticationProvider() throws GuacamoleException {

        // Get local environment
        PostgreSQLEnvironment environment = new PostgreSQLEnvironment();

        // Set up Guice injector.
        injector = Guice.createInjector(

            // Configure PostgreSQL-specific authentication
            new PostgreSQLAuthenticationProviderModule(environment),

            // Configure JDBC authentication core
            new JDBCAuthenticationProviderModule(this, environment)

        );

    }

    @Override
    public String getIdentifier() {
        return "postgresql";
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Create AuthenticatedUser based on credentials, if valid
        AuthenticationProviderService authProviderService = injector.getInstance(AuthenticationProviderService.class);
        return authProviderService.authenticateUser(this, credentials);

    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleException {

        // No need to update authenticated users
        return authenticatedUser;

    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Create UserContext based on credentials, if valid
        AuthenticationProviderService authProviderService = injector.getInstance(AuthenticationProviderService.class);
        return authProviderService.getUserContext(authenticatedUser);

    }

    @Override
    public UserContext updateUserContext(UserContext context,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // No need to update the context
        return context;

    }

}
