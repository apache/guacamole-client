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

package org.glyptodon.guacamole.net.basic.websocket.jetty;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.io.GuacamoleWriter;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A WebSocketServlet partial re-implementation of GuacamoleTunnelServlet.
 *
 * @author Michael Jumper
 */
public abstract class GuacamoleWebSocketTunnelServlet extends WebSocketServlet {

    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(GuacamoleWebSocketTunnelServlet.class);
    
    /**
     * The default, minimum buffer size for instructions.
     */
    private static final int BUFFER_SIZE = 8192;

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {

        // Get tunnel
        final GuacamoleTunnel tunnel;

        try {
            tunnel = doConnect(request);
        }
        catch (GuacamoleException e) {
            logger.error("Error connecting WebSocket tunnel.", e);
            return null;
        }

        // Return new WebSocket which communicates through tunnel
        return new WebSocket.OnTextMessage() {

            @Override
            public void onMessage(String string) {
                GuacamoleWriter writer = tunnel.acquireWriter();

                // Write message received
                try {
                    writer.write(string.toCharArray());
                }
                catch (GuacamoleException e) {
                    logger.debug("Tunnel write failed.", e);
                }

                tunnel.releaseWriter();
            }

            @Override
            public void onOpen(final Connection connection) {

                Thread readThread = new Thread() {

                    @Override
                    public void run() {

                        StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
                        GuacamoleReader reader = tunnel.acquireReader();
                        char[] readMessage;

                        try {
                            while ((readMessage = reader.read()) != null) {

                                // Buffer message
                                buffer.append(readMessage);

                                // Flush if we expect to wait or buffer is getting full
                                if (!reader.available() || buffer.length() >= BUFFER_SIZE) {
                                    connection.sendMessage(buffer.toString());
                                    buffer.setLength(0);
                                }

                            }
                        }
                        catch (IOException e) {
                            logger.debug("Tunnel read failed due to I/O error.", e);
                        }
                        catch (GuacamoleException e) {
                            logger.debug("Tunnel read failed.", e);
                        }

                    }

                };

                readThread.start();

            }

            @Override
            public void onClose(int i, String string) {
                try {
                    tunnel.close();
                }
                catch (GuacamoleException e) {
                    logger.debug("Unable to close WebSocket tunnel.", e);
                }
            }

        };

    }

    protected abstract GuacamoleTunnel doConnect(HttpServletRequest request)
            throws GuacamoleException;

}

