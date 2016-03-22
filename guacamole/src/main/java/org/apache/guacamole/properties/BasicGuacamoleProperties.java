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

package org.apache.guacamole.properties;

import org.apache.guacamole.properties.FileGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Properties used by the default Guacamole web application.
 *
 * @author Michael Jumper
 */
public class BasicGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private BasicGuacamoleProperties() {}

    /**
     * The authentication provider to user when retrieving the authorized
     * configurations of a user. This property is currently supported, but
     * deprecated in favor of the GUACAMOLE_HOME/extensions directory.
     */
    @Deprecated
    public static final AuthenticationProviderProperty AUTH_PROVIDER = new AuthenticationProviderProperty() {

        @Override
        public String getName() { return "auth-provider"; }

    };

    /**
     * The directory to search for authentication provider classes. This
     * property is currently supported, but deprecated in favor of the
     * GUACAMOLE_HOME/lib directory.
     */
    @Deprecated
    public static final FileGuacamoleProperty LIB_DIRECTORY = new FileGuacamoleProperty() {

        @Override
        public String getName() { return "lib-directory"; }

    };

    /**
     * The session timeout for the API, in minutes.
     */
    public static final IntegerGuacamoleProperty API_SESSION_TIMEOUT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "api-session-timeout"; }

    };

    /**
     * Comma-separated list of all allowed languages, where each language is
     * represented by a language key, such as "en" or "en_US". If specified,
     * only languages within this list will be listed as available by the REST
     * service.
     */
    public static final StringSetProperty ALLOWED_LANGUAGES = new StringSetProperty() {

        @Override
        public String getName() { return "allowed-languages"; }

    };

}
