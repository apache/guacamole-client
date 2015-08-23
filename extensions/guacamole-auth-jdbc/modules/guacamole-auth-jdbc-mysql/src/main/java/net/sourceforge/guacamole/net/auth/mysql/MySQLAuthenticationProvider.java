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

package net.sourceforge.guacamole.net.auth.mysql;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.auth.jdbc.JDBCAuthenticationProviderModule;
import org.glyptodon.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.tunnel.ConfigurableGuacamoleTunnelService;
import org.glyptodon.guacamole.auth.jdbc.user.UserContextService;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.environment.LocalEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a MySQL based implementation of the AuthenticationProvider
 * functionality.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class MySQLAuthenticationProvider implements AuthenticationProvider {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(MySQLAuthenticationProvider.class);

    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;

    /**
     * Returns the appropriate tunnel service given the Guacamole environment.
     * The service is configured based on configuration options that dictate
     * the default concurrent usage policy.
     *
     * @param environment
     *     The environment of the Guacamole server.
     *
     * @return
     *     A tunnel service implementation configured according to the
     *     concurrent usage policy options set in the Guacamole environment.
     *
     * @throws GuacamoleException
     *     If an error occurs while reading the configuration options.
     */
    private GuacamoleTunnelService getTunnelService(Environment environment)
                throws GuacamoleException {

        // Tunnel service default configuration
        int connectionDefaultMaxConnections;
        int connectionDefaultMaxConnectionsPerUser;
        int connectionGroupDefaultMaxConnections;
        int connectionGroupDefaultMaxConnectionsPerUser;

        // Read legacy concurrency-related properties
        Boolean disallowSimultaneous = environment.getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS);
        Boolean disallowDuplicate    = environment.getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS);

        // Legacy "simultaneous" property dictates only the maximum number of
        // connections per connection
        if (disallowSimultaneous != null) {

            // Translate legacy property
            if (disallowSimultaneous) {
                connectionDefaultMaxConnections = 1;
                connectionGroupDefaultMaxConnections = 0;
            }
            else {
                connectionDefaultMaxConnections = 0;
                connectionGroupDefaultMaxConnections = 0;
            }

            // Warn of deprecation
            logger.warn("The \"{}\" property is deprecated. Use \"{}\" and \"{}\" instead.",
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(), disallowSimultaneous,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS.getName(),           connectionDefaultMaxConnections,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName(),     connectionGroupDefaultMaxConnections);

        }

        // If legacy property is not specified, use new property
        else {
            connectionDefaultMaxConnections      = environment.getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS, 0);
            connectionGroupDefaultMaxConnections = environment.getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS, 0);
        }

        // Legacy "duplicate" property dictates whether connections and groups
        // may be used concurrently only by different users
        if (disallowDuplicate != null) {

            // Translate legacy property
            if (disallowDuplicate) {
                connectionDefaultMaxConnectionsPerUser      = 1;
                connectionGroupDefaultMaxConnectionsPerUser = 1;
            }
            else {
                connectionDefaultMaxConnectionsPerUser      = 0;
                connectionGroupDefaultMaxConnectionsPerUser = 0;
            }

            // Warn of deprecation
            logger.warn("The \"{}\" property is deprecated. Use \"{}\" and \"{}\" instead.",
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),         disallowDuplicate,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),       connectionDefaultMaxConnectionsPerUser,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER.getName(), connectionGroupDefaultMaxConnectionsPerUser);

        }

        // If legacy property is not specified, use new property
        else {
            connectionDefaultMaxConnectionsPerUser      = environment.getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER, 1);
            connectionGroupDefaultMaxConnectionsPerUser = environment.getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER, 1);
        }

        // Return service configured for specified default limits
        return new ConfigurableGuacamoleTunnelService(
            connectionDefaultMaxConnections,
            connectionDefaultMaxConnectionsPerUser,
            connectionGroupDefaultMaxConnections,
            connectionGroupDefaultMaxConnectionsPerUser
        );

    }
    
    /**
     * Creates a new MySQLAuthenticationProvider that reads and writes
     * authentication data to a MySQL database defined by properties in
     * guacamole.properties.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public MySQLAuthenticationProvider() throws GuacamoleException {

        // Get local environment
        Environment environment = new LocalEnvironment();

        // Set up Guice injector.
        injector = Guice.createInjector(

            // Configure MySQL-specific authentication
            new MySQLAuthenticationProviderModule(environment),

            // Configure JDBC authentication core
            new JDBCAuthenticationProviderModule(environment, getTunnelService(environment))

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
