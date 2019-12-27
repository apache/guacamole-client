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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicy;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

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
     * The earliest version of MariaDB that supported recursive CTEs.
     */
    private static final MySQLVersion MARIADB_SUPPORTS_CTE = new MySQLVersion(10, 2, 2, true);

    /**
     * The earliest version of MySQL that supported recursive CTEs.
     */
    private static final MySQLVersion MYSQL_SUPPORTS_CTE = new MySQLVersion(8, 0, 1, false);

    /**
     * The default MySQL-compatible driver to use, if not specified.
     */
    private static final MySQLDriver DEFAULT_DRIVER = MySQLDriver.MYSQL;
    
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
     * allowed per user to any one connection.
     */
    private final int DEFAULT_MAX_CONNECTIONS_PER_USER = 1;

    /**
     * The default value for the default maximum number of connections to be
     * allowed per user to any one connection group.
     */
    private final int DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER = 1;

    /**
     * The default value for the default maximum number of connections to be
     * allowed to any one connection.
     */
    private final int DEFAULT_MAX_CONNECTIONS = 0;

    /**
     * The default value for the default maximum number of connections to be
     * allowed to any one connection group.
     */
    private final int DEFAULT_MAX_GROUP_CONNECTIONS = 0;

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
     * Returns the MySQL driver that will be used to talk to the MySQL-compatible
     * database server hosting the Guacamole Client database.  If unspecified
     * a default value of MySQL will be used.
     * 
     * @return
     *     The MySQL driver that will be used to communicate with the MySQL-
     *     compatible server.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    public MySQLDriver getMySQLDriver() throws GuacamoleException {
        return getProperty(
            MySQLGuacamoleProperties.MYSQL_DRIVER,
            DEFAULT_DRIVER
        );
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

    @Override
    public boolean isRecursiveQuerySupported(SqlSession session) {

        // Retrieve database version string from JDBC connection
        String versionString;
        try {
            Connection connection = session.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            versionString = metaData.getDatabaseProductVersion();
        }
        catch (SQLException e) {
            throw new PersistenceException("Cannot determine whether "
                    + "MySQL / MariaDB supports recursive queries.", e);
        }

        try {

            // Parse MySQL / MariaDB version from version string
            MySQLVersion version = new MySQLVersion(versionString);
            logger.debug("Database recognized as {}.", version);

            // Recursive queries are supported for MariaDB 10.2.2+ and
            // MySQL 8.0.1+
            return version.isAtLeast(MARIADB_SUPPORTS_CTE)
                || version.isAtLeast(MYSQL_SUPPORTS_CTE);

        }
        catch (IllegalArgumentException e) {
            logger.debug("Unrecognized MySQL / MariaDB version string: "
                    + "\"{}\". Assuming database engine does not support "
                    + "recursive queries.", session);
            return false;
        }

    }

}
