package org.glyptodon.guacamole.net.basic.websocket.jetty;

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
import javax.servlet.http.HttpServletRequest;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.io.GuacamoleWriter;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketServlet;

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

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {

        // Get tunnel
        final GuacamoleTunnel tunnel;

        try {
            tunnel = doConnect(request);
        }
        catch (GuacamoleException e) {
            return null; // FIXME: Can throw exception?
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
                    // FIXME: Handle exception
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
                            // FIXME: Handle exception
                        }
                        catch (GuacamoleException e) {
                            // FIXME: Handle exception
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
                    // FIXME: Handle exception
                }
            }

        };

    }

    protected abstract GuacamoleTunnel doConnect(HttpServletRequest request)
            throws GuacamoleException;

}

