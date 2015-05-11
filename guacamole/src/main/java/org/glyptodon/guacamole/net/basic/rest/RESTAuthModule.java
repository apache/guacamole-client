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

package org.glyptodon.guacamole.net.basic.rest;

import com.google.inject.AbstractModule;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.net.basic.rest.auth.SecureRandomAuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.TokenSessionMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Guice Module for setting up authentication-specific dependency injection.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class RESTAuthModule extends AbstractModule {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RESTAuthModule.class);

    /**
     * The Guacamole server environment.
     */
    private final Environment environment;

    /**
     * Singleton instance of TokenSessionMap.
     */
    private final TokenSessionMap tokenSessionMap;

    /**
     * Creates a module which handles binding of authentication-related
     * objects, including the singleton TokenSessionMap.
     *
     * @param environment
     *     The environment to use when configuring authentication.
     *
     * @param tokenSessionMap
     *     An instance of TokenSessionMap to inject as a singleton wherever
     *     needed.
     */
    public RESTAuthModule(Environment environment,
            TokenSessionMap tokenSessionMap) {
        this.environment = environment;
        this.tokenSessionMap = tokenSessionMap;
    }

    @Override
    protected void configure() {

        // Bind session map
        bind(TokenSessionMap.class).toInstance(tokenSessionMap);

        // Bind low-level services
        bind(AuthenticationService.class);
        bind(AuthTokenGenerator.class).to(SecureRandomAuthTokenGenerator.class);

        // Get and bind auth provider instance, if defined via property
        try {

            // Use "auth-provider" property if present, but warn about deprecation
            AuthenticationProvider authProvider = environment.getProperty(BasicGuacamoleProperties.AUTH_PROVIDER);
            if (authProvider != null) {
                logger.warn("The \"auth-provider\" is now deprecated. Please use the \"extensions\" directory within GUACAMOLE_HOME instead.");
                bind(AuthenticationProvider.class).toInstance(authProvider);
            }

        }
        catch (GuacamoleException e) {
            logger.warn("Value of deprecated \"auth-provider\" property within guacamole.properties is not valid: {}", e.getMessage());
            logger.debug("Error reading authentication provider from guacamole.properties.", e);
        }

    }

}
