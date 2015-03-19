/*
 * Copyright (C) 2015 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic.rest.tunnel;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for retrieving and managing the tunnels of active connections.
 * 
 * @author Michael Jumper
 */
@Path("/tunnels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TunnelRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TunnelRESTService.class);

    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Retrieves the tunnels of all active connections visible to the current
     * user.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @return
     *     The tunnels of all active connections visible to the current user.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the tunnels.
     */
    @GET
    @Path("/")
    @AuthProviderRESTExposure
    public List<APITunnel> getTunnels(@QueryParam("token") String authToken)
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Retrieve all active tunnels
        List<APITunnel> apiTunnels = new ArrayList<APITunnel>();
        for (ConnectionRecord record : userContext.getActiveConnections()) {

            // Locate associated tunnel and UUID
            GuacamoleTunnel tunnel = record.getTunnel();
            if (tunnel != null)
                apiTunnels.add(new APITunnel(record, tunnel.getUUID().toString()));

        }

        return apiTunnels;

    }

    /**
     * Deletes the tunnels having the given UUIDs, effectively closing the
     * tunnels and killing the associated connections.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param tunnelUUIDs
     *     The UUIDs associated with the tunnels being deleted.
     *
     * @throws GuacamoleException
     *     If an error occurs while deleting the tunnels.
     */
    @DELETE
    @Path("/")
    @AuthProviderRESTExposure
    public void deleteTunnels(@QueryParam("token") String authToken,
            @QueryParam("tunnelUUID") Collection<String> tunnelUUIDs) 
            throws GuacamoleException {

        // Attempt to get all requested tunnels
        UserContext userContext = authenticationService.getUserContext(authToken);
        Collection<ConnectionRecord> records = userContext.getActiveConnections(tunnelUUIDs);

        // Close each tunnel, if not already closed
        for (ConnectionRecord record : records) {

            GuacamoleTunnel tunnel = record.getTunnel();
            if (tunnel != null && tunnel.isOpen())
                tunnel.close();

        }

    }
    
}
