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

package org.apache.guacamole.tunnel.websocket.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
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
     * The default, minimum buffer size for instructions.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(GuacamoleWebSocketTunnelServlet.class);

    /**
     * Sends the given status on the given WebSocket connection and closes the
     * connection.
     *
     * @param outbound The outbound WebSocket connection to close.
     * @param guac_status The status to send.
     */
    public void closeConnection(WsOutbound outbound, GuacamoleStatus guac_status) {

        try {
            byte[] message = Integer.toString(guac_status.getGuacamoleStatusCode()).getBytes("UTF-8");
            outbound.close(guac_status.getWebSocketCode(), ByteBuffer.wrap(message));
        }
        catch (IOException e) {
            logger.debug("Unable to close WebSocket tunnel.", e);
        }

    }

    @Override
    protected String selectSubProtocol(List<String> subProtocols) {

        // Search for expected protocol
        for (String protocol : subProtocols)
            if ("guacamole".equals(protocol))
                return "guacamole";
        
        // Otherwise, fail
        return null;

    }

    @Override
    public StreamInbound createWebSocketInbound(String protocol,
            HttpServletRequest request) {

        final TunnelRequest tunnelRequest = new HTTPTunnelRequest(request);

        // Return new WebSocket which communicates through tunnel
        return new StreamInbound() {

            /**
             * The GuacamoleTunnel associated with the connected WebSocket. If
             * the WebSocket has not yet been connected, this will be null.
             */
            private GuacamoleTunnel tunnel = null;

            @Override
            protected void onTextData(Reader reader) throws IOException {

                // Ignore inbound messages if there is no associated tunnel
                if (tunnel == null)
                    return;

                GuacamoleWriter writer = tunnel.acquireWriter();

                // Write all available data
                try {

                    char[] buffer = new char[BUFFER_SIZE];

                    int num_read;
                    while ((num_read = reader.read(buffer)) > 0)
                        writer.write(buffer, 0, num_read);

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
            public void onOpen(final WsOutbound outbound) {

                try {
                    tunnel = doConnect(tunnelRequest);
                }
                catch (GuacamoleException e) {
                    logger.error("Creation of WebSocket tunnel to guacd failed: {}", e.getMessage());
                    logger.debug("Error connecting WebSocket tunnel.", e);
                    closeConnection(outbound, e.getStatus());
                    return;
                }

                // Do not start connection if tunnel does not exist
                if (tunnel == null) {
                    closeConnection(outbound, GuacamoleStatus.RESOURCE_NOT_FOUND);
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
                            outbound.writeTextMessage(CharBuffer.wrap(new GuacamoleInstruction(
                                GuacamoleTunnel.INTERNAL_DATA_OPCODE,
                                tunnel.getUUID().toString()
                            ).toString()));

                            try {

                                // Attempt to read
                                while ((readMessage = reader.read()) != null) {

                                    // Buffer message
                                    buffer.append(readMessage);

                                    // Flush if we expect to wait or buffer is getting full
                                    if (!reader.available() || buffer.length() >= BUFFER_SIZE) {
                                        outbound.writeTextMessage(CharBuffer.wrap(buffer));
                                        buffer.setLength(0);
                                    }

                                }

                                // No more data
                                closeConnection(outbound, GuacamoleStatus.SUCCESS);

                            }

                            // Catch any thrown guacamole exception and attempt
                            // to pass within the WebSocket connection, logging
                            // each error appropriately.
                            catch (GuacamoleClientException e) {
                                logger.info("WebSocket connection terminated: {}", e.getMessage());
                                logger.debug("WebSocket connection terminated due to client error.", e);
                                closeConnection(outbound, e.getStatus());
                            }
                            catch (GuacamoleConnectionClosedException e) {
                                logger.debug("Connection to guacd closed.", e);
                                closeConnection(outbound, GuacamoleStatus.SUCCESS);
                            }
                            catch (GuacamoleException e) {
                                logger.error("Connection to guacd terminated abnormally: {}", e.getMessage());
                                logger.debug("Internal error during connection to guacd.", e);
                                closeConnection(outbound, e.getStatus());
                            }

                        }
                        catch (IOException e) {
                            logger.debug("I/O error prevents further reads.", e);
                            closeConnection(outbound, GuacamoleStatus.SERVER_ERROR);
                        }

                    }

                };

                readThread.start();

            }

            @Override
            public void onClose(int i) {
                try {
                    if (tunnel != null)
                        tunnel.close();
                }
                catch (GuacamoleException e) {
                    logger.debug("Unable to close connection to guacd.", e);
                }
            }

            @Override
            protected void onBinaryData(InputStream in) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
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

