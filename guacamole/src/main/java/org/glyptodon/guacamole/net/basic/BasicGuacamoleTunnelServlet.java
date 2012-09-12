package org.glyptodon.guacamole.net.basic;

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
import java.util.Collection;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.io.GuacamoleWriter;
import org.glyptodon.guacamole.net.GuacamoleResourcePipe;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.basic.event.SessionListenerCollection;
import org.glyptodon.guacamole.net.event.TunnelCloseEvent;
import org.glyptodon.guacamole.net.event.TunnelConnectEvent;
import org.glyptodon.guacamole.net.event.listener.TunnelCloseListener;
import org.glyptodon.guacamole.net.event.listener.TunnelConnectListener;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.glyptodon.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.protocol.GuacamoleInstruction;
import org.glyptodon.guacamole.servlet.GuacamoleHTTPTunnelServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects users to a tunnel associated with the authorized configuration
 * having the given ID.
 *
 * @author Michael Jumper
 */
public class BasicGuacamoleTunnelServlet extends AuthenticatingHttpServlet {

    private Logger logger = LoggerFactory.getLogger(BasicGuacamoleTunnelServlet.class);

    @Override
    protected void authenticatedService(
            Map<String, GuacamoleConfiguration> configs,
            HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

        // If authenticated, respond as tunnel
        tunnelServlet.service(request, response);

    }

    /**
     * Notifies all listeners in the given collection that a tunnel has been
     * connected.
     *
     * @param listeners A collection of all listeners that should be notified.
     * @param credentials The credentials associated with the authentication
     *                    request that connected the tunnel.
     * @return true if all listeners are allowing the tunnel to connect,
     *         or if there are no listeners, and false if any listener is
     *         canceling the connection. Note that once one listener cancels,
     *         no other listeners will run.
     * @throws GuacamoleException If any listener throws an error while being
     *                            notified. Note that if any listener throws an
     *                            error, the connect is canceled, and no other
     *                            listeners will run.
     */
    private boolean notifyConnect(Collection listeners,
            Credentials credentials, GuacamoleTunnel tunnel)
            throws GuacamoleException {

        // Build event for auth success
        TunnelConnectEvent event = new TunnelConnectEvent(credentials, tunnel);

        // Notify all listeners
        for (Object listener : listeners) {
            if (listener instanceof TunnelConnectListener) {

                // Cancel immediately if hook returns false
                if (!((TunnelConnectListener) listener).tunnelConnected(event))
                    return false;

            }
        }

        return true;

    }

    /**
     * Notifies all listeners in the given collection that a tunnel has been
     * closed.
     *
     * @param listeners A collection of all listeners that should be notified.
     * @param credentials The credentials associated with the authentication
     *                    request that closed the tunnel.
     * @return true if all listeners are allowing the tunnel to close,
     *         or if there are no listeners, and false if any listener is
     *         canceling the close. Note that once one listener cancels,
     *         no other listeners will run.
     * @throws GuacamoleException If any listener throws an error while being
     *                            notified. Note that if any listener throws an
     *                            error, the close is canceled, and no other
     *                            listeners will run.
     */
    private boolean notifyClose(Collection listeners,
            Credentials credentials, GuacamoleTunnel tunnel)
            throws GuacamoleException {

        // Build event for auth success
        TunnelCloseEvent event = new TunnelCloseEvent(credentials, tunnel);

        // Notify all listeners
        for (Object listener : listeners) {
            if (listener instanceof TunnelCloseListener) {

                // Cancel immediately if hook returns false
                if (!((TunnelCloseListener) listener).tunnelClosed(event))
                    return false;

            }
        }

        return true;

    }

    /**
     * Wrapped GuacamoleHTTPTunnelServlet which will handle all authenticated
     * requests.
     */
    private GuacamoleHTTPTunnelServlet tunnelServlet = new GuacamoleHTTPTunnelServlet() {

        @Override
        protected GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException {

            HttpSession httpSession = request.getSession(true);

            // Get listeners
            final SessionListenerCollection listeners;
            try {
                listeners = new SessionListenerCollection(httpSession);
            }
            catch (GuacamoleException e) {
                logger.error("Failed to retrieve listeners. Authentication canceled.", e);
                throw e;
            }

            // Get ID of connection
            String id = request.getParameter("id");

            // Get credentials
            final Credentials credentials = getCredentials(httpSession);

            // Get authorized configs
            Map<String, GuacamoleConfiguration> configs = getConfigurations(httpSession);

            // If no configs/credentials in session, not authorized
            if (credentials == null || configs == null)
                throw new GuacamoleSecurityException("Cannot connect - user not logged in.");

            // Get authorized config
            GuacamoleConfiguration config = configs.get(id);
            if (config == null) {
                logger.warn("Configuration id={} not found.", id);
                throw new GuacamoleSecurityException("Requested configuration is not authorized.");
            }

            logger.info("Successful connection from {} to \"{}\".", request.getRemoteAddr(), id);

            // Configure and connect socket
            String hostname = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_HOSTNAME);
            int port = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_PORT);

            GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                    new InetGuacamoleSocket(hostname, port),
                    config
            );

            // Associate socket with tunnel
            GuacamoleTunnel tunnel = new GuacamoleTunnel(socket) {

                @Override
                public void close() throws GuacamoleException {

                    // Only close if not canceled
                    if (!notifyClose(listeners, credentials, this))
                        throw new GuacamoleException("Tunnel close canceled by listener.");

                    // Close if no exception due to listener
                    super.close();

                }

            };

            // Notify listeners about connection
            if (!notifyConnect(listeners, credentials, tunnel)) {
                logger.info("Connection canceled by listener.");
                return null;
            }

            return tunnel;

        }

        @Override
        protected void handleClientInstruction(GuacamoleTunnel tunnel,
            GuacamoleInstruction instruction, GuacamoleWriter writer)
            throws GuacamoleException {

            switch (instruction.getOperation()) {

                // Intercept reject
                case REJECT: {

                    // Get resource tunnel
                    GuacamoleResourcePipe resource = tunnel.getResourcePipe(
                            Integer.parseInt(instruction.getArgs()[0]));

                    // Detach resource
                    tunnel.detachResourcePipe(resource);
                    break;

                }

            }
            
            // Pass through all client-to-server instructions
            super.handleClientInstruction(tunnel, instruction, writer);
            
        }

        @Override
        protected void handleServerInstruction(GuacamoleTunnel tunnel,
            GuacamoleInstruction instruction, GuacamoleWriter writer)
            throws GuacamoleException {

            switch (instruction.getOperation()) {

                // Intercept resource instructions, handle on behalf of client.
                case RESOURCE: {

                    // Allocate resource
                    GuacamoleResourcePipe resource = new GuacamoleResourcePipe(
                            Integer.parseInt(instruction.getArgs()[0]));

                    // Attach resource
                    tunnel.attachResourcePipe(resource);

                    // Pass through to client
                    super.handleServerInstruction(tunnel, instruction, writer);
                    break;
                }

                // Intercept data instructions, appending received data to
                // allocated resources.
                case DATA: {

                    // Get resource tunnel
                    GuacamoleResourcePipe resource = tunnel.getResourcePipe(
                            Integer.parseInt(instruction.getArgs()[0]));

                    // Write data
                    resource.write(instruction.getArgs()[1]);
                    break;
                }

                // Intercept end instructions, closing associated resources.
                case END: {

                    // Get resource tunnel
                    GuacamoleResourcePipe resource = tunnel.getResourcePipe(
                            Integer.parseInt(instruction.getArgs()[0]));

                    // End of stream
                    resource.close();
                    break;
                }

                // Pass through all other instructions.
                default:
                    super.handleServerInstruction(tunnel, instruction, writer);
                
            }
            
        }

    };

}

