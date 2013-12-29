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

package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

import com.google.inject.Inject;
import java.util.ArrayList;
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
import javax.ws.rs.core.Response.Status;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for handling connection group CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/api/connectionGroup")
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
     * A service for managing the REST endpoint APIConnection objects. 
     */
    @Inject
    private ConnectionGroupService connectionGroupService;
    
    /**
     * Gets a list of connection groups with the given ConnectionGroup parentID.
     * If no parentID is provided, returns the connection groups from the root group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param parentID The ID of the ConnectionGroup the connection groups
     *                 belong to. If null, the root connection group will be used.
     * @return The connection list.
     * @throws GuacamoleException If a problem is encountered while listing connection groups.
     */
    @GET
    @AuthProviderRESTExposure
    public List<APIConnectionGroup> getConnectionGroups(@QueryParam("token") String authToken, @QueryParam("parentID") String parentID) 
            throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        // If the parent connection group is passed in, try to find it.
        ConnectionGroup parentConnectionGroup;
        if(parentID == null)
            parentConnectionGroup = userContext.getRootConnectionGroup();
        else {
            ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
            Directory<String, ConnectionGroup> connectionGroupDirectory = rootGroup.getConnectionGroupDirectory();
            parentConnectionGroup = connectionGroupDirectory.get(parentID);
        }

        if(parentConnectionGroup == null)
            throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided parentID.");

        Directory<String, ConnectionGroup> connectionGroupDirectory = 
                parentConnectionGroup.getConnectionGroupDirectory();

        // return the converted connection group list
        return connectionGroupService.convertConnectionGroupList(connectionGroupDirectory);
    }
    
    /**
     * Gets an individual connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionGroupID The ID of the ConnectionGroup.
     * @return The connection group.
     * @throws GuacamoleException If a problem is encountered while retrieving the connection group.
     */
    @GET
    @Path("/{connectionGroupID}")
    @AuthProviderRESTExposure
    public APIConnectionGroup getConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID) throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        // Get the connection group directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, ConnectionGroup> connectionGroupDirectory =
                rootGroup.getConnectionGroupDirectory();

        // Get the connection group
        ConnectionGroup connectionGroup = connectionGroupDirectory.get(connectionGroupID);

        if(connectionGroup == null)
            throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided ID.");

        // Return the connectiion group
        return new APIConnectionGroup(connectionGroup);
    }
    
    /**
     * Deletes an individual connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionGroupID The ID of the ConnectionGroup to delete.
     * @throws GuacamoleException If a problem is encountered while deleting the connection group.
     */
    @DELETE
    @Path("/{connectionGroupID}")
    @AuthProviderRESTExposure
    public void deleteConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID) throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);

        // Get the connection group directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, ConnectionGroup> connectionGroupDirectory =
                rootGroup.getConnectionGroupDirectory();

        // Make sure the connection is there before trying to delete
        if(connectionGroupDirectory.get(connectionGroupID) == null)
            throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided ID.");

        // Delete the connection group
        connectionGroupDirectory.remove(connectionGroupID);
    }
    
    /**
     * Creates a new connection group and returns the identifier of the new connection group.
     * If a parentID is provided, the connection group will be created in the
     * connection group with the parentID. Otherwise, the root connection group
     * will be used.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param parentID The ID of the ConnectionGroup the connection groups
     *                 belong to. If null, the root connection group will be used.
     * @param connection The connection group to create.
     * @return The identifier of the new connection group.
     * @throws GuacamoleException If a problem is encountered while creating the connection group.
     */
    @POST
    @AuthProviderRESTExposure
    public String createConnectionGroup(@QueryParam("token") String authToken, 
            @QueryParam("parentID") String parentID, APIConnectionGroup connectionGroup) throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);

        if(connectionGroup == null)
            throw new GuacamoleClientException("A connection group is required for this request.");

        // If the parent connection group is passed in, try to find it.
        ConnectionGroup parentConnectionGroup;
        if(parentID == null)
            parentConnectionGroup = userContext.getRootConnectionGroup();
        else {
            ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
            Directory<String, ConnectionGroup> connectionGroupDirectory = rootGroup.getConnectionGroupDirectory();
            parentConnectionGroup = connectionGroupDirectory.get(parentID);
        }

        if(parentConnectionGroup == null)
            throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided parentID.");

        Directory<String, ConnectionGroup> connectionGroupDirectory = 
                parentConnectionGroup.getConnectionGroupDirectory();

        // Create the connection group
        connectionGroupDirectory.add(new APIConnectionGroupWrapper(connectionGroup));

        // Return the new connection group identifier
        return connectionGroup.getIdentifier();
    }
    
    /**
     * Updates a connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionID The ID of the ConnectionGroup to update.
     * @param connection The connection group to update.
     * @throws GuacamoleException If a problem is encountered while updating the connection group.
     */
    @POST
    @Path("/{connectionGroupID}")
    @AuthProviderRESTExposure
    public void updateConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID, APIConnectionGroup connectionGroup) 
            throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        if(connectionGroup == null)
            throw new GuacamoleClientException("A connection group is required for this request.");

        // Get the connection directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, ConnectionGroup> connectionGroupDirectory =
                rootGroup.getConnectionGroupDirectory();

        // Make sure the connection group is there before trying to update
        if(connectionGroupDirectory.get(connectionGroupID) == null)
            throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided ID.");

        // Update the connection group
        connectionGroupDirectory.update(new APIConnectionGroupWrapper(connectionGroup));
    }
    
    /**
     * Moves an individual connection group to a different connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionGroupID The ID of the ConnectionGroup to move.
     * @param parentID The ID of the ConnectionGroup the connection group is to be moved to.
     * @throws GuacamoleException If a problem is encountered while moving the connection group.
     */
    @PUT
    @Path("/{connectionGroupID}")
    @AuthProviderRESTExposure
    public void moveConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID, 
            @QueryParam("parentID") String parentID) throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        // Get the connection group directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, ConnectionGroup> connectionGroupDirectory =
                rootGroup.getConnectionGroupDirectory();

        // Find the new parent connection group
        Directory<String, ConnectionGroup> newConnectionGroupDirectory = rootGroup.getConnectionGroupDirectory();
        ConnectionGroup parentConnectionGroup = newConnectionGroupDirectory.get(parentID);

        if(parentConnectionGroup == null)
            throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided parentID.");

        // Move the connection group
        connectionGroupDirectory.move(connectionGroupID, parentConnectionGroup.getConnectionGroupDirectory());
    }

}
