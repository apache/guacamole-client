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

package org.glyptodon.guacamole.net.basic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.glyptodon.guacamole.net.basic.rest.clipboard.ClipboardRESTService;
import java.util.List;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.net.DelegatingGuacamoleTunnel;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.net.event.TunnelCloseEvent;
import org.glyptodon.guacamole.net.event.TunnelConnectEvent;
import org.glyptodon.guacamole.net.event.listener.TunnelCloseListener;
import org.glyptodon.guacamole.net.event.listener.TunnelConnectListener;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that takes a standard request from the Guacamole JavaScript
 * client and produces the corresponding GuacamoleTunnel. The implementation
 * of this utility is specific to the form of request used by the upstream
 * Guacamole web application, and is not necessarily useful to applications
 * that use purely the Guacamole API.
 *
 * @author Michael Jumper
 */
@Singleton
public class TunnelRequestService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(TunnelRequestService.class);

    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Notifies all listeners in the given session that a tunnel has been
     * connected.
     *
     * @param session The session associated with the listeners to be notified.
     * @param tunnel The tunnel being connected.
     * @return true if all listeners are allowing the tunnel to connect,
     *         or if there are no listeners, and false if any listener is
     *         canceling the connection. Note that once one listener cancels,
     *         no other listeners will run.
     * @throws GuacamoleException If any listener throws an error while being
     *                            notified. Note that if any listener throws an
     *                            error, the connect is canceled, and no other
     *                            listeners will run.
     */
    private boolean notifyConnect(GuacamoleSession session, GuacamoleTunnel tunnel)
            throws GuacamoleException {

        // Build event for auth success
        TunnelConnectEvent event = new TunnelConnectEvent(
                session.getUserContext(),
                session.getCredentials(),
                tunnel);

        // Notify all listeners
        for (Object listener : session.getListeners()) {
            if (listener instanceof TunnelConnectListener) {

                // Cancel immediately if hook returns false
                if (!((TunnelConnectListener) listener).tunnelConnected(event))
                    return false;

            }
        }

        return true;

    }

    /**
     * Notifies all listeners in the given session that a tunnel has been
     * closed.
     *
     * @param session The session associated with the listeners to be notified.
     * @param tunnel The tunnel being closed.
     * @return true if all listeners are allowing the tunnel to close,
     *         or if there are no listeners, and false if any listener is
     *         canceling the close. Note that once one listener cancels,
     *         no other listeners will run.
     * @throws GuacamoleException If any listener throws an error while being
     *                            notified. Note that if any listener throws an
     *                            error, the close is canceled, and no other
     *                            listeners will run.
     */
    private boolean notifyClose(GuacamoleSession session, GuacamoleTunnel tunnel)
            throws GuacamoleException {

        // Build event for auth success
        TunnelCloseEvent event = new TunnelCloseEvent(
                session.getUserContext(),
                session.getCredentials(),
                tunnel);

        // Notify all listeners
        for (Object listener : session.getListeners()) {
            if (listener instanceof TunnelCloseListener) {

                // Cancel immediately if hook returns false
                if (!((TunnelCloseListener) listener).tunnelClosed(event))
                    return false;

            }
        }

        return true;

    }

    /**
     * Reads and returns the client information provided within the given
     * request.
     *
     * @param request
     *     The request describing tunnel to create.
     *
     * @return GuacamoleClientInformation
     *     An object containing information about the client sending the tunnel
     *     request.
     */
    protected GuacamoleClientInformation getClientInformation(TunnelRequest request) {
        // Get client information
        GuacamoleClientInformation info = new GuacamoleClientInformation();

        // Set width if provided
        String width  = request.getParameter("width");
        if (width != null)
            info.setOptimalScreenWidth(Integer.parseInt(width));

        // Set height if provided
        String height = request.getParameter("height");
        if (height != null)
            info.setOptimalScreenHeight(Integer.parseInt(height));

        // Set resolution if provided
        String dpi = request.getParameter("dpi");
        if (dpi != null)
            info.setOptimalResolution(Integer.parseInt(dpi));

        // Add audio mimetypes
        List<String> audio_mimetypes = request.getParameterValues("audio");
        if (audio_mimetypes != null)
            info.getAudioMimetypes().addAll(audio_mimetypes);

        // Add video mimetypes
        List<String> video_mimetypes = request.getParameterValues("video");
        if (video_mimetypes != null)
            info.getVideoMimetypes().addAll(video_mimetypes);

        return info;
    }

    /**
     * Creates a new tunnel using which is connected to the connection or
     * connection group identifier by the given ID. Client information
     * is specified in the {@code info} parameter.
     *
     * @param context
     *     The UserContext associated with the user for whom the tunnel is
     *     being created.
     *
     * @param id
     *     The ID of the connection or connection group being connected to. For
     *     connections, this will be of the form "c/IDENTIFIER", where
     *     IDENTIFIER is the connection identifier. For connection groups, this
     *     will be of the form "g/IDENTIFIER", where IDENTIFIER is the
     *     connection group identifier.
     *
     * @param info
     *     Information describing the connected Guacamole client.
     *
     * @return
     *     A new tunnel, connected as required by the request.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the tunnel.
     */
    protected GuacamoleTunnel createConnectedTunnel(UserContext context, String id,
                                                    GuacamoleClientInformation info) throws GuacamoleException {

        // Determine ID type
        TunnelRequest.IdentifierType id_type = TunnelRequest.IdentifierType.getType(id);
        if (id_type == null)
            throw new GuacamoleClientException("Illegal identifier - unknown type.");

        // Remove prefix
        id = id.substring(id_type.PREFIX.length());

        // Create connected tunnel from identifier
        GuacamoleTunnel tunnel;
        switch (id_type) {

            // Connection identifiers
            case CONNECTION: {

                // Get connection directory
                Directory<Connection> directory = context.getConnectionDirectory();

                // Get authorized connection
                Connection connection = directory.get(id);
                if (connection == null) {
                    logger.info("Connection \"{}\" does not exist for user \"{}\".", id, context.self().getIdentifier());
                    throw new GuacamoleSecurityException("Requested connection is not authorized.");
                }

                // Connect tunnel
                tunnel = connection.connect(info);
                logger.info("User \"{}\" successfully connected to \"{}\".", context.self().getIdentifier(), id);
                break;
            }

            // Connection group identifiers
            case CONNECTION_GROUP: {

                // Get connection group directory
                Directory<ConnectionGroup> directory = context.getConnectionGroupDirectory();

                // Get authorized connection group
                ConnectionGroup group = directory.get(id);
                if (group == null) {
                    logger.info("Connection group \"{}\" does not exist for user \"{}\".", id, context.self().getIdentifier());
                    throw new GuacamoleSecurityException("Requested connection group is not authorized.");
                }

                // Connect tunnel
                tunnel = group.connect(info);
                logger.info("User \"{}\" successfully connected to group \"{}\".", context.self().getIdentifier(), id);
                break;
            }

            // Fail if unsupported type
            default:
                throw new GuacamoleClientException("Connection not supported for provided identifier type.");

        }

        return tunnel;

    }

    /**
     * Associates the given tunnel with the given session, returning a wrapped
     * version of the same tunnel which automatically handles closure and
     * removal from the session.
     *
     * @param tunnel
     *     The connected tunnel to wrap and monitor.
     *
     * @param session
     *     The Guacamole session to associate the tunnel with.
     *
     * @return
     *     A new tunnel, associated with the given session, which delegates all
     *     functionality to the given tunnel while monitoring and automatically
     *     handling closure.
     *
     * @throws GuacamoleException
     *     If an error occurs while obtaining the tunnel.
     */
    protected GuacamoleTunnel createAssociatedTunnel(final GuacamoleSession session,
            GuacamoleTunnel tunnel) throws GuacamoleException {

        // Monitor tunnel closure and data
        GuacamoleTunnel monitoredTunnel = new DelegatingGuacamoleTunnel(tunnel) {

            @Override
            public GuacamoleReader acquireReader() {

                // Monitor instructions which pertain to server-side events, if necessary
                try {
                    if (GuacamoleProperties.getProperty(ClipboardRESTService.INTEGRATION_ENABLED, false)) {

                        ClipboardState clipboard = session.getClipboardState();
                        return new MonitoringGuacamoleReader(clipboard, super.acquireReader());

                    }
                }
                catch (GuacamoleException e) {
                    logger.warn("Clipboard integration failed to initialize: {}", e.getMessage());
                    logger.debug("Error setting up clipboard integration.", e);
                }

                // Pass through by default.
                return super.acquireReader();

            }

            @Override
            public void close() throws GuacamoleException {

                // Signal listeners
                if (!notifyClose(session, this))
                    throw new GuacamoleException("Tunnel close canceled by listener.");

                session.removeTunnel(getUUID().toString());

                // Close if no exception due to listener
                super.close();

            }

        };

        // Notify listeners about connection
        if (!notifyConnect(session, monitoredTunnel)) {
            logger.info("Successful connection canceled by hook.");
            return null;
        }

        // Associate tunnel with session
        session.addTunnel(monitoredTunnel);
        return monitoredTunnel;
        
    }

    /**
     * Creates a new tunnel using the parameters and credentials present in
     * the given request.
     *
     * @param request
     *     The request describing the tunnel to create.
     *
     * @return
     *     The created tunnel, or null if the tunnel could not be created.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the tunnel.
     */
    public GuacamoleTunnel createTunnel(TunnelRequest request)
            throws GuacamoleException {

        // Get auth token and session
        final String authToken = request.getParameter("authToken");
        final GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);

        // Get client information and connection ID from request
        final String id = request.getParameter("id");
        final GuacamoleClientInformation info = getClientInformation(request);

        // Create connected tunnel using provided connection ID and client information
        final GuacamoleTunnel tunnel = createConnectedTunnel(session.getUserContext(), id, info);

        // Associate tunnel with session
        return createAssociatedTunnel(session, tunnel);

    }

}
