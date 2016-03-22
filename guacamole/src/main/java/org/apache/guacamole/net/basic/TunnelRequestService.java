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

package org.apache.guacamole.net.basic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.net.DelegatingGuacamoleTunnel;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.basic.rest.ObjectRetrievalService;
import org.apache.guacamole.net.basic.rest.auth.AuthenticationService;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
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
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(TunnelRequestService.class);

    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Service for convenient retrieval of objects.
     */
    @Inject
    private ObjectRetrievalService retrievalService;

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
     *
     * @throws GuacamoleException
     *     If the parameters of the tunnel request are invalid.
     */
    protected GuacamoleClientInformation getClientInformation(TunnelRequest request)
        throws GuacamoleException {

        // Get client information
        GuacamoleClientInformation info = new GuacamoleClientInformation();

        // Set width if provided
        Integer width = request.getWidth();
        if (width != null)
            info.setOptimalScreenWidth(width);

        // Set height if provided
        Integer height = request.getHeight();
        if (height != null)
            info.setOptimalScreenHeight(height);

        // Set resolution if provided
        Integer dpi = request.getDPI();
        if (dpi != null)
            info.setOptimalResolution(dpi);

        // Add audio mimetypes
        List<String> audioMimetypes = request.getAudioMimetypes();
        if (audioMimetypes != null)
            info.getAudioMimetypes().addAll(audioMimetypes);

        // Add video mimetypes
        List<String> videoMimetypes = request.getVideoMimetypes();
        if (videoMimetypes != null)
            info.getVideoMimetypes().addAll(videoMimetypes);

        // Add image mimetypes
        List<String> imageMimetypes = request.getImageMimetypes();
        if (imageMimetypes != null)
            info.getImageMimetypes().addAll(imageMimetypes);

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
     * @param type
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
            final TunnelRequest.Type type, String id,
            GuacamoleClientInformation info)
            throws GuacamoleException {

        // Create connected tunnel from identifier
        GuacamoleTunnel tunnel = null;
        switch (type) {

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
     * @param authToken
     *     The authentication token associated with the given session. If
     *     provided, this token will be automatically invalidated (and the
     *     corresponding session destroyed) if tunnel errors imply that the
     *     user is no longer authorized.
     *
     * @param session
     *     The Guacamole session to associate the tunnel with.
     *
     * @param type
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
    protected GuacamoleTunnel createAssociatedTunnel(GuacamoleTunnel tunnel,
            final String authToken,  final GuacamoleSession session,
            final TunnelRequest.Type type, final String id)
            throws GuacamoleException {

        // Monitor tunnel closure and data
        GuacamoleTunnel monitoredTunnel = new DelegatingGuacamoleTunnel(tunnel) {

            /**
             * The time the connection began, measured in milliseconds since
             * midnight, January 1, 1970 UTC.
             */
            private final long connectionStartTime = System.currentTimeMillis();

            @Override
            public void close() throws GuacamoleException {

                long connectionEndTime = System.currentTimeMillis();
                long duration = connectionEndTime - connectionStartTime;

                // Log closure
                switch (type) {

                    // Connection identifiers
                    case CONNECTION:
                        logger.info("User \"{}\" disconnected from connection \"{}\". Duration: {} milliseconds",
                                session.getAuthenticatedUser().getIdentifier(), id, duration);
                        break;

                    // Connection group identifiers
                    case CONNECTION_GROUP:
                        logger.info("User \"{}\" disconnected from connection group \"{}\". Duration: {} milliseconds",
                                session.getAuthenticatedUser().getIdentifier(), id, duration);
                        break;

                    // Type is guaranteed to be one of the above
                    default:
                        assert(false);

                }

                try {

                    // Close and clean up tunnel
                    session.removeTunnel(getUUID().toString());
                    super.close();

                }

                // Ensure any associated session is invalidated if unauthorized
                catch (GuacamoleUnauthorizedException e) {

                    // If there is an associated auth token, invalidate it
                    if (authenticationService.destroyGuacamoleSession(authToken))
                        logger.debug("Implicitly invalidated session for token \"{}\".", authToken);

                    // Continue with exception processing
                    throw e;

                }

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

        // Parse request parameters
        String authToken                = request.getAuthenticationToken();
        String id                       = request.getIdentifier();
        TunnelRequest.Type type         = request.getType();
        String authProviderIdentifier   = request.getAuthenticationProviderIdentifier();
        GuacamoleClientInformation info = getClientInformation(request);

        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);

        try {

            // Create connected tunnel using provided connection ID and client information
            GuacamoleTunnel tunnel = createConnectedTunnel(userContext, type, id, info);

            // Associate tunnel with session
            return createAssociatedTunnel(tunnel, authToken, session, type, id);

        }

        // Ensure any associated session is invalidated if unauthorized
        catch (GuacamoleUnauthorizedException e) {

            // If there is an associated auth token, invalidate it
            if (authenticationService.destroyGuacamoleSession(authToken))
                logger.debug("Implicitly invalidated session for token \"{}\".", authToken);

            // Continue with exception processing
            throw e;

        }

    }

}
