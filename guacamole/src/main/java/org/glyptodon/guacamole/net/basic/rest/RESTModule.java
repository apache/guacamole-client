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

package org.glyptodon.guacamole.net.basic.rest;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.net.basic.rest.auth.BasicTokenUserContextMap;
import org.glyptodon.guacamole.net.basic.rest.auth.SecureRandomAuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.TokenUserContextMap;
import org.glyptodon.guacamole.net.basic.rest.connection.ConnectionService;
import org.glyptodon.guacamole.net.basic.rest.connectiongroup.ConnectionGroupService;
import org.glyptodon.guacamole.net.basic.rest.permission.PermissionService;
import org.glyptodon.guacamole.net.basic.rest.user.UserService;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Guice Module for setting up dependency injection for the 
 * Guacamole REST API.
 * 
 * @author James Muehlner
 */
public class RESTModule extends AbstractModule {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RESTModule.class);

    /**
     * The AuthenticationProvider to use to authenticate all requests.
     */
    private AuthenticationProvider authProvider;

    @Override
    protected void configure() {

        // Get auth provider instance
        try {
            authProvider = GuacamoleProperties.getRequiredProperty(BasicGuacamoleProperties.AUTH_PROVIDER);
        }
        catch (GuacamoleException e) {
            logger.error("Error getting authentication provider from properties.", e);
            throw new RuntimeException(e);
        }
        
        bind(AuthenticationProvider.class).toInstance(authProvider);
        bind(TokenUserContextMap.class).toInstance(new BasicTokenUserContextMap());
        bind(ConnectionService.class);
        bind(ConnectionGroupService.class);
        bind(PermissionService.class);
        bind(UserService.class);
        bind(AuthenticationService.class);
        
        bind(AuthTokenGenerator.class).to(SecureRandomAuthTokenGenerator.class);
        
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(AuthProviderRESTExposure.class), new AuthProviderRESTExceptionWrapper());
    }
    
}
