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

import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glyptodon.guacamole.net.basic.rest.auth.TokenRESTService;
import org.glyptodon.guacamole.net.basic.rest.connection.ConnectionRESTService;
import org.glyptodon.guacamole.net.basic.rest.connectiongroup.ConnectionGroupRESTService;
import org.glyptodon.guacamole.net.basic.rest.activeconnection.ActiveConnectionRESTService;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.net.basic.rest.auth.SecureRandomAuthTokenGenerator;
import org.glyptodon.guacamole.net.basic.rest.auth.TokenSessionMap;
import org.glyptodon.guacamole.net.basic.rest.history.HistoryRESTService;
import org.glyptodon.guacamole.net.basic.rest.language.LanguageRESTService;
import org.glyptodon.guacamole.net.basic.rest.schema.SchemaRESTService;
import org.glyptodon.guacamole.net.basic.rest.user.UserRESTService;

/**
 * A Guice Module to set up the servlet mappings and authentication-specific
 * dependency injection for the Guacamole REST API.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class RESTServiceModule extends ServletModule {

    /**
     * Singleton instance of TokenSessionMap.
     */
    private final TokenSessionMap tokenSessionMap;

    /**
     * Creates a module which handles binding of REST services and related
     * authentication objects, including the singleton TokenSessionMap.
     *
     * @param tokenSessionMap
     *     An instance of TokenSessionMap to inject as a singleton wherever
     *     needed.
     */
    public RESTServiceModule(TokenSessionMap tokenSessionMap) {
        this.tokenSessionMap = tokenSessionMap;
    }

    @Override
    protected void configureServlets() {

        // Bind session map
        bind(TokenSessionMap.class).toInstance(tokenSessionMap);

        // Bind low-level services
        bind(AuthenticationService.class);
        bind(AuthTokenGenerator.class).to(SecureRandomAuthTokenGenerator.class);

        // Automatically translate GuacamoleExceptions for REST methods
        bindInterceptor(
            Matchers.any(),
            new RESTMethodMatcher(),
            new RESTExceptionWrapper(tokenSessionMap)
        );

        // Bind convenience services used by the REST API
        bind(ObjectRetrievalService.class);

        // Set up the API endpoints
        bind(ActiveConnectionRESTService.class);
        bind(ConnectionGroupRESTService.class);
        bind(ConnectionRESTService.class);
        bind(HistoryRESTService.class);
        bind(LanguageRESTService.class);
        bind(SchemaRESTService.class);
        bind(TokenRESTService.class);
        bind(UserRESTService.class);

        // Set up the servlet and JSON mappings
        bind(GuiceContainer.class);
        bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
        serve("/api/*").with(GuiceContainer.class);

    }

}
