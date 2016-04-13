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

package org.apache.guacamole.rest.tunnel;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.rest.auth.AuthenticationService;
import org.apache.guacamole.tunnel.StreamInterceptingTunnel;

/**
 * A REST Service for retrieving and managing the tunnels of active
 * connections, including any associated objects.
 *
 * @author Michael Jumper
 */
@Path("/tunnels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TunnelRESTService {

    /**
     * The media type to send as the content type of stream contents if no
     * other media type is specified.
     */
    private static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";

    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Returns the UUIDs of all currently-active tunnels associated with the
     * session identified by the given auth token.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @return
     *     A set containing the UUIDs of all currently-active tunnels.
     *
     * @throws GuacamoleException
     *     If the session associated with the given auth token cannot be
     *     retrieved.
     */
    @GET
    public Set<String> getTunnelUUIDs(@QueryParam("token") String authToken)
            throws GuacamoleException {
        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        return session.getTunnels().keySet();
    }

    /**
     * Intercepts and returns the entire contents of a specific stream.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param tunnelUUID
     *     The UUID of the tunnel containing the stream being intercepted.
     *
     * @param streamIndex
     *     The index of the stream to intercept.
     *
     * @param mediaType
     *     The media type (mimetype) of the data within the stream.
     *
     * @param filename
     *     The filename to use for the sake of identifying the data returned.
     *
     * @return
     *     A response through which the entire contents of the intercepted
     *     stream will be sent.
     *
     * @throws GuacamoleException
     *     If the session associated with the given auth token cannot be
     *     retrieved, or if no such tunnel exists.
     */
    @GET
    @Path("/{tunnel}/streams/{index}/{filename}")
    public Response getStreamContents(@QueryParam("token") String authToken,
            @PathParam("tunnel") String tunnelUUID,
            @PathParam("index") final int streamIndex,
            @QueryParam("type") @DefaultValue(DEFAULT_MEDIA_TYPE) String mediaType,
            @PathParam("filename") String filename)
            throws GuacamoleException {

        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        Map<String, StreamInterceptingTunnel> tunnels = session.getTunnels();

        // STUB: For sake of testing, if only one tunnel exists, use that
        if (tunnels.size() == 1)
            tunnelUUID = tunnels.keySet().iterator().next();

        // Pull tunnel with given UUID
        final StreamInterceptingTunnel tunnel = tunnels.get(tunnelUUID);
        if (tunnel == null)
            throw new GuacamoleResourceNotFoundException("No such tunnel.");

        // Intercept all output
        StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException {
                tunnel.interceptStream(streamIndex, output);
            }

        };

        return Response.ok(stream, mediaType).build();

    }

}
