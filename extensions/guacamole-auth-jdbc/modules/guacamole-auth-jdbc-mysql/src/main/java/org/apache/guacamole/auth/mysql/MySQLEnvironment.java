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

package org.apache.guacamole.auth.mysql;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicy;

/**
 * A MySQL-specific implementation of JDBCEnvironment provides database
 * properties specifically for MySQL.
 */
public class MySQLEnvironment extends JDBCEnvironment {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(MySQLEnvironment.class);
    
    /**
     * The default host to connect to, if MYSQL_HOSTNAME is not specified.
     */
    private static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * The default port to connect to, if MYSQL_PORT is not specified.
     */
    private static final int DEFAULT_PORT = 3306;

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
     * Constructs a new MySQLEnvironment, providing access to MySQL-specific
     * configuration options.
     * 
     * @throws GuacamoleException 
     *     If an error occurs while setting up the underlying JDBCEnvironment
     *     or while parsing legacy MySQL configuration options.
     */
    public MySQLEnvironment() throws GuacamoleException {

        // Init underlying JDBC environment
        super();

        // Read legacy concurrency-related property
        Boolean disallowSimultaneous = getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS);
        Boolean disallowDuplicate    = getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS);

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
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(), disallowSimultaneous,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS.getName(),           DEFAULT_MAX_CONNECTIONS,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName(),     DEFAULT_MAX_GROUP_CONNECTIONS);

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
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),         disallowDuplicate,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),       DEFAULT_MAX_CONNECTIONS_PER_USER,
                    MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER.getName(), DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER);

        }

    }

    @Override
    public boolean isUserRequired() throws GuacamoleException {
        return getProperty(
            MySQLGuacamoleProperties.MYSQL_USER_REQUIRED,
            DEFAULT_USER_REQUIRED
        );
    }

    @Override
    public int getAbsoluteMaxConnections() throws GuacamoleException {
        return getProperty(MySQLGuacamoleProperties.MYSQL_ABSOLUTE_MAX_CONNECTIONS,
            DEFAULT_ABSOLUTE_MAX_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxConnections() throws GuacamoleException {
        return getProperty(
            MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS,
            DEFAULT_MAX_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxGroupConnections() throws GuacamoleException {
        return getProperty(
            MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS,
            DEFAULT_MAX_GROUP_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxConnectionsPerUser() throws GuacamoleException {
        return getProperty(
            MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER,
            DEFAULT_MAX_CONNECTIONS_PER_USER
        );
    }

    @Override
    public int getDefaultMaxGroupConnectionsPerUser() throws GuacamoleException {
        return getProperty(
            MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER,
            DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER
        );
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return new MySQLPasswordPolicy(this);
    }

    /**
     * Returns the hostname of the MySQL server hosting the Guacamole
     * authentication tables. If unspecified, this will be "localhost".
     * 
     * @return
     *     The URL of the MySQL server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public String getMySQLHostname() throws GuacamoleException {
        return getProperty(
            MySQLGuacamoleProperties.MYSQL_HOSTNAME,
            DEFAULT_HOSTNAME
        );
    }
    
    /**
     * Returns the port number of the MySQL server hosting the Guacamole
     * authentication tables. If unspecified, this will be the default MySQL
     * port of 3306.
     * 
     * @return
     *     The port number of the MySQL server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public int getMySQLPort() throws GuacamoleException {
        return getProperty(MySQLGuacamoleProperties.MYSQL_PORT, DEFAULT_PORT);
    }
    
    /**
     * Returns the name of the MySQL database containing the Guacamole 
     * authentication tables.
     * 
     * @return
     *     The name of the MySQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getMySQLDatabase() throws GuacamoleException {
        return getRequiredProperty(MySQLGuacamoleProperties.MYSQL_DATABASE);
    }
    
    /**
     * Returns the username that should be used when authenticating with the
     * MySQL database containing the Guacamole authentication tables.
     * 
     * @return
     *     The username for the MySQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getMySQLUsername() throws GuacamoleException {
        return getRequiredProperty(MySQLGuacamoleProperties.MYSQL_USERNAME);
    }
    
    /**
     * Returns the password that should be used when authenticating with the
     * MySQL database containing the Guacamole authentication tables.
     * 
     * @return
     *     The password for the MySQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getMySQLPassword() throws GuacamoleException {
        return getRequiredProperty(MySQLGuacamoleProperties.MYSQL_PASSWORD);
    }
    
}
