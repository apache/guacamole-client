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

package org.apache.guacamole.auth.mongodb;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.morphia.JDBCEnvironment;
import org.apache.guacamole.morphia.security.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MongoDB-specific implementation of JDBCEnvironment provides database
 * properties specifically for MongoDB.
 */
public class MongoDBEnvironment extends JDBCEnvironment {

    private static final Logger logger = LoggerFactory
            .getLogger(MongoDBEnvironment.class);

    /**
     * The default host to connect to, if MONGODB_HOSTNAME is not specified.
     */
    private static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * The default port to connect to, if MONGODB_PORT is not specified.
     */
    private static final int DEFAULT_PORT = 27017;

    /**
     * Whether a database user account is required by default for authentication
     * to succeed.
     */
    private static final boolean DEFAULT_USER_REQUIRED = false;

    /**
     * The default value for the maximum number of connections to be allowed to
     * the Guacamole server overall.
     */
    private final int DEFAULT_ABSOLUTE_MAX_CONNECTIONS = 0;

    /**
     * The default value for the default maximum number of connections to be
     * allowed per user to any one connection. Note that, as long as the legacy
     * "disallow duplicate" and "disallow simultaneous" properties are still
     * supported, these cannot be constants, as the legacy properties dictate
     * the values that should be used in the absence of the correct properties.
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
     * allowed to any one connection. Note that, as long as the legacy "disallow
     * duplicate" and "disallow simultaneous" properties are still supported,
     * these cannot be constants, as the legacy properties dictate the values
     * that should be used in the absence of the correct properties.
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
     * Constructs a new MongoDBEnvironment, providing access to MongoDB-specific
     * configuration options.
     * 
     * @throws GuacamoleException
     *             If an error occurs while setting up the underlying
     *             JDBCEnvironment or while parsing legacy MongoDB configuration
     *             options.
     */
    public MongoDBEnvironment() throws GuacamoleException {

        // Init underlying JDBC environment
        super();

    }

    @Override
    public boolean isUserRequired() throws GuacamoleException {
        return getProperty(MongoDBGuacamoleProperties.MONGODB_USER_REQUIRED,
                DEFAULT_USER_REQUIRED);
    }

    @Override
    public int getAbsoluteMaxConnections() throws GuacamoleException {
        return getProperty(
                MongoDBGuacamoleProperties.MONGODB_ABSOLUTE_MAX_CONNECTIONS,
                DEFAULT_ABSOLUTE_MAX_CONNECTIONS);
    }

    @Override
    public int getDefaultMaxConnections() throws GuacamoleException {
        return getProperty(
                MongoDBGuacamoleProperties.MONGODB_DEFAULT_MAX_CONNECTIONS,
                DEFAULT_MAX_CONNECTIONS);
    }

    @Override
    public int getDefaultMaxGroupConnections() throws GuacamoleException {
        return getProperty(
                MongoDBGuacamoleProperties.MONGODB_DEFAULT_MAX_GROUP_CONNECTIONS,
                DEFAULT_MAX_GROUP_CONNECTIONS);
    }

    @Override
    public int getDefaultMaxConnectionsPerUser() throws GuacamoleException {
        return getProperty(
                MongoDBGuacamoleProperties.MONGODB_DEFAULT_MAX_CONNECTIONS_PER_USER,
                DEFAULT_MAX_CONNECTIONS_PER_USER);
    }

    @Override
    public int getDefaultMaxGroupConnectionsPerUser()
            throws GuacamoleException {
        return getProperty(
                MongoDBGuacamoleProperties.MONGODB_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER,
                DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return new MongoDBPasswordPolicy(this);
    }

    /**
     * Returns the hostname of the MongoDB server hosting the Guacamole
     * authentication tables. If unspecified, this will be "localhost".
     * 
     * @return The URL of the MongoDB server.
     *
     * @throws GuacamoleException
     *             If an error occurs while retrieving the property value.
     */
    public String getMongoDBHostname() {
        try {
            return getProperty(MongoDBGuacamoleProperties.MONGODB_HOSTNAME,
                    DEFAULT_HOSTNAME);
        } catch (GuacamoleException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Returns the port number of the MongoDB server hosting the Guacamole
     * authentication tables. If unspecified, this will be the default MongoDB
     * port of 3306.
     * 
     * @return The port number of the MongoDB server.
     *
     * @throws GuacamoleException
     *             If an error occurs while retrieving the property value.
     */
    public int getMongoDBPort() {
        try {
            return getProperty(MongoDBGuacamoleProperties.MONGODB_PORT,
                    DEFAULT_PORT);
        } catch (GuacamoleException e) {
            logger.error(e.getMessage());
        }
        return 0;
    }

    /**
     * Returns the name of the MongoDB database containing the Guacamole
     * authentication tables.
     * 
     * @return The name of the MongoDB database.
     *
     * @throws GuacamoleException
     *             If an error occurs while retrieving the property value, or if
     *             the value was not set, as this property is required.
     */
    public String getMongoDBDatabase() {
        try {
            return getRequiredProperty(
                    MongoDBGuacamoleProperties.MONGODB_DATABASE);
        } catch (GuacamoleException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Returns the username that should be used when authenticating with the
     * MongoDB database containing the Guacamole authentication tables.
     * 
     * @return The username for the MongoDB database.
     *
     * @throws GuacamoleException
     *             If an error occurs while retrieving the property value, or if
     *             the value was not set, as this property is required.
     */
    public String getMongoDBUsername() {
        try {
            return getRequiredProperty(
                    MongoDBGuacamoleProperties.MONGODB_USERNAME);
        } catch (GuacamoleException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Returns the password that should be used when authenticating with the
     * MongoDB database containing the Guacamole authentication tables.
     * 
     * @return The password for the MongoDB database.
     *
     * @throws GuacamoleException
     *             If an error occurs while retrieving the property value, or if
     *             the value was not set, as this property is required.
     */
    public String getMongoDBPassword() {
        try {
            return getRequiredProperty(
                    MongoDBGuacamoleProperties.MONGODB_PASSWORD);
        } catch (GuacamoleException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

}
