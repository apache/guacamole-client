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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.auth.jdbc.JDBCAuthenticationProviderModule;
import org.glyptodon.guacamole.auth.jdbc.user.UserContextService;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.environment.LocalEnvironment;

/**
 * Provides a MySQL based implementation of the AuthenticationProvider
 * functionality.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class MySQLAuthenticationProvider implements AuthenticationProvider {

    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;

    /**
     * Creates a new MySQLAuthenticationProvider that reads and writes
     * authentication data to a MySQL database defined by properties in
     * guacamole.properties.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public MySQLAuthenticationProvider() throws GuacamoleException {

        // Get local environment
        Environment environment = new LocalEnvironment();

        // Set up Guice injector.
        injector = Guice.createInjector(

            // Configure MySQL-specific authentication
            new MySQLAuthenticationProviderModule(environment),

            // Configure JDBC authentication core
            new JDBCAuthenticationProviderModule(environment)

        );

    }

    @Override
    public UserContext getUserContext(Credentials credentials)
            throws GuacamoleException {

        // Create UserContext based on credentials, if valid
        UserContextService userContextService = injector.getInstance(UserContextService.class);
        return userContextService.getUserContext(credentials);

    }

    @Override
    public UserContext updateUserContext(UserContext context,
        Credentials credentials) throws GuacamoleException {

        // No need to update the context
        return context;

    }

}
