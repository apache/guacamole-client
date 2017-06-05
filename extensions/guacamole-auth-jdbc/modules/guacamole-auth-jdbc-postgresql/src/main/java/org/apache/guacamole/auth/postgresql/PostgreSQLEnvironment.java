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

package org.apache.guacamole.auth.postgresql;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicy;

/**
 * A PostgreSQL-specific implementation of JDBCEnvironment provides database
 * properties specifically for PostgreSQL.
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
     * Whether a database user account is required by default for authentication
     * to succeed.
     */
    private static final boolean DEFAULT_USER_REQUIRED = false;

    /**
     * The default value for the maximum number of connections to be
     * allowed to the Guacamole server overall.
     */
    private final int DEFAULT_ABSOLUTE_MAX_CONNECTIONS = 0;

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
    public boolean isUserRequired() throws GuacamoleException {
        return getProperty(
            PostgreSQLGuacamoleProperties.POSTGRESQL_USER_REQUIRED,
            DEFAULT_USER_REQUIRED
        );
    }

    @Override
    public int getAbsoluteMaxConnections() throws GuacamoleException {
        return getProperty(PostgreSQLGuacamoleProperties.POSTGRESQL_ABSOLUTE_MAX_CONNECTIONS,
            DEFAULT_ABSOLUTE_MAX_CONNECTIONS
        );
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

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return new PostgreSQLPasswordPolicy(this);
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
