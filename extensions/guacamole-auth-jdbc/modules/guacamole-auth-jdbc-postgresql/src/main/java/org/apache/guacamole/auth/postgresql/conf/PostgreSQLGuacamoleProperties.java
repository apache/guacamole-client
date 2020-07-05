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

package org.apache.guacamole.auth.postgresql.conf;

import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.EnumGuacamoleProperty;
import org.apache.guacamole.properties.FileGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Properties used by the PostgreSQL Authentication plugin.
 */
public class PostgreSQLGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private PostgreSQLGuacamoleProperties() {}

    /**
     * The URL of the PostgreSQL server hosting the Guacamole authentication tables.
     */
    public static final StringGuacamoleProperty POSTGRESQL_HOSTNAME =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-hostname"; }

    };

    /**
     * The port of the PostgreSQL server hosting the Guacamole authentication
     * tables.
     */
    public static final IntegerGuacamoleProperty POSTGRESQL_PORT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-port"; }

    };

    /**
     * The name of the PostgreSQL database containing the Guacamole
     * authentication tables.
     */
    public static final StringGuacamoleProperty POSTGRESQL_DATABASE =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-database"; }

    };

    /**
     * The username used to authenticate to the PostgreSQL database containing
     * the Guacamole authentication tables.
     */
    public static final StringGuacamoleProperty POSTGRESQL_USERNAME =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-username"; }

    };

    /**
     * The password used to authenticate to the PostgreSQL database containing
     * the Guacamole authentication tables.
     */
    public static final StringGuacamoleProperty POSTGRESQL_PASSWORD =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-password"; }

    };

    /**
     * Whether a user account within the database is required for authentication
     * to succeed, even if the user has been authenticated via another
     * authentication provider.
     */
    public static final BooleanGuacamoleProperty
            POSTGRESQL_USER_REQUIRED = new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-user-required"; }

    };

    /**
     * The maximum number of concurrent connections to allow overall. Zero
     * denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            POSTGRESQL_ABSOLUTE_MAX_CONNECTIONS =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-absolute-max-connections"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection. Zero denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            POSTGRESQL_DEFAULT_MAX_CONNECTIONS =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-default-max-connections"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection group. Zero denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-default-max-group-connections"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection by an individual user. Zero denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            POSTGRESQL_DEFAULT_MAX_CONNECTIONS_PER_USER =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-default-max-connections-per-user"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection group by an individual user. Zero denotes
     * unlimited.
     */
    public static final IntegerGuacamoleProperty
            POSTGRESQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-default-max-group-connections-per-user"; }

    };
    
    /**
     * The SSL mode that should be used by the JDBC driver when making
     * connections to the remote server.  By default SSL will be attempted but
     * plain-text will be allowed if SSL fails.
     */
    public static final EnumGuacamoleProperty<PostgreSQLSSLMode> POSTGRESQL_SSL_MODE =
            new EnumGuacamoleProperty<PostgreSQLSSLMode>(PostgreSQLSSLMode.class) {
        
        @Override
        public String getName() { return "postgresql-ssl-mode"; }
        
    };
    
    /**
     * The client SSL certificate file used by the JDBC driver to make the
     * SSL connection.
     */
    public static final FileGuacamoleProperty POSTGRESQL_SSL_CERT_FILE =
            new FileGuacamoleProperty() {
             
        @Override
        public String getName() { return "postgresql-ssl-cert-file"; }
                
    };
    
    /**
     * The client SSL private key file used by the JDBC driver to make the
     * SSL connection.
     */
    public static final FileGuacamoleProperty POSTGRESQL_SSL_KEY_FILE =
            new FileGuacamoleProperty() {
    
        @Override
        public String getName() { return "postgresql-ssl-key-file"; }
        
    };
    
    /**
     * The client SSL root certificate file used by the JDBC driver to validate
     * certificates when making the SSL connection.
     */
    public static final FileGuacamoleProperty POSTGRESQL_SSL_ROOT_CERT_FILE =
            new FileGuacamoleProperty() {
        
        @Override
        public String getName() { return "postgresql-ssl-root-cert-file"; }
        
    };
    
    /**
     * The password of the SSL private key used by the JDBC driver to make
     * the SSL connection to the PostgreSQL server.
     */
    public static final StringGuacamoleProperty POSTGRESQL_SSL_KEY_PASSWORD =
            new StringGuacamoleProperty() {
        
        @Override
        public String getName() { return "postgresql-ssl-key-password"; }
        
    };
    
    /**
     * Whether or not to automatically create accounts in the PostgreSQL
     * database for users who successfully authenticate through another
     * extension. By default users will not be automatically created.
     */
    public static final BooleanGuacamoleProperty POSTGRESQL_AUTO_CREATE_ACCOUNTS =
            new BooleanGuacamoleProperty() {
                
        @Override
        public String getName() { return "postgresql-auto-create-accounts"; }
                
    };
    
}
