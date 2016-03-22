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

package org.apache.guacamole.tunnel.websocket.jetty9;

import java.io.IOException;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket listener implementation which provides a Guacamole tunnel
 * 
 * @author Michael Jumper
 */
public abstract class GuacamoleWebSocketTunnelListener implements WebSocketListener {

    /**
     * The default, minimum buffer size for instructions.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RestrictedGuacamoleWebSocketTunnelServlet.class);

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
            int code = guac_status.getWebSocketCode();
            String message = Integer.toString(guac_status.getGuacamoleStatusCode());
            session.close(new CloseStatus(code, message));
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
     * @return A connected tunnel, or null if no such tunnel exists.
     * @throws GuacamoleException If an error occurs while retrieving the
     *                            tunnel, or if access to the tunnel is denied.
     */
    protected abstract GuacamoleTunnel createTunnel(Session session)
            throws GuacamoleException;

    @Override
    public void onWebSocketConnect(final Session session) {

        try {

            // Get tunnel
            tunnel = createTunnel(session);
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

        // Prepare read transfer thread
        Thread readThread = new Thread() {

            /**
             * Remote (client) side of this connection
             */
            private final RemoteEndpoint remote = session.getRemote();
                
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
                                remote.sendString(buffer.toString());
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
                }

            }

        };

        readThread.start();

    }

    @Override
    public void onWebSocketText(String message) {

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
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        throw new UnsupportedOperationException("Binary WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketError(Throwable t) {

        logger.debug("WebSocket tunnel closing due to error.", t);
        
        try {
            if (tunnel != null)
                tunnel.close();
        }
        catch (GuacamoleException e) {
            logger.debug("Unable to close connection to guacd.", e);
        }

     }

   
    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        try {
            if (tunnel != null)
                tunnel.close();
        }
        catch (GuacamoleException e) {
            logger.debug("Unable to close connection to guacd.", e);
        }
        
    }

}
