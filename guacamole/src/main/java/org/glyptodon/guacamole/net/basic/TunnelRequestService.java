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
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.net.DelegatingGuacamoleTunnel;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
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
 * @author Vasily Loginov
 */
@Singleton
public class TunnelRequestService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;
    
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
     * @param idType
     *     The type of object being connected to (connection or group).
     *
     * @param id
     *     The id of the connection or group being connected to.
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
    protected GuacamoleTunnel createConnectedTunnel(UserContext context,
            final TunnelRequest.IdentifierType idType, String id,
            GuacamoleClientInformation info)
            throws GuacamoleException {

        // Create connected tunnel from identifier
        GuacamoleTunnel tunnel = null;
        switch (idType) {

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
                logger.info("User \"{}\" connected to connection \"{}\".", context.self().getIdentifier(), id);
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
                logger.info("User \"{}\" connected to group \"{}\".", context.self().getIdentifier(), id);
                break;
            }

            // Type is guaranteed to be one of the above
            default:
                assert(false);

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
     * @param idType
     *     The type of object being connected to (connection or group).
     *
     * @param id
     *     The id of the connection or group being connected to.
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
            GuacamoleTunnel tunnel, final TunnelRequest.IdentifierType idType,
            final String id) throws GuacamoleException {

        // Monitor tunnel closure and data
        GuacamoleTunnel monitoredTunnel = new DelegatingGuacamoleTunnel(tunnel) {

            /**
             * The time the connection began, measured in milliseconds since
             * midnight, January 1, 1970 UTC.
             */
            private final long connectionStartTime = System.currentTimeMillis();

            @Override
            public GuacamoleReader acquireReader() {

                // Monitor instructions which pertain to server-side events, if necessary
                try {
                    if (environment.getProperty(ClipboardRESTService.INTEGRATION_ENABLED, false)) {

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

                long connectionEndTime = System.currentTimeMillis();
                long duration = connectionEndTime - connectionStartTime;

                // Log closure
                switch (idType) {

                    // Connection identifiers
                    case CONNECTION:
                        logger.info("User \"{}\" disconnected from connection \"{}\". Duration: {} milliseconds",
                                session.getUserContext().self().getIdentifier(), id, duration);
                        break;

                    // Connection group identifiers
                    case CONNECTION_GROUP:
                        logger.info("User \"{}\" disconnected from connection group \"{}\". Duration: {} milliseconds",
                                session.getUserContext().self().getIdentifier(), id, duration);
                        break;

                    // Type is guaranteed to be one of the above
                    default:
                        assert(false);

                }

                // Close and clean up tunnel
                session.removeTunnel(getUUID().toString());
                super.close();

            }

        };

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
        String id = request.getParameter("id");
        final GuacamoleClientInformation info = getClientInformation(request);

        // Determine ID type
        TunnelRequest.IdentifierType idType = TunnelRequest.IdentifierType.getType(id);
        if (idType == null)
            throw new GuacamoleClientException("Illegal identifier - unknown type.");

        // Remove prefix
        id = id.substring(idType.PREFIX.length());

        // Create connected tunnel using provided connection ID and client information
        final GuacamoleTunnel tunnel = createConnectedTunnel(session.getUserContext(), idType, id, info);

        // Associate tunnel with session
        return createAssociatedTunnel(session, tunnel, idType, id);

    }

}
