package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
     */
    @GET
    public List<APIConnectionGroup> getConnectionGroups(@QueryParam("token") String authToken, @QueryParam("parentID") String parentID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
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
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Status.FORBIDDEN, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while listing connection groups.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Gets an individual connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionGroupID The ID of the ConnectionGroup.
     * @return The connection group.
     */
    @GET
    @Path("/{connectionGroupID}")
    public APIConnectionGroup getConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
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
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Status.FORBIDDEN, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while getting connection group.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Deletes an individual connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionGroupID The ID of the ConnectionGroup to delete.
     */
    @DELETE
    @Path("/{connectionGroupID}")
    public void deleteConnectionGroup(@QueryParam("token") String authToken, @PathParam("connectionGroupID") String connectionGroupID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            // Get the connection group directory
            ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
            Directory<String, ConnectionGroup> connectionGroupDirectory =
                    rootGroup.getConnectionGroupDirectory();
            
            // Make sure the connection is there before trying to delete
            if(connectionGroupDirectory.get(connectionGroupID) == null)
                throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided ID.");
            
            // Delete the connection group
            connectionGroupDirectory.remove(connectionGroupID);
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Status.FORBIDDEN, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while deleting connection group.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
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
     */
    @POST
    public String createConnectionGroup(@QueryParam("token") String authToken, 
            @QueryParam("parentID") String parentID, APIConnectionGroup connectionGroup) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
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
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Status.FORBIDDEN, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while creating connection group.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Updates a connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionID The ID of the ConnectionGroup to update.
     * @param connection The connection group to update.
     */
    @POST
    @Path("/{connectionGroupID}")
    public void updateConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID, APIConnectionGroup connectionGroup) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            if(connectionGroup == null)
                throw new GuacamoleClientException("A connection is required for this request.");
        
            // Get the connection directory
            ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
            Directory<String, ConnectionGroup> connectionGroupDirectory =
                    rootGroup.getConnectionGroupDirectory();
            
            // Make sure the connection group is there before trying to update
            if(connectionGroupDirectory.get(connectionGroupID) == null)
                throw new HTTPException(Status.NOT_FOUND, "No ConnectionGroup found with the provided ID.");
            
            // Update the connection group
            connectionGroupDirectory.update(new APIConnectionGroupWrapper(connectionGroup));
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Status.FORBIDDEN, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught updating connection group.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Moves an individual connection group to a different connection group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionID The ID of the ConnectionGroup to move.
     * @param parentID The ID of the ConnectionGroup the connection groups
     *                 belong to. If null, the root connection group will be used.
     */
    @PUT
    @Path("/{connectionGroupID}")
    public void moveConnectionGroup(@QueryParam("token") String authToken, 
            @PathParam("connectionGroupID") String connectionGroupID, @QueryParam("parentID") String parentID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
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
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Status.FORBIDDEN, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught moving connection group.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }

}
