/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceNotFoundException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.ObjectRetrievalService;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.net.basic.rest.connection.APIConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for handling connection group CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/connectionGroups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectionGroupRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConnectionGroupRESTService.class);
    
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
     * Determines whether the given user has at least one of the given
     * permissions for the connection having the given identifier.
     * 
     * @param user
     *     The user to check permissions for.
     * 
     * @param identifier 
     *     The identifier of the connection to check permissions for.
     * 
     * @param permissions
     *     The permissions to check. The given user must have one or more of
     *     these permissions for this function to return true.
     * 
     * @return
     *     true if the user has at least one of the given permissions.
     */
    private boolean hasConnectionPermission(User user, String identifier,
            List<ObjectPermission.Type> permissions) throws GuacamoleException {

        // Retrieve connection permissions
        ObjectPermissionSet<String> connectionPermissions = user.getConnectionPermissions();
        
        // Determine whether user has at least one of the given permissions
        for (ObjectPermission.Type permission : permissions) {
            if (connectionPermissions.hasPermission(permission, identifier))
                return true;
        }
        
        // None of the given permissions were present
        return false;
        
    }

    /**
     * Determines whether the given user has at least one of the given
     * permissions for the connection group having the given identifier.
     *
     * @param user
     *     The user to check permissions for.
     *
     * @param identifier
     *     The identifier of the connection group to check permissions for.
     *
     * @param permissions
     *     The permissions to check. The given user must have one or more of
     *     these permissions for this function to return true.
     *
     * @return
     *     true if the user has at least one of the given permissions.
     */
    private boolean hasConnectionGroupPermission(User user, String identifier,
            List<ObjectPermission.Type> permissions) throws GuacamoleException {

        // Retrieve connection group permissions
        ObjectPermissionSet<String> connectionGroupPermissions = user.getConnectionGroupPermissions();
        
        // Determine whether user has at least one of the given permissions
        for (ObjectPermission.Type permission : permissions) {
            if (connectionGroupPermissions.hasPermission(permission, identifier))
                return true;
        }

        // None of the given permissions were present
        return false;

    }

    /**
     * Retrieves the given connection group from the user context, including
     * all descendant connections and groups if requested.
     *
     * @param userContext
     *     The user context from which to retrieve the connection group.
     *
     * @param identifier
     *     The unique identifier of the connection group to retrieve.
     *
     * @param includeDescendants
     *     Whether the descendant connections and groups of the given
     *     connection group should also be retrieved.
     * 
     * @param permissions
     *     The set of permissions to filter with. A user must have one or more
     *     of these permissions for a connection to appear in the result. 
     *     If null, no filtering will be performed.
     *
     * @return
     *     The requested connection group, or null if no such connection group
     *     exists.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the requested connection group
     *     or any of its descendants.
     */
    private APIConnectionGroup retrieveConnectionGroup(UserContext userContext,
            String identifier, boolean includeDescendants, List<ObjectPermission.Type> permissions)
            throws GuacamoleException {

        User self = userContext.self();
        
        // An admin user has access to any connection or connection group
        SystemPermissionSet systemPermissions = self.getSystemPermissions();
        boolean isAdmin = systemPermissions.hasPermission(SystemPermission.Type.ADMINISTER);

        // Retrieve specified connection group
        ConnectionGroup connectionGroup;
        try {
            connectionGroup = retrievalService.retrieveConnectionGroup(userContext, identifier);
        }
        catch (GuacamoleResourceNotFoundException e) {
            return null;
        }

        // Wrap queried connection group
        APIConnectionGroup apiConnectionGroup = new APIConnectionGroup(connectionGroup);

        // Recursively query all descendants if necessary, only querying the
        // descendants of balancing groups if we have admin permission on that
        // group
        if (includeDescendants
            && (connectionGroup.getType() != ConnectionGroup.Type.BALANCING
                || isAdmin
                || hasConnectionGroupPermission(self, identifier,
                        Collections.singletonList(ObjectPermission.Type.ADMINISTER)))) {

            // Query all child connections
            Collection<APIConnection> apiConnections = new ArrayList<APIConnection>();
            Directory<String, Connection> connectionDirectory = connectionGroup.getConnectionDirectory();

            for (String childIdentifier : connectionDirectory.getIdentifiers()) {

                // Pull current connection - silently ignore if connection was removed prior to read
                Connection childConnection = connectionDirectory.get(childIdentifier);
                if (childConnection == null)
                    continue;

                // Filter based on permission, if requested
                if (isAdmin || permissions == null || hasConnectionPermission(self, childIdentifier, permissions))
                    apiConnections.add(new APIConnection(childConnection));

            }
            
            // Associate child connections with current connection group
            apiConnectionGroup.setChildConnections(apiConnections);

            // Query all child connection groups
            Collection<APIConnectionGroup> apiConnectionGroups = new ArrayList<APIConnectionGroup>();
            Directory<String, ConnectionGroup> groupDirectory = connectionGroup.getConnectionGroupDirectory();

            for (String childIdentifier : groupDirectory.getIdentifiers()) {

                // Pull current connection group - silently ignore if connection group was removed prior to read
                APIConnectionGroup childConnectionGroup = retrieveConnectionGroup(userContext, childIdentifier, true, permissions);
                if (childConnectionGroup == null)
                    continue;

                apiConnectionGroups.add(childConnectionGroup);

            }
            
            // Associate child groups with current connection group
            apiConnectionGroup.setChildConnectionGroups(apiConnectionGroups);

        }
        
        // Return the connectiion group
        return apiConnectionGroup;

    }

    /**
     * Gets an individual connection group.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     * 
     * @param connectionGroupID
     *     The ID of the connection group to retrieve.
     * 
     * @return
     *     The connection group, without any descendants.
     *
     * @throws GuacamoleException
     *     If a problem is encountered while retrieving the connection group.
     */
    @GET
    @Path("/{connectionGroupID}")
    @AuthProviderRESTExposure
    public APIConnectionGroup getConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Retrieve requested connection group only
        APIConnectionGroup connectionGroup = retrieveConnectionGroup(userContext, connectionGroupID, false, null);
        if (connectionGroup == null)
            throw new GuacamoleResourceNotFoundException("No such connection group: \"" + connectionGroupID + "\"");

        return connectionGroup;

    }

    /**
     * Gets an individual connection group and all children.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionGroupID
     *     The ID of the connection group to retrieve.
     *
     * @param permissions
     *     If specified and non-empty, limit the returned list to only those
     *     connections for which the current user has any of the given
     *     permissions. Otherwise, all visible connections are returned.
     *     Connection groups are unaffected by this parameter.
     * 
     * @return
     *     The requested connection group, including all descendants.
     *
     * @throws GuacamoleException
     *     If a problem is encountered while retrieving the connection group or
     *     its descendants.
     */
    @GET
    @Path("/{connectionGroupID}/tree")
    @AuthProviderRESTExposure
    public APIConnectionGroup getConnectionGroupTree(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID,
            @QueryParam("permission") List<ObjectPermission.Type> permissions)
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Do not filter on permissions if no permissions are specified
        if (permissions != null && permissions.isEmpty())
            permissions = null;
        
        // Retrieve requested connection group and all descendants
        APIConnectionGroup connectionGroup = retrieveConnectionGroup(userContext, connectionGroupID, true, permissions);
        if (connectionGroup == null)
            throw new GuacamoleResourceNotFoundException("No such connection group: \"" + connectionGroupID + "\"");

        return connectionGroup;

    }

    /**
     * Deletes an individual connection group.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionGroupID
     *     The identifier of the connection group to delete.
     *
     * @throws GuacamoleException
     *     If an error occurs while deleting the connection group.
     */
    @DELETE
    @Path("/{connectionGroupID}")
    @AuthProviderRESTExposure
    public void deleteConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the connection group directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, ConnectionGroup> connectionGroupDirectory =
                rootGroup.getConnectionGroupDirectory();

        // Delete the connection group
        connectionGroupDirectory.remove(connectionGroupID);

    }
    
    /**
     * Creates a new connection group and returns the identifier of the new connection group.
     * If a parentID is provided, the connection group will be created in the
     * connection group with the parentID. Otherwise, the root connection group
     * will be used.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionGroup
     *     The connection group to create.
     * 
     * @return
     *     The identifier of the new connection group.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the connection group.
     */
    @POST
    @AuthProviderRESTExposure
    public String createConnectionGroup(@QueryParam("token") String authToken,
            APIConnectionGroup connectionGroup) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Validate that connection group data was provided
        if (connectionGroup == null)
            throw new GuacamoleClientException("Connection group JSON must be submitted when creating connections groups.");

        // Retrieve parent group
        String parentID = connectionGroup.getParentIdentifier();
        ConnectionGroup parentConnectionGroup = retrievalService.retrieveConnectionGroup(userContext, parentID);

        // Add the new connection group
        Directory<String, ConnectionGroup> connectionGroupDirectory = parentConnectionGroup.getConnectionGroupDirectory();
        connectionGroupDirectory.add(new APIConnectionGroupWrapper(connectionGroup));

        // Return the new connection group identifier
        return connectionGroup.getIdentifier();

    }
    
    /**
     * Updates a connection group. If the parent identifier of the
     * connection group is changed, the connection group will also be moved to
     * the new parent group.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionGroupID
     *     The identifier of the existing connection group to update.
     *
     * @param connectionGroup
     *     The data to update the existing connection group with.
     *
     * @throws GuacamoleException
     *     If an error occurs while updating the connection group.
     */
    @PUT
    @Path("/{connectionGroupID}")
    @AuthProviderRESTExposure
    public void updateConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID, APIConnectionGroup connectionGroup) 
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Validate that connection group data was provided
        if (connectionGroup == null)
            throw new GuacamoleClientException("Connection group JSON must be submitted when updating connection groups.");

        // Get the connection group directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, ConnectionGroup> connectionGroupDirectory =
                rootGroup.getConnectionGroupDirectory();

        // Retrieve connection group to update
        ConnectionGroup existingConnectionGroup = connectionGroupDirectory.get(connectionGroupID);
        
        // Update the connection group
        existingConnectionGroup.setName(connectionGroup.getName());
        existingConnectionGroup.setType(connectionGroup.getType());
        connectionGroupDirectory.update(existingConnectionGroup);

        // Get old and new parents
        String oldParentIdentifier = existingConnectionGroup.getParentIdentifier();
        ConnectionGroup updatedParentGroup = retrievalService.retrieveConnectionGroup(userContext, connectionGroup.getParentIdentifier());

        // Update connection group parent, if changed
        if (    (oldParentIdentifier != null && !oldParentIdentifier.equals(updatedParentGroup.getIdentifier()))
             || (oldParentIdentifier == null && updatedParentGroup.getIdentifier() != null))
            connectionGroupDirectory.move(connectionGroupID, updatedParentGroup.getConnectionGroupDirectory());

    }
    
}
