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

package org.apache.guacamole.tunnel.websocket.jetty8;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.tunnel.http.HTTPTunnelRequest;
import org.apache.guacamole.tunnel.TunnelRequest;
import org.apache.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A WebSocketServlet partial re-implementation of GuacamoleTunnelServlet.
 */
public abstract class GuacamoleWebSocketTunnelServlet extends WebSocketServlet {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(GuacamoleWebSocketTunnelServlet.class);
    
    /**
     * The default, minimum buffer size for instructions.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Sends the given status on the given WebSocket connection and closes the
     * connection.
     *
     * @param connection The WebSocket connection to close.
     * @param guac_status The status to send.
     */
    public static void closeConnection(Connection connection,
            GuacamoleStatus guac_status) {

        connection.close(guac_status.getWebSocketCode(),
                Integer.toString(guac_status.getGuacamoleStatusCode()));

    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {

        final TunnelRequest tunnelRequest = new HTTPTunnelRequest(request);

        // Return new WebSocket which communicates through tunnel
        return new WebSocket.OnTextMessage() {

            /**
             * The GuacamoleTunnel associated with the connected WebSocket. If
             * the WebSocket has not yet been connected, this will be null.
             */
            private GuacamoleTunnel tunnel = null;

            @Override
            public void onMessage(String string) {

                // Ignore inbound messages if there is no associated tunnel
                if (tunnel == null)
                    return;

                GuacamoleWriter writer = tunnel.acquireWriter();

                // Write message received
                try {
                    writer.write(string.toCharArray());
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
            public void onOpen(final Connection connection) {

                try {
                    tunnel = doConnect(tunnelRequest);
                }
                catch (GuacamoleException e) {
                    logger.error("Creation of WebSocket tunnel to guacd failed: {}", e.getMessage());
                    logger.debug("Error connecting WebSocket tunnel.", e);
                    closeConnection(connection, e.getStatus());
                    return;
                }

                // Do not start connection if tunnel does not exist
                if (tunnel == null) {
                    closeConnection(connection, GuacamoleStatus.RESOURCE_NOT_FOUND);
                    return;
                }

                Thread readThread = new Thread() {

                    @Override
                    public void run() {

                        StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
                        GuacamoleReader reader = tunnel.acquireReader();
                        char[] readMessage;

                        try {

                            // Send tunnel UUID
                            connection.sendMessage(new GuacamoleInstruction(
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
                                        connection.sendMessage(buffer.toString());
                                        buffer.setLength(0);
                                    }

                                }

                                // No more data
                                closeConnection(connection, GuacamoleStatus.SUCCESS);
                                
                            }

                            // Catch any thrown guacamole exception and attempt
                            // to pass within the WebSocket connection, logging
                            // each error appropriately.
                            catch (GuacamoleClientException e) {
                                logger.info("WebSocket connection terminated: {}", e.getMessage());
                                logger.debug("WebSocket connection terminated due to client error.", e);
                                closeConnection(connection, e.getStatus());
                            }
                            catch (GuacamoleConnectionClosedException e) {
                                logger.debug("Connection to guacd closed.", e);
                                closeConnection(connection, GuacamoleStatus.SUCCESS);
                            }
                            catch (GuacamoleException e) {
                                logger.error("Connection to guacd terminated abnormally: {}", e.getMessage());
                                logger.debug("Internal error during connection to guacd.", e);
                                closeConnection(connection, e.getStatus());
                            }

                        }
                        catch (IOException e) {
                            logger.debug("WebSocket tunnel read failed due to I/O error.", e);
                            closeConnection(connection, GuacamoleStatus.SERVER_ERROR);
                        }

                    }

                };

                readThread.start();

            }

            @Override
            public void onClose(int i, String string) {
                try {
                    if (tunnel != null)
                        tunnel.close();
                }
                catch (GuacamoleException e) {
                    logger.debug("Unable to close connection to guacd.", e);
                }
            }

        };

    }

    /**
     * Called whenever the JavaScript Guacamole client makes a connection
     * request. It it up to the implementor of this function to define what
     * conditions must be met for a tunnel to be configured and returned as a
     * result of this connection request (whether some sort of credentials must
     * be specified, for example).
     *
     * @param request
     *     The TunnelRequest associated with the connection request received.
     *     Any parameters specified along with the connection request can be
     *     read from this object.
     *
     * @return
     *     A newly constructed GuacamoleTunnel if successful, null otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while constructing the GuacamoleTunnel, or if the
     *     conditions required for connection are not met.
     */
    protected abstract GuacamoleTunnel doConnect(TunnelRequest request)
            throws GuacamoleException;

}

