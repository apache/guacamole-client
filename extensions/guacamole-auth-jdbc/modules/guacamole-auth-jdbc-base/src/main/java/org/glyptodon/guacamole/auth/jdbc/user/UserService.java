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

package org.glyptodon.guacamole.auth.jdbc.user;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.auth.jdbc.base.ModeledDirectoryObjectMapper;
import org.glyptodon.guacamole.auth.jdbc.base.ModeledDirectoryObjectService;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleUnsupportedException;
import org.glyptodon.guacamole.auth.jdbc.permission.ObjectPermissionMapper;
import org.glyptodon.guacamole.auth.jdbc.permission.ObjectPermissionModel;
import org.glyptodon.guacamole.auth.jdbc.permission.UserPermissionMapper;
import org.glyptodon.guacamole.auth.jdbc.security.PasswordEncryptionService;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 *
 * @author Michael Jumper, James Muehlner
 */
public class UserService extends ModeledDirectoryObjectService<ModeledUser, User, UserModel> {
    
    /**
     * All user permissions which are implicitly granted to the new user upon
     * creation.
     */
    private static final ObjectPermission.Type[] IMPLICIT_USER_PERMISSIONS = {
        ObjectPermission.Type.READ,
        ObjectPermission.Type.UPDATE
    };
    
    /**
     * Mapper for accessing users.
     */
    @Inject
    private UserMapper userMapper;

    /**
     * Mapper for manipulating user permissions.
     */
    @Inject
    private UserPermissionMapper userPermissionMapper;
    
    /**
     * Provider for creating users.
     */
    @Inject
    private Provider<ModeledUser> userProvider;

    /**
     * Service for hashing passwords.
     */
    @Inject
    private PasswordEncryptionService encryptionService;

    @Override
    protected ModeledDirectoryObjectMapper<UserModel> getObjectMapper() {
        return userMapper;
    }

    @Override
    protected ObjectPermissionMapper getPermissionMapper() {
        return userPermissionMapper;
    }

    @Override
    protected ModeledUser getObjectInstance(AuthenticatedUser currentUser,
            UserModel model) {
        ModeledUser user = userProvider.get();
        user.init(currentUser, model);
        return user;
    }

    @Override
    protected UserModel getModelInstance(AuthenticatedUser currentUser,
            final User object) {

        // Create new ModeledUser backed by blank model
        UserModel model = new UserModel();
        ModeledUser user = getObjectInstance(currentUser, model);

        // Set model contents through ModeledUser, copying the provided user
        user.setIdentifier(object.getIdentifier());
        user.setPassword(object.getPassword());

        return model;
        
    }

    @Override
    protected boolean hasCreatePermission(AuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit user creation permission
        SystemPermissionSet permissionSet = user.getUser().getSystemPermissions();
        return permissionSet.hasPermission(SystemPermission.Type.CREATE_USER);

    }

    @Override
    protected ObjectPermissionSet getPermissionSet(AuthenticatedUser user)
            throws GuacamoleException {

        // Return permissions related to users
        return user.getUser().getUserPermissions();

    }

    @Override
    protected void beforeCreate(AuthenticatedUser user, UserModel model)
            throws GuacamoleException {

        super.beforeCreate(user, model);
        
        // Username must not be blank
        if (model.getIdentifier() == null || model.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("The username must not be blank.");
        
        // Do not create duplicate users
        Collection<UserModel> existing = userMapper.select(Collections.singleton(model.getIdentifier()));
        if (!existing.isEmpty())
            throw new GuacamoleClientException("User \"" + model.getIdentifier() + "\" already exists.");

    }

    @Override
    protected void beforeUpdate(AuthenticatedUser user,
            UserModel model) throws GuacamoleException {

        super.beforeUpdate(user, model);
        
        // Username must not be blank
        if (model.getIdentifier() == null || model.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("The username must not be blank.");
        
        // Check whether such a user is already present
        UserModel existing = userMapper.selectOne(model.getIdentifier());
        if (existing != null) {

            // Do not rename to existing user
            if (!existing.getObjectID().equals(model.getObjectID()))
                throw new GuacamoleClientException("User \"" + model.getIdentifier() + "\" already exists.");
            
        }
        
    }

    @Override
    protected Collection<ObjectPermissionModel>
        getImplicitPermissions(AuthenticatedUser user, UserModel model) {
            
        // Get original set of implicit permissions
        Collection<ObjectPermissionModel> implicitPermissions = super.getImplicitPermissions(user, model);
        
        // Grant implicit permissions to the new user
        for (ObjectPermission.Type permissionType : IMPLICIT_USER_PERMISSIONS) {
            
            ObjectPermissionModel permissionModel = new ObjectPermissionModel();
            permissionModel.setUserID(model.getObjectID());
            permissionModel.setUsername(model.getIdentifier());
            permissionModel.setType(permissionType);
            permissionModel.setObjectIdentifier(model.getIdentifier());

            // Add new permission to implicit permission set 
            implicitPermissions.add(permissionModel);
            
        }
        
        return implicitPermissions;
    }
        
    @Override
    protected void beforeDelete(AuthenticatedUser user, String identifier) throws GuacamoleException {

        super.beforeDelete(user, identifier);

        // Do not allow users to delete themselves
        if (identifier.equals(user.getUser().getIdentifier()))
            throw new GuacamoleUnsupportedException("Deleting your own user is not allowed.");

    }

    /**
     * Retrieves the user corresponding to the given credentials from the
     * database.
     *
     * @param credentials
     *     The credentials to use when locating the user.
     *
     * @return
     *     The existing ModeledUser object if the credentials given are valid,
     *     null otherwise.
     */
    public ModeledUser retrieveUser(Credentials credentials) {

        // Get username and password
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        // Retrieve corresponding user model, if such a user exists
        UserModel userModel = userMapper.selectOne(username);
        if (userModel == null)
            return null;

        // If password hash matches, return the retrieved user
        byte[] hash = encryptionService.createPasswordHash(password, userModel.getPasswordSalt());
        if (Arrays.equals(hash, userModel.getPasswordHash())) {

            // Return corresponding user, set up cyclic reference
            ModeledUser user = getObjectInstance(null, userModel);
            user.setCurrentUser(new AuthenticatedUser(user, credentials));
            return user;

        }

        // Otherwise, the credentials do not match
        return null;

    }

}
