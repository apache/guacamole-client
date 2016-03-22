/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.apache.guacamole.tunnel.websocket;

import com.google.inject.Provider;
import com.google.inject.servlet.ServletModule;
import java.util.Arrays;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.apache.guacamole.tunnel.TunnelLoader;
import org.apache.guacamole.tunnel.TunnelRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the JSR-356 WebSocket tunnel implementation.
 * 
 * @author Michael Jumper
 */
public class WebSocketTunnelModule extends ServletModule implements TunnelLoader {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(WebSocketTunnelModule.class);

    @Override
    public boolean isSupported() {

        try {

            // Attempt to find WebSocket servlet
            Class.forName("javax.websocket.Endpoint");

            // Support found
            return true;

        }

        // If no such servlet class, this particular WebSocket support
        // is not present
        catch (ClassNotFoundException e) {}
        catch (NoClassDefFoundError e) {}

        // Support not found
        return false;
        
    }
    
    @Override
    public void configureServlets() {

        logger.info("Loading JSR-356 WebSocket support...");

        // Get container
        ServerContainer container = (ServerContainer) getServletContext().getAttribute("javax.websocket.server.ServerContainer"); 
        if (container == null) {
            logger.warn("ServerContainer attribute required by JSR-356 is missing. Cannot load JSR-356 WebSocket support.");
            return;
        }

        Provider<TunnelRequestService> tunnelRequestServiceProvider = getProvider(TunnelRequestService.class);

        // Build configuration for WebSocket tunnel
        ServerEndpointConfig config =
                ServerEndpointConfig.Builder.create(RestrictedGuacamoleWebSocketTunnelEndpoint.class, "/websocket-tunnel")
                                            .configurator(new RestrictedGuacamoleWebSocketTunnelEndpoint.Configurator(tunnelRequestServiceProvider))
                                            .subprotocols(Arrays.asList(new String[]{"guacamole"}))
                                            .build();

        try {

            // Add configuration to container
            container.addEndpoint(config);

        }
        catch (DeploymentException e) {
            logger.error("Unable to deploy WebSocket tunnel.", e);
        }

    }

}
