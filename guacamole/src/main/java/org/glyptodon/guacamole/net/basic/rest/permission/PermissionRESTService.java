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

package org.glyptodon.guacamole.net.basic.rest.permission;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.basic.rest.APIPatch;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;
import org.glyptodon.guacamole.net.basic.rest.PATCH;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for handling connection CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PermissionRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(PermissionRESTService.class);
    
    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;
    
    /**
     * A service for managing the REST endpoint APIPermission objects. 
     */
    @Inject
    private PermissionService permissionService;
    
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
    @Path("/{userID}")
    @AuthProviderRESTExposure
    public List<APIPermission> getPermissions(@QueryParam("token") String authToken, @PathParam("userID") String userID) 
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Get the user
        User user = userContext.getUserDirectory().get(userID);
        if (user == null)
            throw new HTTPException(Status.NOT_FOUND, "User not found with the provided userID.");

        return permissionService.convertPermissionList(user.getPermissions());

    }
    
    /**
     * Applies a given list of permission patches.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param patches   The permission patches to apply for this request.
     * @throws GuacamoleException If a problem is encountered while removing the permission.
     */
    @PATCH
    @AuthProviderRESTExposure
    public void patchPermissions(@QueryParam("token") String authToken, 
            List<APIPatch<APIPermission>> patches) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the user directory
        Directory<String, User> userDirectory = userContext.getUserDirectory();
        
        // All users who have had permissions added or removed
        Map<String, User> modifiedUsers = new HashMap<String, User>();
        
        for (APIPatch<APIPermission> patch : patches) {

            String userID = patch.getPath();
            Permission permission = patch.getValue().toPermission();
            
            // See if we've already modified this user in this request
            User user = modifiedUsers.get(userID);
            if (user == null)
                user = userDirectory.get(userID);
            
            if (user == null)
                throw new HTTPException(Status.NOT_FOUND, "User not found with userID " + userID + ".");
            
            // Only the add and remove operations are supported for permissions
            switch(patch.getOp()) {
                case add:
                    user.addPermission(permission);
                    modifiedUsers.put(userID, user);
                    break;
                case remove:
                    user.removePermission(permission);
                    modifiedUsers.put(userID, user);
                    break;
            }

        }
        
        // Save the permission changes for all modified users
        for (User user : modifiedUsers.values())
            userDirectory.update(user);

    }

}
