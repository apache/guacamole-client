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

package org.glyptodon.guacamole.net.basic.rest.activeconnection;

import com.google.inject.Inject;
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
import org.glyptodon.guacamole.net.auth.ActiveConnection;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
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
@Path("/activeConnections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActiveConnectionRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ActiveConnectionRESTService.class);

    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Gets a list of active connections in the system, filtering the returned
     * list by the given permissions, if specified.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param permissions
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for a user to appear in the result. 
     *     If null, no filtering will be performed.
     * 
     * @return
     *     A list of all active connections. If a permission was specified,
     *     this list will contain only those active connections for which the
     *     current user has that permission.
     * 
     * @throws GuacamoleException
     *     If an error is encountered while retrieving active connections.
     */
    @GET
    @AuthProviderRESTExposure
    public Map<String, APIActiveConnection> getActiveConnections(@QueryParam("token") String authToken,
            @QueryParam("permission") List<ObjectPermission.Type> permissions)
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        User self = userContext.self();
        
        // Do not filter on permissions if no permissions are specified
        if (permissions != null && permissions.isEmpty())
            permissions = null;

        // An admin user has access to any user
        SystemPermissionSet systemPermissions = self.getSystemPermissions();
        boolean isAdmin = systemPermissions.hasPermission(SystemPermission.Type.ADMINISTER);

        // Get the directory
        Directory<ActiveConnection> activeConnectionDirectory = userContext.getActiveConnectionDirectory();

        // Filter users, if requested
        Collection<String> activeConnectionIdentifiers = activeConnectionDirectory.getIdentifiers();
        if (!isAdmin && permissions != null) {
            ObjectPermissionSet activeConnectionPermissions = self.getActiveConnectionPermissions();
            activeConnectionIdentifiers = activeConnectionPermissions.getAccessibleObjects(permissions, activeConnectionIdentifiers);
        }
            
        // Retrieve all active connections , converting to API active connections
        Map<String, APIActiveConnection> apiActiveConnections = new HashMap<String, APIActiveConnection>();
        for (ActiveConnection activeConnection : activeConnectionDirectory.getAll(activeConnectionIdentifiers))
            apiActiveConnections.put(activeConnection.getIdentifier(), new APIActiveConnection(activeConnection));

        return apiActiveConnections;

    }

    /**
     * Applies the given active connection patches. This operation currently
     * only supports deletion of active connections through the "remove" patch
     * operation. Deleting an active connection effectively kills the
     * connection. The path of each patch operation is of the form "/ID"
     * where ID is the identifier of the active connection being modified.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param patches
     *     The active connection patches to apply for this request.
     *
     * @throws GuacamoleException
     *     If an error occurs while deleting the active connections.
     */
    @PATCH
    @Path("/")
    @AuthProviderRESTExposure
    public void patchTunnels(@QueryParam("token") String authToken,
            List<APIPatch<String>> patches) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Get the directory
        Directory<ActiveConnection> activeConnectionDirectory = userContext.getActiveConnectionDirectory();

        // Close each connection listed for removal
        for (APIPatch<String> patch : patches) {

            // Only remove is supported
            if (patch.getOp() != APIPatch.Operation.remove)
                throw new GuacamoleUnsupportedException("Only the \"remove\" operation is supported when patching active connections.");

            // Retrieve and validate path
            String path = patch.getPath();
            if (!path.startsWith("/"))
                throw new GuacamoleClientException("Patch paths must start with \"/\".");

            // Close connection 
            activeConnectionDirectory.remove(path.substring(1));
            
        }
        
    }
    
}
