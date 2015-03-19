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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleUnsupportedException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.APIPatch;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.PATCH;
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
     *     A map of the tunnels of all active connections visible to the
     *     current user, where the key of each entry is the tunnel's UUID.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the tunnels.
     */
    @GET
    @Path("/")
    @AuthProviderRESTExposure
    public Map<String, APITunnel> getTunnels(@QueryParam("token") String authToken)
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Retrieve all active tunnels
        Map<String, APITunnel> apiTunnels = new HashMap<String, APITunnel>();
        for (ConnectionRecord record : userContext.getActiveConnections()) {

            // Locate associated tunnel and UUID
            GuacamoleTunnel tunnel = record.getTunnel();
            if (tunnel != null) {
                APITunnel apiTunnel = new APITunnel(record, tunnel.getUUID().toString());
                apiTunnels.put(apiTunnel.getUUID(), apiTunnel);
            }

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
     * @param patches
     *     The tunnel patches to apply for this request.
     *
     * @throws GuacamoleException
     *     If an error occurs while deleting the tunnels.
     */
    @PATCH
    @Path("/")
    @AuthProviderRESTExposure
    public void patchTunnels(@QueryParam("token") String authToken,
            List<APIPatch<String>> patches) throws GuacamoleException {

        // Attempt to get all requested tunnels
        UserContext userContext = authenticationService.getUserContext(authToken);

        // Build list of tunnels to delete
        Collection<String> tunnelUUIDs = new ArrayList<String>(patches.size());
        for (APIPatch<String> patch : patches) {

            // Only remove is supported
            if (patch.getOp() != APIPatch.Operation.remove)
                throw new GuacamoleUnsupportedException("Only the \"remove\" operation is supported when patching tunnels.");

            // Retrieve and validate path
            String path = patch.getPath();
            if (!path.startsWith("/"))
                throw new GuacamoleClientException("Patch paths must start with \"/\".");
            
            // Add UUID
            tunnelUUIDs.add(path.substring(1));
            
        }
        
        // Close each tunnel, if not already closed
        Collection<ConnectionRecord> records = userContext.getActiveConnections(tunnelUUIDs);
        for (ConnectionRecord record : records) {

            GuacamoleTunnel tunnel = record.getTunnel();
            if (tunnel != null && tunnel.isOpen())
                tunnel.close();

        }

    }
    
}
