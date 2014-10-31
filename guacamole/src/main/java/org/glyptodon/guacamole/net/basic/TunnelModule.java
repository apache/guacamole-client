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

package org.glyptodon.guacamole.net.basic;

import com.google.inject.Provider;
import com.google.inject.servlet.ServletModule;
import java.util.Arrays;
import javax.servlet.http.HttpServlet;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.basic.websocket.BasicGuacamoleWebSocketTunnelEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module which loads tunnel implementations.
 * 
 * @author Michael Jumper
 */
public class TunnelModule extends ServletModule {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(TunnelModule.class);

    /**
     * Classnames of all legacy (non-JSR) WebSocket tunnel implementations.
     */
    private static final String[] WEBSOCKET_CLASSES = {
        "org.glyptodon.guacamole.net.basic.websocket.jetty8.BasicGuacamoleWebSocketTunnelServlet",
        "org.glyptodon.guacamole.net.basic.websocket.jetty9.BasicGuacamoleWebSocketTunnelServlet",
        "org.glyptodon.guacamole.net.basic.websocket.tomcat.BasicGuacamoleWebSocketTunnelServlet"
    };

    /**
     * Checks for JSR 356 support, returning true if such support is found, and
     * false otherwise.
     *
     * @return true if support for JSR 356 is found, false otherwise.
     */
    private boolean implementsJSR_356() {

        try {

            // Attempt to find WebSocket servlet
            GuacamoleClassLoader.getInstance().findClass("javax.websocket.Endpoint");

            // JSR 356 found
            return true;

        }

        // If no such servlet class, this particular WebSocket support
        // is not present
        catch (ClassNotFoundException e) {}
        catch (NoClassDefFoundError e) {}

        // Log all GuacamoleExceptions
        catch (GuacamoleException e) {
            logger.error("Unable to load/detect WebSocket support: {}", e.getMessage());
            logger.debug("Error loading/detecting WebSocket support.", e);
        }
        
        // JSR 356 not found
        return false;
        
    }
    
    private boolean loadWebSocketTunnel(String classname) {

        try {

            // Attempt to find WebSocket servlet
            Class<HttpServlet> servlet = (Class<HttpServlet>)
                    GuacamoleClassLoader.getInstance().findClass(classname);

            // Add WebSocket servlet
            serve("/websocket-tunnel").with(servlet);
            return true;

        }

        // If no such servlet class, this particular WebSocket support
        // is not present
        catch (ClassNotFoundException e) {}
        catch (NoClassDefFoundError e) {}

        // Log all GuacamoleExceptions
        catch (GuacamoleException e) {
            logger.error("Unable to load/detect WebSocket support: {}", e.getMessage());
            logger.debug("Error loading/detecting WebSocket support.", e);
        }

        // Load attempt failed
        return false;

    }

    @Override
    protected void configureServlets() {

        bind(TunnelRequestService.class);

        // Set up HTTP tunnel
        serve("/tunnel").with(BasicGuacamoleTunnelServlet.class);

        // Check for JSR 356 support
        if (implementsJSR_356()) {

            logger.info("JSR-356 WebSocket support present.");

            // Get container
            ServerContainer container = (ServerContainer) getServletContext().getAttribute("javax.websocket.server.ServerContainer"); 
            if (container == null) {
                logger.warn("ServerContainer attribute required by JSR-356 is missing. Cannot load JSR-356 WebSocket support.");
                return;
            }

            Provider<TunnelRequestService> tunnelRequestServiceProvider = getProvider(TunnelRequestService.class);

            // Build configuration for WebSocket tunnel
            ServerEndpointConfig config =
                    ServerEndpointConfig.Builder.create(BasicGuacamoleWebSocketTunnelEndpoint.class, "/websocket-tunnel")
                                                .configurator(new BasicGuacamoleWebSocketTunnelEndpoint.Configurator(tunnelRequestServiceProvider))
                                                .subprotocols(Arrays.asList(new String[]{"guacamole"}))
                                                .build();

            try {

                // Add configuration to container
                container.addEndpoint(config);

            }
            catch (DeploymentException e) {
                logger.error("Unable to deploy WebSocket tunnel.", e);
            }
            
            return;
        }
        
        // Try to load each WebSocket tunnel in sequence
        for (String classname : WEBSOCKET_CLASSES) {
            if (loadWebSocketTunnel(classname)) {
                logger.info("Legacy (non-JSR) WebSocket support loaded: {}", classname);
                return;
            }
        }

        // Warn of lack of WebSocket
        logger.info("WebSocket support NOT present. Only HTTP will be used.");

    }

}

