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
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @throws GuacamoleException If a problem is encountered while listing users.
     */
    @GET
    @AuthProviderRESTExposure
    public List<APIUser> getUsers(@QueryParam("token") String authToken) throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);

        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        // Convert and return the user directory listing
        return userService.convertUserList(userDirectory);
    }
    
    /**
     * Gets an individual user.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @return user The user.
     * @throws GuacamoleException If a problem is encountered while retrieving the user.
     */
    @GET
    @Path("/{userID}")
    @AuthProviderRESTExposure
    public APIUser getUser(@QueryParam("token") String authToken, @PathParam("userID") String userID) 
            throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);

        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        // Get the user
        User user = userDirectory.get(userID);

        if(user == null)
            throw new HTTPException(Response.Status.NOT_FOUND, "User not found with the provided userID.");

        // Return the user
        return new APIUser(user);
    }
    
    /**
     * Creates a new user and returns the username.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param user The new user to create.
     * @throws GuacamoleException If a problem is encountered while creating the user.
     */
    @POST
    @AuthProviderRESTExposure
    public String createUser(@QueryParam("token") String authToken, APIUser user) 
            throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        // Create the user
        userDirectory.add(new APIUserWrapper(user));

        return user.getUsername();
    }
    
    /**
     * Updates an individual existing user.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param userID The unique identifier of the user to update.
     * @param user The updated user.
     * @throws GuacamoleException If a problem is encountered while updating the user.
     */
    @POST
    @Path("/{userID}")
    @AuthProviderRESTExposure
    public void updateUser(@QueryParam("token") String authToken, @PathParam("userID") String userID, APIUser user) 
            throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
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
    }
    
    /**
     * Deletes an individual existing user.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param userID The unique identifier of the user to delete.
     * @throws GuacamoleException If a problem is encountered while deleting the user.
     */
    @DELETE
    @Path("/{userID}")
    @AuthProviderRESTExposure
    public void deleteUser(@QueryParam("token") String authToken, @PathParam("userID") String userID) 
            throws GuacamoleException {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        // Get the user
        User existingUser = userDirectory.get(userID);

        if(existingUser == null)
            throw new HTTPException(Response.Status.NOT_FOUND, "User not found with the provided userID.");

        // Delete the user
        userDirectory.remove(userID);
    }
}
