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

import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Properties used by the MySQL Authentication plugin.
 *
 * @author James Muehlner
 */
public class MySQLGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private MySQLGuacamoleProperties() {}

    /**
     * The hostname of the MySQL server hosting the Guacamole authentication 
     * tables.
     */
    public static final StringGuacamoleProperty MYSQL_HOSTNAME = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-hostname"; }

    };

    /**
     * The port number of the MySQL server hosting the Guacamole authentication 
     * tables.
     */
    public static final IntegerGuacamoleProperty MYSQL_PORT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-port"; }

    };

    /**
     * The name of the MySQL database containing the Guacamole authentication 
     * tables.
     */
    public static final StringGuacamoleProperty MYSQL_DATABASE = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-database"; }

    };

    /**
     * The username that should be used when authenticating with the MySQL
     * database containing the Guacamole authentication tables.
     */
    public static final StringGuacamoleProperty MYSQL_USERNAME = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-username"; }

    };

    /**
     * The password that should be used when authenticating with the MySQL
     * database containing the Guacamole authentication tables.
     */
    public static final StringGuacamoleProperty MYSQL_PASSWORD = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-password"; }

    };

    /**
     * Whether or not multiple users accessing the same connection at the same 
     * time should be disallowed.
     */
    public static final BooleanGuacamoleProperty MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS = new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-disallow-simultaneous-connections"; }

    };

    /**
     * Whether or not the same user accessing the same connection or connection 
     * group at the same time should be disallowed.
     */
    public static final BooleanGuacamoleProperty MYSQL_DISALLOW_DUPLICATE_CONNECTIONS = new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-disallow-duplicate-connections"; }

    };

    /**
     * The maximum number of concurrent connections to allow overall. Zero
     * denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            MYSQL_ABSOLUTE_MAX_CONNECTIONS =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-absolute-max-connections"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection. Zero denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            MYSQL_DEFAULT_MAX_CONNECTIONS =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-default-max-connections"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection group. Zero denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-default-max-group-connections"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection by an individual user. Zero denotes unlimited.
     */
    public static final IntegerGuacamoleProperty
            MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-default-max-connections-per-user"; }

    };

    /**
     * The maximum number of concurrent connections to allow to any one
     * connection group by an individual user. Zero denotes
     * unlimited.
     */
    public static final IntegerGuacamoleProperty
            MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-default-max-group-connections-per-user"; }

    };

}
