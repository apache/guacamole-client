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

package org.apache.guacamole.net.basic.websocket;

import com.google.inject.Provider;
import java.util.Map;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.basic.TunnelRequest;
import org.apache.guacamole.net.basic.TunnelRequestService;
import org.apache.guacamole.websocket.GuacamoleWebSocketTunnelEndpoint;

/**
 * Tunnel implementation which uses WebSocket as a tunnel backend, rather than
 * HTTP, properly parsing connection IDs included in the connection request.
 */
public class BasicGuacamoleWebSocketTunnelEndpoint extends GuacamoleWebSocketTunnelEndpoint {

    /**
     * Unique string which shall be used to store the TunnelRequest
     * associated with a WebSocket connection.
     */
    private static final String TUNNEL_REQUEST_PROPERTY = "WS_GUAC_TUNNEL_REQUEST";

    /**
     * Unique string which shall be used to store the TunnelRequestService to
     * be used for processing TunnelRequests.
     */
    private static final String TUNNEL_REQUEST_SERVICE_PROPERTY = "WS_GUAC_TUNNEL_REQUEST_SERVICE";

    /**
     * Configurator implementation which stores the requested GuacamoleTunnel
     * within the user properties. The GuacamoleTunnel will be later retrieved
     * during the connection process.
     */
    public static class Configurator extends ServerEndpointConfig.Configurator {

        /**
         * Provider which provides instances of a service for handling
         * tunnel requests.
         */
        private final Provider<TunnelRequestService> tunnelRequestServiceProvider;
         
        /**
         * Creates a new Configurator which uses the given tunnel request
         * service provider to retrieve the necessary service to handle new
         * connections requests.
         * 
         * @param tunnelRequestServiceProvider
         *     The tunnel request service provider to use for all new
         *     connections.
         */
        public Configurator(Provider<TunnelRequestService> tunnelRequestServiceProvider) {
            this.tunnelRequestServiceProvider = tunnelRequestServiceProvider;
        }
        
        @Override
        public void modifyHandshake(ServerEndpointConfig config,
                HandshakeRequest request, HandshakeResponse response) {

            super.modifyHandshake(config, request, response);
            
            // Store tunnel request and tunnel request service for retrieval
            // upon WebSocket open
            Map<String, Object> userProperties = config.getUserProperties();
            userProperties.clear();
            userProperties.put(TUNNEL_REQUEST_PROPERTY, new WebSocketTunnelRequest(request));
            userProperties.put(TUNNEL_REQUEST_SERVICE_PROPERTY, tunnelRequestServiceProvider.get());

        }
        
    }
    
    @Override
    protected GuacamoleTunnel createTunnel(Session session,
            EndpointConfig config) throws GuacamoleException {

        Map<String, Object> userProperties = config.getUserProperties();

        // Get original tunnel request
        TunnelRequest tunnelRequest = (TunnelRequest) userProperties.get(TUNNEL_REQUEST_PROPERTY);
        if (tunnelRequest == null)
            return null;

        // Get tunnel request service
        TunnelRequestService tunnelRequestService = (TunnelRequestService) userProperties.get(TUNNEL_REQUEST_SERVICE_PROPERTY);
        if (tunnelRequestService == null)
            return null;

        // Create and return tunnel
        return tunnelRequestService.createTunnel(tunnelRequest);

    }

}
