package org.glyptodon.guacamole.net.basic.rest;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.inject.AbstractModule;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.BasicTokenUserContextMap;
import org.glyptodon.guacamole.net.basic.rest.auth.SecureRandomAuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.TokenUserContextMap;
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
        
        bind(AuthTokenGenerator.class).to(SecureRandomAuthTokenGenerator.class);
    }
    
}
