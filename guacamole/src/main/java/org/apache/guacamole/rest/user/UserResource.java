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

package org.apache.guacamole.rest.user;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.GuacamoleCredentialsException;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.Permission;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.apache.guacamole.rest.APIPatch;
import org.apache.guacamole.rest.PATCH;
import org.apache.guacamole.rest.directory.DirectoryObjectResource;
import org.apache.guacamole.rest.directory.DirectoryObjectTranslator;
import org.apache.guacamole.rest.permission.APIPermissionSet;

/**
 * A REST resource which abstracts the operations available on an existing
 * User.
 *
 * @author Michael Jumper
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource
        extends DirectoryObjectResource<User, APIUser> {

    /**
     * The UserContext associated with the Directory which contains the User
     * exposed by this resource.
     */
    private final UserContext userContext;

    /**
     * The Directory which contains the User object represented by this
     * UserResource.
     */
    private final Directory<User> directory;

    /**
     * The User object represented by this UserResource.
     */
    private final User user;

    /**
     * Creates a new UserResource which exposes the operations and subresources
     * available for the given User.
     *
     * @param userContext
     *     The UserContext associated with the given Directory.
     *
     * @param directory
     *     The Directory which contains the given User.
     *
     * @param user
     *     The User that this UserResource should represent.
     *
     * @param translator
     *     A DirectoryObjectTranslator implementation which handles Users.
     */
    @AssistedInject
    public UserResource(@Assisted UserContext userContext,
            @Assisted Directory<User> directory,
            @Assisted User user,
            DirectoryObjectTranslator<User, APIUser> translator) {
        super(directory, user, translator);
        this.userContext = userContext;
        this.directory = directory;
        this.user = user;
    }

    @Override
    public void updateObject(APIUser modifiedObject) throws GuacamoleException {

        // A user may not use this endpoint to modify himself
        if (userContext.self().getIdentifier().equals(modifiedObject.getUsername()))
            throw new GuacamoleSecurityException("Permission denied.");

        super.updateObject(modifiedObject);

    }

    /**
     * Updates the password for an individual existing user.
     *
     * @param userPasswordUpdate
     *     The object containing the old password for the user, as well as the
     *     new password to set for that user.
     *
     * @param request
     *     The HttpServletRequest associated with the password update attempt.
     *
     * @throws GuacamoleException
     *     If an error occurs while updating the user's password.
     */
    @PUT
    @Path("password")
    public void updatePassword(APIUserPasswordUpdate userPasswordUpdate,
            @Context HttpServletRequest request) throws GuacamoleException {

        // Build credentials
        Credentials credentials = new Credentials();
        credentials.setUsername(user.getIdentifier());
        credentials.setPassword(userPasswordUpdate.getOldPassword());
        credentials.setRequest(request);
        credentials.setSession(request.getSession(true));

        // Verify that the old password was correct
        try {
            AuthenticationProvider authProvider = userContext.getAuthenticationProvider();
            if (authProvider.authenticateUser(credentials) == null)
                throw new GuacamoleSecurityException("Permission denied.");
        }

        // Pass through any credentials exceptions as simple permission denied
        catch (GuacamoleCredentialsException e) {
            throw new GuacamoleSecurityException("Permission denied.");
        }

        // Set password to the newly provided one
        user.setPassword(userPasswordUpdate.getNewPassword());
        directory.update(user);

    }

    /**
     * Gets a list of permissions for the user with the given username.
     *
     * @return
     *     A list of all permissions granted to the specified user.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving permissions.
     */
    @GET
    @Path("permissions")
    public APIPermissionSet getPermissions() throws GuacamoleException {
        return new APIPermissionSet(user);
    }

    /**
     * Updates the given permission set patch by queuing an add or remove
     * operation for the given permission based on the given patch operation.
     *
     * @param <PermissionType>
     *     The type of permission stored within the permission set.
     *
     * @param operation
     *     The patch operation to perform.
     *
     * @param permissionSetPatch
     *     The permission set patch being modified.
     *
     * @param permission
     *     The permission being added or removed from the set.
     *
     * @throws GuacamoleException
     *     If the requested patch operation is not supported.
     */
    private <PermissionType extends Permission> void updatePermissionSet(
            APIPatch.Operation operation,
            PermissionSetPatch<PermissionType> permissionSetPatch,
            PermissionType permission) throws GuacamoleException {

        // Add or remove permission based on operation
        switch (operation) {

            // Add permission
            case add:
                permissionSetPatch.addPermission(permission);
                break;

            // Remove permission
            case remove:
                permissionSetPatch.removePermission(permission);
                break;

            // Unsupported patch operation
            default:
                throw new GuacamoleClientException("Unsupported patch operation: \"" + operation + "\"");

        }

    }

    /**
     * Applies a given list of permission patches. Each patch specifies either
     * an "add" or a "remove" operation for a permission type, represented by
     * a string. Valid permission types depend on the path of each patch
     * operation, as the path dictates the permission being modified, such as
     * "/connectionPermissions/42" or "/systemPermissions".
     *
     * @param patches
     *     The permission patches to apply for this request.
     *
     * @throws GuacamoleException
     *     If a problem is encountered while modifying permissions.
     */
    @PATCH
    @Path("permissions")
    public void patchPermissions(List<APIPatch<String>> patches)
            throws GuacamoleException {

        // Permission patches for all types of permissions
        PermissionSetPatch<ObjectPermission> connectionPermissionPatch       = new PermissionSetPatch<ObjectPermission>();
        PermissionSetPatch<ObjectPermission> connectionGroupPermissionPatch  = new PermissionSetPatch<ObjectPermission>();
        PermissionSetPatch<ObjectPermission> activeConnectionPermissionPatch = new PermissionSetPatch<ObjectPermission>();
        PermissionSetPatch<ObjectPermission> userPermissionPatch             = new PermissionSetPatch<ObjectPermission>();
        PermissionSetPatch<SystemPermission> systemPermissionPatch           = new PermissionSetPatch<SystemPermission>();

        // Apply all patch operations individually
        for (APIPatch<String> patch : patches) {

            String path = patch.getPath();

            // Create connection permission if path has connection prefix
            if (path.startsWith(CONNECTION_PERMISSION_PATCH_PATH_PREFIX)) {

                // Get identifier and type from patch operation
                String identifier = path.substring(CONNECTION_PERMISSION_PATCH_PATH_PREFIX.length());
                ObjectPermission.Type type = ObjectPermission.Type.valueOf(patch.getValue());

                // Create and update corresponding permission
                ObjectPermission permission = new ObjectPermission(type, identifier);
                updatePermissionSet(patch.getOp(), connectionPermissionPatch, permission);

            }

            // Create connection group permission if path has connection group prefix
            else if (path.startsWith(CONNECTION_GROUP_PERMISSION_PATCH_PATH_PREFIX)) {

                // Get identifier and type from patch operation
                String identifier = path.substring(CONNECTION_GROUP_PERMISSION_PATCH_PATH_PREFIX.length());
                ObjectPermission.Type type = ObjectPermission.Type.valueOf(patch.getValue());

                // Create and update corresponding permission
                ObjectPermission permission = new ObjectPermission(type, identifier);
                updatePermissionSet(patch.getOp(), connectionGroupPermissionPatch, permission);

            }

            // Create active connection permission if path has active connection prefix
            else if (path.startsWith(ACTIVE_CONNECTION_PERMISSION_PATCH_PATH_PREFIX)) {

                // Get identifier and type from patch operation
                String identifier = path.substring(ACTIVE_CONNECTION_PERMISSION_PATCH_PATH_PREFIX.length());
                ObjectPermission.Type type = ObjectPermission.Type.valueOf(patch.getValue());

                // Create and update corresponding permission
                ObjectPermission permission = new ObjectPermission(type, identifier);
                updatePermissionSet(patch.getOp(), activeConnectionPermissionPatch, permission);

            }

            // Create user permission if path has user prefix
            else if (path.startsWith(USER_PERMISSION_PATCH_PATH_PREFIX)) {

                // Get identifier and type from patch operation
                String identifier = path.substring(USER_PERMISSION_PATCH_PATH_PREFIX.length());
                ObjectPermission.Type type = ObjectPermission.Type.valueOf(patch.getValue());

                // Create and update corresponding permission
                ObjectPermission permission = new ObjectPermission(type, identifier);
                updatePermissionSet(patch.getOp(), userPermissionPatch, permission);

            }

            // Create system permission if path is system path
            else if (path.equals(SYSTEM_PERMISSION_PATCH_PATH)) {

                // Get identifier and type from patch operation
                SystemPermission.Type type = SystemPermission.Type.valueOf(patch.getValue());

                // Create and update corresponding permission
                SystemPermission permission = new SystemPermission(type);
                updatePermissionSet(patch.getOp(), systemPermissionPatch, permission);

            }

            // Otherwise, the path is not supported
            else
                throw new GuacamoleClientException("Unsupported patch path: \"" + path + "\"");

        } // end for each patch operation

        // Save the permission changes
        connectionPermissionPatch.apply(user.getConnectionPermissions());
        connectionGroupPermissionPatch.apply(user.getConnectionGroupPermissions());
        activeConnectionPermissionPatch.apply(user.getActiveConnectionPermissions());
        userPermissionPatch.apply(user.getUserPermissions());
        systemPermissionPatch.apply(user.getSystemPermissions());

    }

}
