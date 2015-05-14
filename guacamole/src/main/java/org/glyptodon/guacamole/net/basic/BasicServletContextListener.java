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

package org.glyptodon.guacamole.net.basic;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import javax.servlet.ServletContextEvent;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.environment.LocalEnvironment;
import org.glyptodon.guacamole.net.basic.extension.ExtensionModule;
import org.glyptodon.guacamole.net.basic.log.LogModule;
import org.glyptodon.guacamole.net.basic.rest.RESTAuthModule;
import org.glyptodon.guacamole.net.basic.rest.RESTServletModule;
import org.glyptodon.guacamole.net.basic.rest.auth.BasicTokenSessionMap;
import org.glyptodon.guacamole.net.basic.rest.auth.TokenSessionMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ServletContextListener to listen for initialization of the servlet context
 * in order to set up dependency injection.
 *
 * @author James Muehlner
 */
public class BasicServletContextListener extends GuiceServletContextListener {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(BasicServletContextListener.class);

    /**
     * The Guacamole server environment.
     */
    private Environment environment;

    /**
     * Singleton instance of a TokenSessionMap.
     */
    private TokenSessionMap sessionMap;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        try {
            environment = new LocalEnvironment();
            sessionMap = new BasicTokenSessionMap(environment);
        }
        catch (GuacamoleException e) {
            logger.error("Unable to read guacamole.properties: {}", e.getMessage());
            logger.debug("Error reading guacamole.properties.", e);
            throw new RuntimeException(e);
        }

        super.contextInitialized(servletContextEvent);

    }

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(Stage.PRODUCTION,
            new EnvironmentModule(environment),
            new LogModule(environment),
            new ExtensionModule(environment),
            new RESTAuthModule(sessionMap),
            new RESTServletModule(),
            new TunnelModule()
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

        super.contextDestroyed(servletContextEvent);

        // Shutdown TokenSessionMap
        if (sessionMap != null)
            sessionMap.shutdown();

    }

}
