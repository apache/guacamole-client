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

package org.apache.guacamole.auth.sqlserver;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicy;

/**
 * A SQLServer-specific implementation of JDBCEnvironment provides database
 * properties specifically for SQLServer.
 */
public class SQLServerEnvironment extends JDBCEnvironment {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(SQLServerEnvironment.class);

    /**
     * The default host to connect to, if SQLSERVER_HOSTNAME is not specified.
     */
    private static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * The default port to connect to, if SQLSERVER_PORT is not specified.
     */
    private static final int DEFAULT_PORT = 1433;

    /**
     * Whether a database user account is required by default for authentication
     * to succeed.
     */
    private static final boolean DEFAULT_USER_REQUIRED = true;

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
     * The value for the sqlserver-driver property that triggers the use of
     * the open source JTDS driver.
     */
    public final static String SQLSERVER_DRIVER_JTDS = "jtds";

    /**
     * The value for the sqlserver-driver property that triggers the use of
     * the DataDirect JDBC driver.
     */
    public final static String SQLSERVER_DRIVER_DATADIRECT = "datadirect";

    /**
     * The value for the sqlserver-driver property that triggers the use of
     * the older Microsoft JDBC driver.
     */
    public final static String SQLSERVER_DRIVER_MS = "microsoft";

    /**
     * The value for the sqlserver-driver property that triggers the use of
     * the Microsoft JDBC driver.  This is the default.
     */
    public final static String SQLSERVER_DRIVER_MS_2005 = "microsoft2005";

    /**
     * Constructs a new SQLServerEnvironment, providing access to SQLServer-specific
     * configuration options.
     * 
     * @throws GuacamoleException 
     *     If an error occurs while setting up the underlying JDBCEnvironment
     *     or while parsing legacy SQLServer configuration options.
     */
    public SQLServerEnvironment() throws GuacamoleException {

        // Init underlying JDBC environment
        super();

        // Read legacy concurrency-related property
        Boolean disallowSimultaneous = getProperty(SQLServerGuacamoleProperties.SQLSERVER_DISALLOW_SIMULTANEOUS_CONNECTIONS);
        Boolean disallowDuplicate    = getProperty(SQLServerGuacamoleProperties.SQLSERVER_DISALLOW_DUPLICATE_CONNECTIONS);

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
                    SQLServerGuacamoleProperties.SQLSERVER_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(),
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_CONNECTIONS.getName(),
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    SQLServerGuacamoleProperties.SQLSERVER_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(), disallowSimultaneous,
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_CONNECTIONS.getName(),           DEFAULT_MAX_CONNECTIONS,
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_GROUP_CONNECTIONS.getName(),     DEFAULT_MAX_GROUP_CONNECTIONS);

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
                    SQLServerGuacamoleProperties.SQLSERVER_DISALLOW_DUPLICATE_CONNECTIONS.getName(),
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

            // Inform of new equivalent
            logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                    SQLServerGuacamoleProperties.SQLSERVER_DISALLOW_DUPLICATE_CONNECTIONS.getName(),         disallowDuplicate,
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),       DEFAULT_MAX_CONNECTIONS_PER_USER,
                    SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER.getName(), DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER);

        }

        // Check driver property is one of the acceptable values.
        String driver = getProperty(SQLServerGuacamoleProperties.SQLSERVER_DRIVER);
        if (!(driver.equals(SQLSERVER_DRIVER_JTDS) ||
                                driver.equals(SQLSERVER_DRIVER_DATADIRECT) ||
                                driver.equals(SQLSERVER_DRIVER_MS) ||
                                driver.equals(SQLSERVER_DRIVER_MS_2005)))
            logger.warn("{} property has been set to an invalid value.  The default Microsoft 2005 driver will be used.",
                        SQLServerGuacamoleProperties.SQLSERVER_DRIVER.getName());

    }

    @Override
    public boolean isUserRequired() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_USER_REQUIRED,
            DEFAULT_USER_REQUIRED
        );
    }

    @Override
    public int getAbsoluteMaxConnections() throws GuacamoleException {
        return getProperty(SQLServerGuacamoleProperties.SQLSERVER_ABSOLUTE_MAX_CONNECTIONS,
            DEFAULT_ABSOLUTE_MAX_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxConnections() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_CONNECTIONS,
            DEFAULT_MAX_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxGroupConnections() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_GROUP_CONNECTIONS,
            DEFAULT_MAX_GROUP_CONNECTIONS
        );
    }

    @Override
    public int getDefaultMaxConnectionsPerUser() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_CONNECTIONS_PER_USER,
            DEFAULT_MAX_CONNECTIONS_PER_USER
        );
    }

    @Override
    public int getDefaultMaxGroupConnectionsPerUser() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER,
            DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER
        );
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return new SQLServerPasswordPolicy(this);
    }

    /**
     * Returns the hostname of the SQLServer server hosting the Guacamole
     * authentication tables. If unspecified, this will be "localhost".
     * 
     * @return
     *     The URL of the SQLServer server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public String getSQLServerHostname() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_HOSTNAME,
            DEFAULT_HOSTNAME
        );
    }
    
    /**
     * Returns the port number of the SQLServer server hosting the Guacamole
     * authentication tables. If unspecified, this will be the default
     * SQLServer port of 5432.
     * 
     * @return
     *     The port number of the SQLServer server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public int getSQLServerPort() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_PORT,
            DEFAULT_PORT
        );
    }
    
    /**
     * Returns the name of the SQLServer database containing the Guacamole
     * authentication tables.
     * 
     * @return
     *     The name of the SQLServer database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getSQLServerDatabase() throws GuacamoleException {
        return getRequiredProperty(SQLServerGuacamoleProperties.SQLSERVER_DATABASE);
    }

    /**
     * Returns the username that should be used when authenticating with the
     * SQLServer database containing the Guacamole authentication tables.
     * 
     * @return
     *     The username for the SQLServer database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getSQLServerUsername() throws GuacamoleException {
        return getRequiredProperty(SQLServerGuacamoleProperties.SQLSERVER_USERNAME);
    }
    
    /**
     * Returns the password that should be used when authenticating with the
     * SQLServer database containing the Guacamole authentication tables.
     * 
     * @return
     *     The password for the SQLServer database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getSQLServerPassword() throws GuacamoleException {
        return getRequiredProperty(SQLServerGuacamoleProperties.SQLSERVER_PASSWORD);
    }

    /**
     * Returns whether or not to use the SourceForge JTDS driver for more
     * generic JTDS connections instead of the Microsoft-provided JDBC driver.
     *
     * @return
     *     True if the JTDS driver should be used; false by default.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getSQLServerDriver() throws GuacamoleException {
        return getProperty(
            SQLServerGuacamoleProperties.SQLSERVER_DRIVER,
            SQLSERVER_DRIVER_MS_2005
        );
    }
    
}
