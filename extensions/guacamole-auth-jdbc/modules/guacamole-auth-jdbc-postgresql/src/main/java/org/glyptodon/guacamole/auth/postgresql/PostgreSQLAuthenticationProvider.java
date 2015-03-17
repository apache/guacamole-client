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

package org.glyptodon.guacamole.auth.postgresql;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.auth.jdbc.JDBCAuthenticationProviderModule;
import org.glyptodon.guacamole.auth.jdbc.tunnel.BalancedGuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.tunnel.MultiseatGuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.tunnel.SingleSeatGuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.tunnel.UnrestrictedGuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.user.UserContextService;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.environment.LocalEnvironment;

/**
 * Provides a PostgreSQL-based implementation of the AuthenticationProvider
 * functionality.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class PostgreSQLAuthenticationProvider implements AuthenticationProvider {

    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;

    /**
     * Returns the appropriate socket service class given the Guacamole
     * environment. The class is chosen based on configuration options that
     * dictate concurrent usage policy.
     *
     * @param environment
     *     The environment of the Guacamole server.
     *
     * @return
     *     The socket service class that matches the concurrent usage policy
     *     options set in the Guacamole environment.
     *
     * @throws GuacamoleException
     *     If an error occurs while reading the configuration options.
     */
    private Class<? extends GuacamoleTunnelService>
        getSocketServiceClass(Environment environment)
                throws GuacamoleException {

        // Read concurrency-related properties
        boolean disallowSimultaneous = environment.getProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_SIMULTANEOUS_CONNECTIONS, false);
        boolean disallowDuplicate    = environment.getProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_DUPLICATE_CONNECTIONS, true);

        if (disallowSimultaneous) {

            // Connections may not be used concurrently
            if (disallowDuplicate)
                return SingleSeatGuacamoleTunnelService.class;

            // Connections are reserved for a single user when in use
            else
                return BalancedGuacamoleTunnelService.class;

        }

        else {

            // Connections may be used concurrently, but only once per user
            if (disallowDuplicate)
                return MultiseatGuacamoleTunnelService.class;

            // Connection use is not restricted
            else
                return UnrestrictedGuacamoleTunnelService.class;

        }
         
    }
    
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
        Environment environment = new LocalEnvironment();

        // Set up Guice injector.
        injector = Guice.createInjector(

            // Configure PostgreSQL-specific authentication
            new PostgreSQLAuthenticationProviderModule(environment),

            // Configure JDBC authentication core
            new JDBCAuthenticationProviderModule(environment, getSocketServiceClass(environment))

        );

    }

    @Override
    public UserContext getUserContext(Credentials credentials)
            throws GuacamoleException {

        // Create UserContext based on credentials, if valid
        UserContextService userContextService = injector.getInstance(UserContextService.class);
        return userContextService.getUserContext(credentials);

    }

    @Override
    public UserContext updateUserContext(UserContext context,
        Credentials credentials) throws GuacamoleException {

        // No need to update the context
        return context;

    }

}
