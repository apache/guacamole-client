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
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.ObjectRetrievalService;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
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

        // Retrieve the requested connection group
        return new APIConnectionGroup(retrievalService.retrieveConnectionGroup(userContext, connectionGroupID));

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

        // Retrieve the requested tree, filtering by the given permissions
        ConnectionGroup treeRoot = retrievalService.retrieveConnectionGroup(userContext, connectionGroupID);
        ConnectionGroupTree tree = new ConnectionGroupTree(treeRoot, permissions);

        // Return tree as a connection group
        return tree.getRootAPIConnectionGroup();

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
        Directory<ConnectionGroup> connectionGroupDirectory =
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
        Directory<ConnectionGroup> connectionGroupDirectory = parentConnectionGroup.getConnectionGroupDirectory();
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
        Directory<ConnectionGroup> connectionGroupDirectory =
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
