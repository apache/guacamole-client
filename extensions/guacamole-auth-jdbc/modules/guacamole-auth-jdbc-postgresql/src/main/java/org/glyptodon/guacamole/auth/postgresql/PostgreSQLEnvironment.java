/*
 * Copyright (C) 2015 Glyptodon LLC
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

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.jdbc.JDBCEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PostgreSQL-specific implementation of JDBCEnvironment provides database
 * properties specifically for PostgreSQL.
 *
 * @author Michael Jumper
 */
public class PostgreSQLEnvironment extends JDBCEnvironment {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLEnvironment.class);

    /**
     * The default host to connect to, if POSTGRESQL_HOSTNAME is not specified.
     */
    private static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * The default port to connect to, if POSTGRESQL_PORT is not specified.
     */
    private static final int DEFAULT_PORT = 5432;

    /**
     * The default value for the default maximum number of connections to be
     * allowed per user to any one connection. Note that, as long as the
     * legacy "disallow duplicate" and "disallow simultaneous" properties are
     * still supported, these cannot be constants, as the legacy properties
     * dictate the values that should be used in the absence of the correct
     * properties.
     */
    private int DEFAULT_MAX_CONNECTIONS_PER_USER = 1;

    /**
     * The default value for the default maximum number of connections to be
     * allowed per user to any one connection group. Note that, as long as the
     * legacy "disallow duplicate" and "disallow simultaneous" properties are
     * still supported, these cannot be constants, as the legacy properties
     * dictate the values that should be used in the absence of the correct
     * properties.
     */
    private int DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER = 1;

    /**
     * The default value for the default maximum number of connections to be
     * allowed to any one connection. Note that, as long as the legacy
     * "disallow duplicate" and "disallow simultaneous" properties are still
     * supported, these cannot be constants, as the legacy properties dictate
     * the values that should be used in the absence of the correct properties.
     */
    private int DEFAULT_MAX_CONNECTIONS = 0;

    /**
     * The default value for the default maximum number of connections to be
     * allowed to any one connection group. Note that, as long as the legacy
     * "disallow duplicate" and "disallow simultaneous" properties are still
     * supported, these cannot be constants, as the legacy properties dictate
     * the values that should be used in the absence of the correct properties.
     */
    private int DEFAULT_MAX_GROUP_CONNECTIONS = 0;

    /**
     * Constructs a new PostgreSQLEnvironment, providing access to PostgreSQL-specific
     * configuration options.
     * 
     * @throws GuacamoleException 
     *     If an error occurs while setting up the underlying JDBCEnvironment
     *     or while parsing legacy PostgreSQL configuration options.
     */
    public PostgreSQLEnvironment() throws GuacamoleException {

        // Init underlying JDBC environment
        super();

        // Read legacy concurrency-related property
        Boolean disallowSimultaneous = getProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_SIMULTANEOUS_CONNECTIONS);
        Boolean disallowDuplicate    = getProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_DUPLICATE_CONNECTIONS);

        // Legacy "simultaneous" property dictates only the maximum number of
        // connections per connection
        if (disallowSimultaneous != null) {

            // Translate legacy property
            if (disallowSimultaneous) {
                DEFAULT_MAX_CONNECTIONS       = 1;
                DEFAULT_MAX_GROUP_CONNECTIONS = 0;
            }
            else {
                DEFAULT_MAX_CONNECTIONS       = 0;
                DEFAULT_MAX_GROUP_CONNECTIONS = 0;
            }

            // Warn of deprecation
            logger.warn("The \"{}\" property is deprecated. Use \"{}\" and \"{}\" instead.",
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(),
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_CONNECTIONS.getName(),
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(), disallowSimultaneous,
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_CONNECTIONS.getName(),           DEFAULT_MAX_CONNECTIONS,
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName(),     DEFAULT_MAX_GROUP_CONNECTIONS);

        }

        // Legacy "duplicate" property dictates whether connections and groups
        // may be used concurrently only by different users
        if (disallowDuplicate != null) {

            // Translate legacy property
            if (disallowDuplicate) {
                DEFAULT_MAX_CONNECTIONS_PER_USER       = 1;
                DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER = 1;
            }
            else {
                DEFAULT_MAX_CONNECTIONS_PER_USER       = 0;
                DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER = 0;
            }

            // Warn of deprecation
            logger.warn("The \"{}\" property is deprecated. Use \"{}\" and \"{}\" instead.",
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),         disallowDuplicate,
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),       DEFAULT_MAX_CONNECTIONS_PER_USER,
                    PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER.getName(), DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER);

        }

    }

    @Override
    public int getDefaultMaxConnections() throws GuacamoleException {
        return getProperty(
            PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_CONNECTIONS,
            DEFAULT_MAX_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxGroupConnections() throws GuacamoleException {
        return getProperty(
            PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS,
            DEFAULT_MAX_GROUP_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxConnectionsPerUser() throws GuacamoleException {
        return getProperty(
            PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_CONNECTIONS_PER_USER,
            DEFAULT_MAX_CONNECTIONS_PER_USER
        );
    }

    @Override
    public int getDefaultMaxGroupConnectionsPerUser() throws GuacamoleException {
        return getProperty(
            PostgreSQLGuacamoleProperties.POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER,
            DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER
        );
    }

    /**
     * Returns the hostname of the PostgreSQL server hosting the Guacamole
     * authentication tables. If unspecified, this will be "localhost".
     * 
     * @return
     *     The URL of the PostgreSQL server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public String getPostgreSQLHostname() throws GuacamoleException {
        return getProperty(
            PostgreSQLGuacamoleProperties.POSTGRESQL_HOSTNAME,
            DEFAULT_HOSTNAME
        );
    }
    
    /**
     * Returns the port number of the PostgreSQL server hosting the Guacamole
     * authentication tables. If unspecified, this will be the default
     * PostgreSQL port of 5432.
     * 
     * @return
     *     The port number of the PostgreSQL server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public int getPostgreSQLPort() throws GuacamoleException {
        return getProperty(
            PostgreSQLGuacamoleProperties.POSTGRESQL_PORT,
            DEFAULT_PORT
        );
    }
    
    /**
     * Returns the name of the PostgreSQL database containing the Guacamole
     * authentication tables.
     * 
     * @return
     *     The name of the PostgreSQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getPostgreSQLDatabase() throws GuacamoleException {
        return getRequiredProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_DATABASE);
    }
    
    /**
     * Returns the username that should be used when authenticating with the
     * PostgreSQL database containing the Guacamole authentication tables.
     * 
     * @return
     *     The username for the PostgreSQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getPostgreSQLUsername() throws GuacamoleException {
        return getRequiredProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_USERNAME);
    }
    
    /**
     * Returns the password that should be used when authenticating with the
     * PostgreSQL database containing the Guacamole authentication tables.
     * 
     * @return
     *     The password for the PostgreSQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getPostgreSQLPassword() throws GuacamoleException {
        return getRequiredProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_PASSWORD);
    }
    
}
