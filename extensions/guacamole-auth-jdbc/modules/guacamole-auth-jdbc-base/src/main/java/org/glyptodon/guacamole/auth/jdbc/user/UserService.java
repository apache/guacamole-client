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
import java.util.Collection;
import java.util.Collections;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.auth.jdbc.base.DirectoryObjectMapper;
import org.glyptodon.guacamole.auth.jdbc.base.DirectoryObjectService;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 *
 * @author Michael Jumper, James Muehlner
 */
public class UserService extends DirectoryObjectService<ModeledUser, User, UserModel> {

    /**
     * Mapper for accessing users.
     */
    @Inject
    private UserMapper userMapper;

    /**
     * Provider for creating users.
     */
    @Inject
    private Provider<ModeledUser> userProvider;

    @Override
    protected DirectoryObjectMapper<UserModel> getObjectMapper() {
        return userMapper;
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
    protected void validateNewObject(AuthenticatedUser user, User object)
            throws GuacamoleException {

        // Username must not be blank
        if (object.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("The username must not be blank.");
        
        // Do not create duplicate users
        Collection<UserModel> existing = userMapper.select(Collections.singleton(object.getIdentifier()));
        if (!existing.isEmpty())
            throw new GuacamoleClientException("User \"" + object.getIdentifier() + "\" already exists.");

    }

    @Override
    protected void validateExistingObject(AuthenticatedUser user,
            ModeledUser object) throws GuacamoleException {

        // Username must not be blank
        if (object.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("The username must not be blank.");
        
        // Check whether such a user is already present
        ModeledUser existing = retrieveObject(user, object.getIdentifier());
        if (existing != null) {

            UserModel existingModel = existing.getModel();
            UserModel updatedModel = object.getModel();

            // Do not rename to existing user
            if (!existingModel.getObjectID().equals(updatedModel.getObjectID()))
                throw new GuacamoleClientException("User \"" + object.getIdentifier() + "\" already exists.");
            
        }
        
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

        // Retrieve user model, if the user exists
        UserModel userModel = userMapper.selectByCredentials(username, password);
        if (userModel == null)
            return null;

        // Return corresponding user, set up cyclic reference
        ModeledUser user = getObjectInstance(null, userModel);
        user.setCurrentUser(new AuthenticatedUser(user, credentials));
        return user;

    }

}
