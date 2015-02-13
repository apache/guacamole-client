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

package net.sourceforge.guacamole.net.auth.mysql.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.DirectoryObjectMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.UserModel;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 *
 * @author Michael Jumper, James Muehlner
 */
public class UserService extends DirectoryObjectService<MySQLUser, UserModel> {

    /**
     * Mapper for accessing users.
     */
    @Inject
    private UserMapper userMapper;

    /**
     * Provider for creating users.
     */
    @Inject
    private Provider<MySQLUser> mySQLUserProvider;

    @Override
    protected DirectoryObjectMapper<UserModel> getObjectMapper() {
        return userMapper;
    }

    @Override
    protected MySQLUser getObjectInstance(UserModel model) {
        MySQLUser user = mySQLUserProvider.get();
        user.setModel(model);
        return user;
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

    /**
     * Retrieves the user corresponding to the given credentials from the
     * database.
     *
     * @param credentials The credentials to use when locating the user.
     * @return The existing MySQLUser object if the credentials given are
     *         valid, null otherwise.
     */
    public MySQLUser retrieveUser(Credentials credentials) {

        // Get username and password
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        // Retrieve user model, if the user exists
        UserModel userModel = userMapper.selectByCredentials(username, password);
        if (userModel == null)
            return null;

        // Return corresponding user
        return getObjectInstance(userModel);
        
    }

}
