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

package org.glyptodon.guacamole.net.basic.rest.user;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import javax.ws.rs.core.Response.Status;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.permission.ConnectionGroupPermission;
import org.glyptodon.guacamole.net.auth.permission.ConnectionPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.UserPermission;
import org.glyptodon.guacamole.net.basic.rest.APIPatch;
import static org.glyptodon.guacamole.net.basic.rest.APIPatch.Operation.add;
import static org.glyptodon.guacamole.net.basic.rest.APIPatch.Operation.remove;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;
import org.glyptodon.guacamole.net.basic.rest.PATCH;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.net.basic.rest.permission.APIPermissionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for handling user CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserRESTService.class);

    /**
     * The prefix of any path within an operation of a JSON patch which
     * modifies the permissions of a user regarding a specific connection.
     */
    private static final String CONNECTION_PERMISSION_PATCH_PATH_PREFIX = "/connectionPermissions/";
    
    /**
     * The prefix of any path within an operation of a JSON patch which
     * modifies the permissions of a user regarding a specific connection group.
     */
    private static final String CONNECTION_GROUP_PERMISSION_PATCH_PATH_PREFIX = "/connectionGroupPermissions/";

    /**
     * The prefix of any path within an operation of a JSON patch which
     * modifies the permissions of a user regarding another, specific user.
     */
    private static final String USER_PERMISSION_PATCH_PATH_PREFIX = "/userPermissions/";

    /**
     * The path of any operation within a JSON patch which modifies the
     * permissions of a user regarding the entire system.
     */
    private static final String SYSTEM_PERMISSION_PATCH_PATH = "/systemPermissions";
    
    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;
    
    /**
     * Gets a list of users in the system, filtering the returned list by the
     * given permission, if specified.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param permission
     *     If specified, limit the returned list to only those users for whom
     *     the current user has the given permission. Otherwise, all visible
     *     users are returned.
     * 
     * @return The user list.
     * 
     * @throws GuacamoleException
     *     If an error is encountered while retrieving users.
     */
    @GET
    @AuthProviderRESTExposure
    public List<APIUser> getUsers(@QueryParam("token") String authToken,
            @QueryParam("permission") UserPermission.Type permission)
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        User self = userContext.self();

        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        List<APIUser> users = new ArrayList<APIUser>();

        // Add all users matching the given permission filter
        for (String username : userDirectory.getIdentifiers()) {

            if (permission == null || self.hasPermission(new UserPermission(permission, username)))
                users.add(new APIUser(userDirectory.get(username)));

        }
        
        // Return the user directory listing
        return users;

    }
    
    /**
     * Gets an individual user.
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param userID    The ID of the user to retrieve.
     * @return user The user.
     * @throws GuacamoleException If a problem is encountered while retrieving the user.
     */
    @GET
    @Path("/{userID}")
    @AuthProviderRESTExposure
    public APIUser getUser(@QueryParam("token") String authToken, @PathParam("userID") String userID) 
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        // Get the user
        User user = userDirectory.get(userID);
        if (user == null)
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
     * 
     * @return The username of the newly created user.
     */
    @POST
    @AuthProviderRESTExposure
    public String createUser(@QueryParam("token") String authToken, APIUser user) 
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();
        
        // Randomly set the password if it wasn't provided
        if (user.getPassword() == null)
            user.setPassword(UUID.randomUUID().toString());

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

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        if (!user.getUsername().equals(userID))
            throw new HTTPException(Response.Status.BAD_REQUEST, "Username does not match provided userID.");

        // Get the user
        User existingUser = userDirectory.get(userID);
        if (existingUser == null)
            throw new HTTPException(Response.Status.NOT_FOUND, "User not found with the provided userID.");

        // Do not update the user password if no password was provided
        if (user.getPassword() != null) {
            /*
             * Update the user with the permission set from the existing user
             * since the user REST endpoints do not expose permissions.
             */
            existingUser.setPassword(user.getPassword());
            userDirectory.update(existingUser);
        }

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

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        // Get the user
        User existingUser = userDirectory.get(userID);
        if (existingUser == null)
            throw new HTTPException(Response.Status.NOT_FOUND, "User not found with the provided userID.");

        // Delete the user
        userDirectory.remove(userID);

    }

    /**
     * Gets a list of permissions for the user with the given userID.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param userID The ID of the user to retrieve permissions for.
     * @return The permission list.
     * @throws GuacamoleException If a problem is encountered while listing permissions.
     */
    @GET
    @Path("/{userID}/permissions")
    @AuthProviderRESTExposure
    public APIPermissionSet getPermissions(@QueryParam("token") String authToken, @PathParam("userID") String userID) 
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Get the user
        User user = userContext.getUserDirectory().get(userID);
        if (user == null)
            throw new HTTPException(Status.NOT_FOUND, "User not found with the provided userID.");

        return new APIPermissionSet(user.getPermissions());

    }
    
    /**
     * Applies a given list of permission patches. Each patch specifies either
     * an "add" or a "remove" operation for a permission type, represented by
     * a string. Valid permission types depend on the path of each patch
     * operation, as the path dictates the permission being modified, such as
     * "/connectionPermissions/42" or "/systemPermissions".
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     * 
     * @param userID
     *     The ID of the user to modify the permissions of.
     *
     * @param patches
     *     The permission patches to apply for this request.
     *
     * @throws GuacamoleException
     *     If a problem is encountered while modifying permissions.
     */
    @PATCH
    @Path("/{userID}/permissions")
    @AuthProviderRESTExposure
    public void patchPermissions(@QueryParam("token") String authToken,
            @PathParam("userID") String userID,
            List<APIPatch<String>> patches) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the user directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();

        // Get the user
        User user = userContext.getUserDirectory().get(userID);
        if (user == null)
            throw new HTTPException(Status.NOT_FOUND, "User not found with the provided userID.");

        // Apply all patch operations individually
        for (APIPatch<String> patch : patches) {

            Permission permission;

            String path = patch.getPath();

            // Create connection permission if path has connection prefix
            if (path.startsWith(CONNECTION_PERMISSION_PATCH_PATH_PREFIX)) {

                // Get identifier and type from patch operation
                String identifier = path.substring(CONNECTION_PERMISSION_PATCH_PATH_PREFIX.length());
                ObjectPermission.Type type = ObjectPermission.Type.valueOf(patch.getValue());

                // Create corresponding permission
                permission = new ConnectionPermission(type, identifier);
                
            }

            // Create connection group permission if path has connection group prefix
            else if (path.startsWith(CONNECTION_GROUP_PERMISSION_PATCH_PATH_PREFIX)) {

                // Get identifier and type from patch operation
                String identifier = path.substring(CONNECTION_GROUP_PERMISSION_PATCH_PATH_PREFIX.length());
                ObjectPermission.Type type = ObjectPermission.Type.valueOf(patch.getValue());

                // Create corresponding permission
                permission = new ConnectionGroupPermission(type, identifier);
                
            }

            // Create user permission if path has user prefix
            else if (path.startsWith(USER_PERMISSION_PATCH_PATH_PREFIX)) {

                // Get identifier and type from patch operation
                String identifier = path.substring(USER_PERMISSION_PATCH_PATH_PREFIX.length());
                ObjectPermission.Type type = ObjectPermission.Type.valueOf(patch.getValue());

                // Create corresponding permission
                permission = new UserPermission(type, identifier);
                
            }

            // Create system permission if path is system path
            else if (path.startsWith(SYSTEM_PERMISSION_PATCH_PATH)) {

                // Get identifier and type from patch operation
                SystemPermission.Type type = SystemPermission.Type.valueOf(patch.getValue());

                // Create corresponding permission
                permission = new SystemPermission(type);
                
            }

            // Otherwise, the path is not supported
            else
                throw new HTTPException(Status.BAD_REQUEST, "Unsupported patch path: \"" + path + "\"");

            // Add or remove permission based on operation
            switch (patch.getOp()) {

                // Add permission
                case add:
                    user.addPermission(permission);
                    break;

                // Remove permission
                case remove:
                    user.removePermission(permission);
                    break;

                // Unsupported patch operation
                default:
                    throw new HTTPException(Status.BAD_REQUEST, "Unsupported patch operation: \"" + patch.getOp() + "\"");

            }

        } // end for each patch operation
        
        // Save the permission changes
        userDirectory.update(user);

    }


}
