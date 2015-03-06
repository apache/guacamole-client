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

import org.glyptodon.guacamole.properties.BooleanGuacamoleProperty;
import org.glyptodon.guacamole.properties.IntegerGuacamoleProperty;
import org.glyptodon.guacamole.properties.StringGuacamoleProperty;

/**
 * Properties used by the PostgreSQL Authentication plugin.
 *
 * @author James Muehlner
 * @author Michael Jumper
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
    
}
