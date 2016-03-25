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

package org.apache.guacamole.rest.activeconnection;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.rest.APIPatch;
import org.apache.guacamole.rest.ObjectRetrievalService;
import org.apache.guacamole.rest.PATCH;
import org.apache.guacamole.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for retrieving and managing the tunnels of active connections.
 * 
 * @author Michael Jumper
 */
@Path("/data/{dataSource}/activeConnections")
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
     * Service for convenient retrieval of objects.
     */
    @Inject
    private ObjectRetrievalService retrievalService;

    /**
     * Gets a list of active connections in the system, filtering the returned
     * list by the given permissions, if specified.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext containing the active connections to be retrieved.
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
    public Map<String, APIActiveConnection> getActiveConnections(@QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier,
            @QueryParam("permission") List<ObjectPermission.Type> permissions)
            throws GuacamoleException {

        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);
        User self = userContext.self();
        
        // Do not filter on permissions if no permissions are specified
        if (permissions != null && permissions.isEmpty())
            permissions = null;

        // An admin user has access to any connection
        SystemPermissionSet systemPermissions = self.getSystemPermissions();
        boolean isAdmin = systemPermissions.hasPermission(SystemPermission.Type.ADMINISTER);

        // Get the directory
        Directory<ActiveConnection> activeConnectionDirectory = userContext.getActiveConnectionDirectory();

        // Filter connections, if requested
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
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext containing the active connections to be deleted.
     *
     * @param patches
     *     The active connection patches to apply for this request.
     *
     * @throws GuacamoleException
     *     If an error occurs while deleting the active connections.
     */
    @PATCH
    public void patchTunnels(@QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier,
            List<APIPatch<String>> patches) throws GuacamoleException {

        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);

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
