
package net.sourceforge.guacamole.net.auth.mysql.service;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-auth-mysql.
 *
 * The Initial Developer of the Original Code is
 * James Muehlner.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserWithBLOBs;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating users.
 *
 * @author Michael Jumper, James Muehlner
 */
public class UserService {

    /**
     * DAO for accessing users.
     */
    @Inject
    private UserMapper userDAO;

    /**
     * DAO for accessing user permissions, which will be injected.
     */
    @Inject
    private UserPermissionMapper userPermissionDAO;

    /**
     * DAO for accessing connection permissions, which will be injected.
     */
    @Inject
    private ConnectionPermissionMapper connectionPermissionDAO;

    /**
     * DAO for accessing system permissions, which will be injected.
     */
    @Inject
    private SystemPermissionMapper systemPermissionDAO;

    /**
     * Provider for creating users.
     */
    @Inject
    private Provider<MySQLUser> mySQLUserProvider;

    /**
     * Service for checking permissions.
     */
    @Inject
    private PermissionCheckService permissionCheckService;

    /**
     * Service for encrypting passwords.
     */
    @Inject
    private PasswordEncryptionService passwordService;

    /**
     * Service for generating random salts.
     */
    @Inject
    private SaltService saltService;

    /**
     * Create a new MySQLUser based on the provided User.
     *
     * @param user The User to use when populating the data of the given
     *             MySQLUser.
     * @return A new MySQLUser object, populated with the data of the given
     *         user.
     *
     * @throws GuacamoleException If an error occurs while reading the data
     *                            of the provided User.
     */
    public MySQLUser toMySQLUser(User user) throws GuacamoleException {
        MySQLUser mySQLUser = mySQLUserProvider.get();
        mySQLUser.init(user);
        return mySQLUser;
    }

    /**
     * Create a new MySQLUser based on the provided database record.
     *
     * @param user The database record describing the user.
     * @return A new MySQLUser object, populated with the data of the given
     *         database record.
     */
    private MySQLUser toMySQLUser(UserWithBLOBs user) {

        // Retrieve user from provider
        MySQLUser mySQLUser = mySQLUserProvider.get();

        // Init with data from given database user
        mySQLUser.init(
            user.getUser_id(),
            user.getUsername(),
            null,
            permissionCheckService.getAllPermissions(user.getUser_id())
        );

        // Return new user
        return mySQLUser;

    }

    /**
     * Retrieves the user having the given ID from the database.
     *
     * @param id The ID of the user to retrieve.
     * @return The existing MySQLUser object if found, null otherwise.
     */
    public MySQLUser retrieveUser(Integer id) {

        // Query user by ID
        UserWithBLOBs user = userDAO.selectByPrimaryKey(id);

        // If no user found, return null
        if(user == null)
            return null;

        // Otherwise, return found user
        return toMySQLUser(user);

    }

    /**
     * Retrieves the users having the given IDs from the database.
     *
     * @param ids The IDs of the users to retrieve.
     * @return A list of existing MySQLUser objects.
     */
    public List<MySQLUser> retrieveUsersByID(List<Integer> ids) {

        // If no IDs given, just return empty list
        if (ids.isEmpty())
            return Collections.EMPTY_LIST;

        // Query users by ID
        UserExample example = new UserExample();
        example.createCriteria().andUser_idIn(ids);
        List<UserWithBLOBs> users = userDAO.selectByExampleWithBLOBs(example);

        // Convert to MySQLUser list
        List<MySQLUser> mySQLUsers = new ArrayList<MySQLUser>(users.size());
        for (UserWithBLOBs user : users)
            mySQLUsers.add(toMySQLUser(user));

        // Return found users
        return mySQLUsers;

    }

    /**
     * Retrieves the users having the given usernames from the database.
     *
     * @param names The usernames of the users to retrieve.
     * @return A list of existing MySQLUser objects.
     */
    public List<MySQLUser> retrieveUsersByUsername(List<String> names) {

        // If no names given, just return empty list
        if (names.isEmpty())
            return Collections.EMPTY_LIST;

        // Query users by ID
        UserExample example = new UserExample();
        example.createCriteria().andUsernameIn(names);
        List<UserWithBLOBs> users = userDAO.selectByExampleWithBLOBs(example);

        // Convert to MySQLUser list
        List<MySQLUser> mySQLUsers = new ArrayList<MySQLUser>(users.size());
        for (UserWithBLOBs user : users)
            mySQLUsers.add(toMySQLUser(user));

        // Return found users
        return mySQLUsers;

    }

    /**
     * Retrieves the user having the given username from the database.
     *
     * @param name The username of the user to retrieve.
     * @return The existing MySQLUser object if found, null otherwise.
     */
    public MySQLUser retrieveUser(String name) {

        // Query user by ID
        UserExample example = new UserExample();
        example.createCriteria().andUsernameEqualTo(name);
        List<UserWithBLOBs> users = userDAO.selectByExampleWithBLOBs(example);

        // If no user found, return null
        if(users.isEmpty())
            return null;

        // Otherwise, return found user
        return toMySQLUser(users.get(0));

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

        // No null users in database
        if (credentials.getUsername() == null)
            return null;

        // Query user
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUsernameEqualTo(credentials.getUsername());
        List<UserWithBLOBs> users = userDAO.selectByExampleWithBLOBs(userExample);

        // Check that a user was found
        if (users.isEmpty())
            return null;

        // Assert only one user found
        assert users.size() == 1 : "Multiple users with same username.";

        // Get first (and only) user
        UserWithBLOBs user = users.get(0);

        // Check password, if invalid return null
        if (!passwordService.checkPassword(credentials.getPassword(),
                user.getPassword_hash(), user.getPassword_salt()))
            return null;

        // Return found user
        return toMySQLUser(user);

    }

    /**
     * Creates a new user having the given username and password.
     *
     * @param username The username to assign to the new user.
     * @param password The password to assign to the new user.
     * @return A new MySQLUser containing the data of the newly created
     *         user.
     */
    public MySQLUser createUser(String username, String password) {

        // Initialize database user
        UserWithBLOBs user = new UserWithBLOBs();
        user.setUsername(username);

        // Set password if specified
        if (password != null) {
            byte[] salt = saltService.generateSalt();
            user.setPassword_salt(salt);
            user.setPassword_hash(
                passwordService.createPasswordHash(password, salt));
        }

        // Create user
        userDAO.insert(user);
        return toMySQLUser(user);

    }

    /**
     * Deletes the user having the given username from the database
     * @param username The username of the user to delete.
     */
    public void deleteUser(String username) {

        // Get specified user
        MySQLUser mySQLUser = retrieveUser(username);
        int user_id = mySQLUser.getUserID();

        // Delete all user permissions
        UserPermissionExample userPermissionExample = new UserPermissionExample();
        userPermissionExample.createCriteria().andUser_idEqualTo(user_id);
        userPermissionDAO.deleteByExample(userPermissionExample);

        // Delete all connection permissions
        ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
        connectionPermissionExample.createCriteria().andUser_idEqualTo(user_id);
        connectionPermissionDAO.deleteByExample(connectionPermissionExample);

        // Delete all system permissions
        SystemPermissionExample systemPermissionExample = new SystemPermissionExample();
        systemPermissionExample.createCriteria().andUser_idEqualTo(user_id);
        systemPermissionDAO.deleteByExample(systemPermissionExample);

        // Delete all permissions that refer to this user
        userPermissionExample.clear();
        userPermissionExample.createCriteria().andAffected_user_idEqualTo(user_id);
        userPermissionDAO.deleteByExample(userPermissionExample);

        // Delete the user in the database
        userDAO.deleteByPrimaryKey(user_id);

    }


    /**
     * Updates the user in the database corresponding to the given MySQLUser.
     *
     * @param mySQLUser The MySQLUser to update (save) to the database. This
     *                  user must already exist.
     */
    public void updateUser(MySQLUser mySQLUser) {

        UserWithBLOBs user = new UserWithBLOBs();
        user.setUser_id(mySQLUser.getUserID());
        user.setUsername(mySQLUser.getUsername());

        // Set password if specified
        if (mySQLUser.getPassword() != null) {
            byte[] salt = saltService.generateSalt();
            user.setPassword_salt(salt);
            user.setPassword_hash(
                passwordService.createPasswordHash(mySQLUser.getPassword(), salt));
        }

        // Update the user in the database
        userDAO.updateByPrimaryKeySelective(user);

    }


}
