/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.websocket;

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
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A WebSocket implementation of GuacamoleTunnel functionality, compatible with
 * the Guacamole.WebSocketTunnel object included with the JavaScript API.
 * Messages sent/received are simply chunks of the Guacamole protocol
 * instruction stream.
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
            logger.debug("Unable to close WebSocket connection.", e);
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
    protected abstract GuacamoleTunnel createTunnel(Session session, EndpointConfig config)
            throws GuacamoleException;

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
            logger.error("Creation of WebSocket tunnel to guacd failed: {}", e.getMessage());
            logger.debug("Error connecting WebSocket tunnel.", e);
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

                    // Send tunnel UUID
                    remote.sendText(new GuacamoleInstruction(
                        GuacamoleTunnel.INTERNAL_DATA_OPCODE,
                        tunnel.getUUID().toString()
                    ).toString());

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
                        logger.info("WebSocket connection terminated: {}", e.getMessage());
                        logger.debug("WebSocket connection terminated due to client error.", e);
                        closeConnection(session, e.getStatus());
                    }
                    catch (GuacamoleConnectionClosedException e) {
                        logger.debug("Connection to guacd closed.", e);
                        closeConnection(session, GuacamoleStatus.SUCCESS);
                    }
                    catch (GuacamoleException e) {
                        logger.error("Connection to guacd terminated abnormally: {}", e.getMessage());
                        logger.debug("Internal error during connection to guacd.", e);
                        closeConnection(session, e.getStatus());
                    }

                }
                catch (IOException e) {
                    logger.debug("I/O error prevents further reads.", e);
                    closeConnection(session, GuacamoleStatus.SERVER_ERROR);
                }

            }

        };

        readThread.start();

    }

    @OnMessage
    public void onMessage(String message) {

        // Ignore inbound messages if there is no associated tunnel
        if (tunnel == null)
            return;

        GuacamoleWriter writer = tunnel.acquireWriter();

        try {
            // Write received message
            writer.write(message.toCharArray());
        }
        catch (GuacamoleConnectionClosedException e) {
            logger.debug("Connection to guacd closed.", e);
        }
        catch (GuacamoleException e) {
            logger.debug("WebSocket tunnel write failed.", e);
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

