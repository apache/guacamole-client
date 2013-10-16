package org.glyptodon.guacamole.net.basic.websocket.tomcat;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.CharBuffer;
import javax.servlet.http.HttpServletRequest;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.io.GuacamoleWriter;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A WebSocketServlet partial re-implementation of GuacamoleTunnelServlet.
 *
 * @author Michael Jumper
 */
public abstract class GuacamoleWebSocketTunnelServlet extends WebSocketServlet {

    /**
     * The default, minimum buffer size for instructions.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Logger for this class.
     */
    private Logger logger = LoggerFactory.getLogger(GuacamoleWebSocketTunnelServlet.class);

    @Override
    public StreamInbound createWebSocketInbound(String protocol, HttpServletRequest request) {

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
        return new StreamInbound() {

            @Override
            protected void onTextData(Reader reader) throws IOException {

                GuacamoleWriter writer = tunnel.acquireWriter();

                // Write all available data
                try {

                    char[] buffer = new char[BUFFER_SIZE];

                    int num_read;
                    while ((num_read = reader.read(buffer)) > 0)
                        writer.write(buffer, 0, num_read);

                }
                catch (GuacamoleException e) {
                    logger.debug("Tunnel write failed.", e);
                }

                tunnel.releaseWriter();
            }

            @Override
            public void onOpen(final WsOutbound outbound) {

                Thread readThread = new Thread() {

                    @Override
                    public void run() {

                        CharBuffer charBuffer = CharBuffer.allocate(BUFFER_SIZE);
                        StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
                        GuacamoleReader reader = tunnel.acquireReader();
                        char[] readMessage;

                        try {
                            while ((readMessage = reader.read()) != null) {

                                // Buffer message
                                buffer.append(readMessage);

                                // Flush if we expect to wait or buffer is getting full
                                if (!reader.available() || buffer.length() >= BUFFER_SIZE) {

                                    // Reallocate buffer if necessary
                                    if (buffer.length() > charBuffer.length())
                                        charBuffer = CharBuffer.allocate(buffer.length());
                                    else
                                        charBuffer.clear();

                                    charBuffer.put(buffer.toString().toCharArray());
                                    charBuffer.flip();

                                    outbound.writeTextMessage(charBuffer);
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
            public void onClose(int i) {
                try {
                    tunnel.close();
                }
                catch (GuacamoleException e) {
                    logger.debug("Unable to close WebSocket tunnel.", e);
                }
            }

            @Override
            protected void onBinaryData(InputStream in) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        };

    }

    protected abstract GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException;

}

