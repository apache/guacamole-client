package org.glyptodon.guacamole.net.basic.rest.user;

import com.google.inject.Inject;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


/**
 * A REST Service for handling user CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/api/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserRESTService.class);
    
    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;
    
    /**
     * A service for managing the REST endpoint APIPermission objects. 
     */
    @Inject
    private UserService userService;
    
    /**
     * Gets a list of users in the system.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @return The user list.
     */
    @GET
    public List<APIUser> getUsers(@QueryParam("token") String authToken) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            // Get the directory
            Directory<String, User> userDirectory = userContext.getUserDirectory();
            
            // Convert and return the user directory listing
            return userService.convertUserList(userDirectory);
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Response.Status.UNAUTHORIZED, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Response.Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while listing permissions.", e);
            throw new HTTPException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Gets an individual user.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @return user The user.
     */
    @GET
    @Path("/{userID}")
    public APIUser getUser(@QueryParam("token") String authToken, @PathParam("userID") String userID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            // Get the directory
            Directory<String, User> userDirectory = userContext.getUserDirectory();
            
            // Get the user
            User user = userDirectory.get(userID);
            
            if(user == null)
                throw new HTTPException(Response.Status.NOT_FOUND, "User not found with the provided userID.");
            
            // Return the user
            return new APIUser(user);
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Response.Status.UNAUTHORIZED, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Response.Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while listing permissions.", e);
            throw new HTTPException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Creates a new user and returns the username.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param user The new user to create.
     */
    @POST
    public String createUser(@QueryParam("token") String authToken, APIUser user) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            // Get the directory
            Directory<String, User> userDirectory = userContext.getUserDirectory();
            
            // Create the user
            userDirectory.add(new APIUserWrapper(user));
            
            return user.getUsername();
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Response.Status.UNAUTHORIZED, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Response.Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while listing permissions.", e);
            throw new HTTPException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Updates an individual existing user.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param userID The unique identifier of the user to update.
     * @param user The updated user.
     */
    @POST
    @Path("/{userID}")
    public void updateUser(@QueryParam("token") String authToken, @PathParam("userID") String userID, APIUser user) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            // Get the directory
            Directory<String, User> userDirectory = userContext.getUserDirectory();
            
            if(!user.getUsername().equals(userID))
                throw new HTTPException(Response.Status.BAD_REQUEST, "Username does not match provided userID.");
            
            // Get the user
            User existingUser = userDirectory.get(userID);
            
            if(existingUser == null)
                throw new HTTPException(Response.Status.NOT_FOUND, "User not found with the provided userID.");
            
            /*
             * Update the user with the permission set from the existing user
             * since the user REST endpoints do not expose permissions
             */
            userDirectory.update(new APIUserWrapper(user, existingUser.getPermissions()));
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Response.Status.UNAUTHORIZED, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Response.Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while listing permissions.", e);
            throw new HTTPException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
    
    /**
     * Deletes an individual existing user.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param userID The unique identifier of the user to delete.
     */
    @DELETE
    @Path("/{userID}")
    public void deleteUser(@QueryParam("token") String authToken, @PathParam("userID") String userID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            // Get the directory
            Directory<String, User> userDirectory = userContext.getUserDirectory();
            
            // Get the user
            User existingUser = userDirectory.get(userID);
            
            if(existingUser == null)
                throw new HTTPException(Response.Status.NOT_FOUND, "User not found with the provided userID.");
            
            // Delete the user
            userDirectory.remove(userID);
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Response.Status.UNAUTHORIZED, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Response.Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while listing permissions.", e);
            throw new HTTPException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
}
