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

import org.apache.guacamole.properties.BooleanGuacamoleProperty;
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
     * Whether or not multiple users accessing the same connection at the same
     * time should be disallowed.
     */
    public static final BooleanGuacamoleProperty
            POSTGRESQL_DISALLOW_SIMULTANEOUS_CONNECTIONS =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-disallow-simultaneous-connections"; }

    };

    /**
     * Whether or not the same user accessing the same connection or connection
     * group at the same time should be disallowed.
     */
    public static final BooleanGuacamoleProperty
            POSTGRESQL_DISALLOW_DUPLICATE_CONNECTIONS =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "postgresql-disallow-duplicate-connections"; }

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

}
