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

package org.glyptodon.guacamole.net.basic.websocket.jsr;

import java.io.IOException;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.io.GuacamoleWriter;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A WebSocket implementation of GuacamoleTunnel functionality, compatible with
 * the Guacamole.WebSocketTunnel object included with the JavaScript API.
 * Messages sent/received are simply chunks of the Guacamole protocol
 * instruction stream.
 *
 * @author Michael Jumper
 */
public abstract class GuacamoleWebSocketTunnelEndpoint extends Endpoint {

    /**
     * The default, minimum buffer size for instructions.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(GuacamoleWebSocketTunnelEndpoint.class);

    /**
     * The underlying GuacamoleTunnel. WebSocket reads/writes will be handled
     * as reads/writes to this tunnel.
     */
    private GuacamoleTunnel tunnel;
    
    /**
     * Sends the given status on the given WebSocket connection and closes the
     * connection.
     *
     * @param session The outbound WebSocket connection to close.
     * @param guac_status The status to send.
     */
    private void closeConnection(Session session, GuacamoleStatus guac_status) {

        try {
            CloseCode code = CloseReason.CloseCodes.getCloseCode(guac_status.getWebSocketCode());
            String message = Integer.toString(guac_status.getGuacamoleStatusCode());
            session.close(new CloseReason(code, message));
        }
        catch (IOException e) {
            logger.error("Unable to close WebSocket connection.", e);
        }

    }

    /**
     * Returns a new tunnel for the given session. How this tunnel is created
     * or retrieved is implementation-dependent.
     *
     * @param session The session associated with the active WebSocket
     *                connection.
     * @param config Configuration information associated with the instance of
     *               the endpoint created for handling this single connection.
     * @return A connected tunnel, or null if no such tunnel exists.
     * @throws GuacamoleException If an error occurs while retrieving the
     *                            tunnel, or if access to the tunnel is denied.
     */
    protected abstract GuacamoleTunnel createTunnel(Session session, EndpointConfig config) throws GuacamoleException;

    @Override
    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {

        try {

            // Get tunnel
            tunnel = createTunnel(session, config);
            if (tunnel == null) {
                closeConnection(session, GuacamoleStatus.RESOURCE_NOT_FOUND);
                return;
            }

        }
        catch (GuacamoleException e) {
            logger.error("Error connecting WebSocket tunnel.", e);
            closeConnection(session, e.getStatus());
            return;
        }

        // Manually register message handler
        session.addMessageHandler(new MessageHandler.Whole<String>() {

            @Override
            public void onMessage(String message) {
                GuacamoleWebSocketTunnelEndpoint.this.onMessage(message);
            }

        });

        // Prepare read transfer thread
        Thread readThread = new Thread() {

            /**
             * Remote (client) side of this connection
             */
            private final RemoteEndpoint.Basic remote = session.getBasicRemote();
                
            @Override
            public void run() {

                StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
                GuacamoleReader reader = tunnel.acquireReader();
                char[] readMessage;

                try {

                    try {

                        // Attempt to read
                        while ((readMessage = reader.read()) != null) {

                            // Buffer message
                            buffer.append(readMessage);

                            // Flush if we expect to wait or buffer is getting full
                            if (!reader.available() || buffer.length() >= BUFFER_SIZE) {
                                remote.sendText(buffer.toString());
                                buffer.setLength(0);
                            }

                        }

                        // No more data
                        closeConnection(session, GuacamoleStatus.SUCCESS);

                    }

                    // Catch any thrown guacamole exception and attempt
                    // to pass within the WebSocket connection, logging
                    // each error appropriately.
                    catch (GuacamoleClientException e) {
                        logger.warn("Client request rejected: {}", e.getMessage());
                        closeConnection(session, e.getStatus());
                    }
                    catch (GuacamoleException e) {
                        logger.error("Internal server error.", e);
                        closeConnection(session, e.getStatus());
                    }

                }
                catch (IOException e) {
                    logger.debug("I/O error prevents further reads.", e);
                }

            }

        };

        readThread.start();

    }

    @OnMessage
    public void onMessage(String message) {

        GuacamoleWriter writer = tunnel.acquireWriter();

        try {
            // Write received message
            writer.write(message.toCharArray());
        }
        catch (GuacamoleException e) {
            logger.debug("Tunnel write failed.", e);
        }

        tunnel.releaseWriter();

    }
    
    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

        try {
            if (tunnel != null)
                tunnel.close();
        }
        catch (GuacamoleException e) {
            logger.debug("Unable to close WebSocket tunnel.", e);
        }
        
    }

}

