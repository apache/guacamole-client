
package net.sourceforge.guacamole.net.auth.mysql;

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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.Connection;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.User;
import net.sourceforge.guacamole.net.auth.mysql.model.UserExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.UserWithBLOBs;
import net.sourceforge.guacamole.net.auth.mysql.service.PasswordEncryptionService;
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
import net.sourceforge.guacamole.net.auth.mysql.service.ProviderService;
import net.sourceforge.guacamole.net.auth.mysql.service.SaltService;
import net.sourceforge.guacamole.net.auth.permission.ConnectionDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.ConnectionPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.net.auth.permission.UserDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.UserPermission;
import org.mybatis.guice.transactional.Transactional;

/**
 * A MySQL based implementation of the User Directory.
 * @author James Muehlner
 */
public class UserDirectory implements Directory<String, net.sourceforge.guacamole.net.auth.User> {

    /**
     * The ID of the user who this user directory belongs to.
     * Access is based on his/her permission settings.
     */
    private int user_id;

    /**
     * DAO for accessing users, which will be injected.
     */
    @Inject
    private UserMapper userDAO;

    /**
     * DAO for accessing connections, which will be injected.
     */
    @Inject
    private ConnectionMapper connectionDAO;

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
     * Utility class for checking various permissions, which will be injected.
     */
    @Inject
    private PermissionCheckService permissionCheckUtility;

    /**
     * Utility class that provides convenient access to object creation and
     * retrieval functions.
     */
    @Inject
    private ProviderService providerUtility;
    
    /**
     * Service for encrypting passwords.
     */
    @Inject
    private PasswordEncryptionService passwordUtility;

    /**
     * Service for generating random salts.
     */
    @Inject
    private SaltService saltUtility;

    /**
     * Set the user for this directory.
     *
     * @param user_id The ID of the user whose permissions define the visibility
     *                of other users in this directory.
     */
    public void init(int user_id) {
        this.user_id = user_id;
    }

    @Transactional
    @Override
    public net.sourceforge.guacamole.net.auth.User get(String identifier)
            throws GuacamoleException {
        permissionCheckUtility.verifyUserReadAccess(this.user_id, identifier);
        return providerUtility.getExistingMySQLUser(identifier);
    }

    @Transactional
    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {

        // Get set of all readable users
        Set<MySQLUser> users = permissionCheckUtility.getReadableUsers(this.user_id);

        // Build set of usernames of readable users
        Set<String> userNameSet = new HashSet<String>();
        for (MySQLUser mySQLUser : users)
            userNameSet.add(mySQLUser.getUsername());

        return userNameSet;
    }

    @Override
    @Transactional
    public void add(net.sourceforge.guacamole.net.auth.User object)
            throws GuacamoleException {

        // Verify current user has permission to create users
        permissionCheckUtility.verifyCreateUserPermission(this.user_id);
        Preconditions.checkNotNull(object);

        // Create user in database
        UserWithBLOBs user = new UserWithBLOBs();
        user.setUsername(object.getUsername());

        // Set password if specified
        if (object.getPassword() != null) {
            byte[] salt = saltUtility.generateSalt();
            user.setPassword_salt(salt);
            user.setPassword_hash(
                passwordUtility.createPasswordHash(object.getPassword(), salt));
        }
        
        userDAO.insert(user);

        // Create permissions of new user in database
        createPermissions(user.getUser_id(), object.getPermissions());

        // Give the current user full access to the newly created user.
        UserPermissionKey newUserPermission = new UserPermissionKey();
        newUserPermission.setUser_id(this.user_id);
        newUserPermission.setAffected_user_id(user.getUser_id());

        // READ permission on new user
        newUserPermission.setPermission(MySQLConstants.USER_READ);
        userPermissionDAO.insert(newUserPermission);

        // UPDATE permission on new user
        newUserPermission.setPermission(MySQLConstants.USER_UPDATE);
        userPermissionDAO.insert(newUserPermission);

        // DELETE permission on new user
        newUserPermission.setPermission(MySQLConstants.USER_DELETE);
        userPermissionDAO.insert(newUserPermission);

        // ADMINISTER permission on new user
        newUserPermission.setPermission(MySQLConstants.USER_ADMINISTER);
        userPermissionDAO.insert(newUserPermission);

    }

    /**
     * Update all the permissions for a given user to be only those specified in the user object.
     * Delete any permissions not in the list, and create any in the list that do not exist
     * in the database.
     *
     * @param user The user whose permissions should be updated.
     * @throws GuacamoleException If an error occurs while updating the
     *                            permissions of the given user.
     */
    private void createPermissions(int user_id, Set<Permission> permissions) throws GuacamoleException {

        // Partition given permissions by permission type
        List<UserPermission> newUserPermissions = new ArrayList<UserPermission>();
        List<ConnectionPermission> newConnectionPermissions = new ArrayList<ConnectionPermission>();
        List<SystemPermission> newSystemPermissions = new ArrayList<SystemPermission>();

        for (Permission permission : permissions) {

            if (permission instanceof UserPermission)
                newUserPermissions.add((UserPermission) permission);

            else if (permission instanceof ConnectionPermission)
                newConnectionPermissions.add((ConnectionPermission) permission);

            else if (permission instanceof SystemPermission)
                newSystemPermissions.add((SystemPermission) permission);
        }

        // Create the new permissions
        createUserPermissions(newUserPermissions, user_id);
        createConnectionPermissions(newConnectionPermissions, user_id);
        createSystemPermissions(newSystemPermissions, user_id);

    }


    private void removePermissions(int user_id, Set<Permission> permissions) throws GuacamoleException {
        
        // Partition given permissions by permission type
        List<UserPermission> removedUserPermissions = new ArrayList<UserPermission>();
        List<ConnectionPermission> removedConnectionPermissions = new ArrayList<ConnectionPermission>();
        List<SystemPermission> removedSystemPermissions = new ArrayList<SystemPermission>();

        for (Permission permission : permissions) {

            if (permission instanceof UserPermission)
                removedUserPermissions.add((UserPermission) permission);

            else if (permission instanceof ConnectionPermission)
                removedConnectionPermissions.add((ConnectionPermission) permission);

            else if (permission instanceof SystemPermission)
                removedSystemPermissions.add((SystemPermission) permission);
        }
        
        // Delete the removed permissions.
        deleteUserPermissions(removedUserPermissions, user_id);
        deleteConnectionPermissions(removedConnectionPermissions, user_id);
        deleteSystemPermissions(removedSystemPermissions, user_id);
        
    }

    /**
     * Create any new permissions having to do with users for a given user.
     *
     * @param permissions The new permissions the given user should have when
     *                    this operation completes.
     * @param user_id The ID of the user to change the permissions of.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is denied.
     */
    private void createUserPermissions(Collection<UserPermission> permissions,
            int user_id)
            throws GuacamoleException {
        
        if(permissions.isEmpty())
            return;

        // Get set of administerable users
        Set<Integer> administerableUsers =
                permissionCheckUtility.getAdministerableUserIDs(this.user_id);

        // Get list of usernames for all given user permissions.
        List<String> usernames = new ArrayList<String>();
        for (UserPermission permission : permissions)
            usernames.add(permission.getObjectIdentifier());

        // Find all the users by username
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUsernameIn(usernames);
        List<User> dbUsers = userDAO.selectByExample(userExample);

        // Build map of found users, indexed by username
        Map<String, User> dbUserMap = new HashMap<String, User>();
        for (User dbUser : dbUsers) {
            dbUserMap.put(dbUser.getUsername(), dbUser);
        }
        
        for (UserPermission permission : permissions) {

            // Get user
            User dbAffectedUser = dbUserMap.get(permission.getObjectIdentifier());
            if (dbAffectedUser == null)
                throw new GuacamoleException(
                          "User '" + permission.getObjectIdentifier()
                        + "' not found.");

            // Verify that the user actually has permission to administrate
            // every one of these users
            if (!administerableUsers.contains(dbAffectedUser.getUser_id()))
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate user "
                    + dbAffectedUser.getUser_id());

            // Create new permission
            UserPermissionKey newPermission = new UserPermissionKey();
            newPermission.setAffected_user_id(dbAffectedUser.getUser_id());
            newPermission.setPermission(permission.getType().name());
            newPermission.setUser_id(user_id);
            userPermissionDAO.insert(newPermission);
         }
    }

    /**
     * Delete permissions having to do with users for a given user.
     *
     * @param permissions The permissions the given user should no longer have
     *                    when this operation completes.
     * @param user_id The ID of the user to change the permissions of.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is denied.
     */
    private void deleteUserPermissions(Collection<UserPermission> permissions,
            int user_id)
            throws GuacamoleException {
        
        if(permissions.isEmpty())
            return;

        // Get set of administerable users
        Set<Integer> administerableUsers =
                permissionCheckUtility.getAdministerableUserIDs(this.user_id);

        // Get list of usernames for all given user permissions.
        List<String> usernames = new ArrayList<String>();
        for (UserPermission permission : permissions)
            usernames.add(permission.getObjectIdentifier());

        // Find all the users by username
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUsernameIn(usernames);
        List<User> dbUsers = userDAO.selectByExample(userExample);
        List<Integer> userIDs = new ArrayList<Integer>();

        // Build map of found users, indexed by username
        Map<String, User> dbUserMap = new HashMap<String, User>();
        for (User dbUser : dbUsers) {
            dbUserMap.put(dbUser.getUsername(), dbUser);
            userIDs.add(dbUser.getUser_id());
        }
        
        // Verify we have permission to delete each user permission.
        for (UserPermission permission : permissions) {

            // Get user
            User dbAffectedUser = dbUserMap.get(permission.getObjectIdentifier());
            if (dbAffectedUser == null)
                throw new GuacamoleException(
                          "User '" + permission.getObjectIdentifier()
                        + "' not found.");

            // Verify that the user actually has permission to administrate
            // every one of these users
            if (!administerableUsers.contains(dbAffectedUser.getUser_id()))
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate user "
                    + dbAffectedUser.getUser_id());
        }
        
        if(!userIDs.isEmpty()) {
            UserPermissionExample userPermissionExample = new UserPermissionExample();
            userPermissionExample.createCriteria().andUser_idEqualTo(user_id)
                    .andAffected_user_idIn(userIDs);
            userPermissionDAO.deleteByExample(userPermissionExample);
        }
    }

    /**
     * Create any new permissions having to do with connections for a given
     * user.
     *
     * @param permissions The new permissions the user should have after this
     *                    operation completes.
     * @param user_id The ID of the user to assign or remove permissions from.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is deniedD
     */
    private void createConnectionPermissions(
            Collection<ConnectionPermission> permissions, int user_id)
            throws GuacamoleException {
        
        if(permissions.isEmpty())
            return;

        // Get adminsterable connection identifiers
        Set<Integer> administerableConnections =
                permissionCheckUtility.getAdministerableConnectionIDs(this.user_id);

        // Build list of affected connection names from the permissions given
        List<String> connectionNames = new ArrayList<String>();
        for (ConnectionPermission permission : permissions)
            connectionNames.add(permission.getObjectIdentifier());

        // Find all the connections by connection name
        ConnectionExample connectionExample = new ConnectionExample();
        connectionExample.createCriteria().andConnection_nameIn(connectionNames);
        List<Connection> dbConnections = connectionDAO.selectByExample(connectionExample);

        // Build map of found connections, indexed by name
        Map<String, Connection> dbConnectionMap = new HashMap<String, Connection>();
        for (Connection dbConnection : dbConnections) {
            dbConnectionMap.put(dbConnection.getConnection_name(), dbConnection);
        }

        // Finally, insert the new permissions
        for (ConnectionPermission permission : permissions) {

            // Get permission
            Connection dbConnection = dbConnectionMap.get(permission.getObjectIdentifier());
            if (dbConnection == null)
                throw new GuacamoleException(
                    "Connection '" + permission.getObjectIdentifier()
                    + "' not found.");

            // Throw exception if permission to administer this connection
            // is not granted
            if (!administerableConnections.contains(dbConnection.getConnection_id()))
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate connection "
                    + dbConnection.getConnection_id());


            // Insert previously-non-existent connection permission
            ConnectionPermissionKey newPermission = new ConnectionPermissionKey();
            newPermission.setConnection_id(dbConnection.getConnection_id());
            newPermission.setPermission(permission.getType().name());
            newPermission.setUser_id(user_id);
            connectionPermissionDAO.insert(newPermission);
        }
    }

    /**
     * Delete permissions having to do with connections for a given user.
     *
     * @param permissions The permissions the given user should no longer have
     *                    when this operation completes.
     * @param user_id The ID of the user to change the permissions of.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is denied.
     */
    private void deleteConnectionPermissions(Collection<ConnectionPermission> permissions,
            int user_id)
            throws GuacamoleException {
        
        if(permissions.isEmpty())
            return;

        // Get set of administerable users
        Set<Integer> administerableConnections =
                permissionCheckUtility.getAdministerableConnectionIDs(this.user_id);

        // Get list of identifiers for all given user permissions.
        List<String> identifiers = new ArrayList<String>();
        for (ConnectionPermission permission : permissions)
            identifiers.add(permission.getObjectIdentifier());

        // Find all the connections by identifiers
        ConnectionExample connectionExample = new ConnectionExample();
        connectionExample.createCriteria().andConnection_nameIn(identifiers);
        List<Connection> dbConnections = connectionDAO.selectByExample(connectionExample);
        List<Integer> connectionIDs = new ArrayList<Integer>();

        // Build map of found connections, indexed by identifier
        Map<String, Connection> dbConnectionMap = new HashMap<String, Connection>();
        for (Connection dbConnection : dbConnections) {
            dbConnectionMap.put(dbConnection.getConnection_name(), dbConnection);
            connectionIDs.add(dbConnection.getConnection_id());
        }
        
        // Verify we have permission to delete each connection permission.
        for (ConnectionPermission permission : permissions) {

            // Get user
            Connection dbConnection = dbConnectionMap.get(permission.getObjectIdentifier());
            if (dbConnection == null)
                throw new GuacamoleException(
                          "User '" + permission.getObjectIdentifier()
                        + "' not found.");

            // Verify that the user actually has permission to administrate
            // every one of these connections
            if (!administerableConnections.contains(dbConnection.getConnection_id()))
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate connection "
                    + dbConnection.getConnection_id());
        }
        
        if(!connectionIDs.isEmpty()) {
            ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
            connectionPermissionExample.createCriteria().andUser_idEqualTo(user_id)
                    .andConnection_idIn(connectionIDs);
            connectionPermissionDAO.deleteByExample(connectionPermissionExample);
        }
    }

    /**
     * Create any new system permissions for a given user. All permissions in
     * the given list will be inserted.
     *
     * @param permissions The new system permissions that the given user should
     *                    have when this operation completes.
     * @param user_id The ID of the user whose permissions should be updated.
     */
    private void createSystemPermissions(Collection<SystemPermission> permissions,
            int user_id) {

        if(permissions.isEmpty())
            return;
        
        // Build list of requested system permissions
        List<String> systemPermissionTypes = new ArrayList<String>();
        for (SystemPermission permission : permissions) {

            // Connection directory permission
            if (permission instanceof ConnectionDirectoryPermission) {
                switch (permission.getType()) {

                    // Create permission
                    case CREATE:
                        systemPermissionTypes.add(MySQLConstants.SYSTEM_CONNECTION_CREATE);
                        break;

                    // Fail if unexpected type encountered
                    default:
                        assert false : "Unsupported type: " + permission.getType();

                }
            }

            // User directory permission
            else if (permission instanceof UserDirectoryPermission) {
                switch (permission.getType()) {

                    // Create permission
                    case CREATE:
                        systemPermissionTypes.add(MySQLConstants.SYSTEM_USER_CREATE);
                        break;

                    // Fail if unexpected type encountered
                    default:
                        assert false : "Unsupported type: " + permission.getType();

                }
            }

        } // end for each system permission

        // Finally, insert any NEW system permissions for this user
        for (String systemPermissionType : systemPermissionTypes) {

            // Insert permission
            SystemPermissionKey newSystemPermission = new SystemPermissionKey();
            newSystemPermission.setUser_id(user_id);
            newSystemPermission.setPermission(systemPermissionType);
            systemPermissionDAO.insert(newSystemPermission);

        }

    }
    
    /**
     * Delete system permissions for a given user. All permissions in
     * the given list will be removed from the user.
     *
     * @param permissions The permissions the given user should no longer have
     *                    when this operation completes.
     * @param user_id The ID of the user whose permissions should be updated.
     */
    private void deleteSystemPermissions(Collection<SystemPermission> permissions,
            int user_id) {
        
        if(permissions.isEmpty())
            return;

        // Build list of requested system permissions
        List<String> systemPermissionTypes = new ArrayList<String>();
        for (SystemPermission permission : permissions) {

            // Connection directory permission
            if (permission instanceof ConnectionDirectoryPermission) {
                switch (permission.getType()) {

                    // Create permission
                    case CREATE:
                        systemPermissionTypes.add(MySQLConstants.SYSTEM_CONNECTION_CREATE);
                        break;

                    // Fail if unexpected type encountered
                    default:
                        assert false : "Unsupported type: " + permission.getType();

                }
            }

            // User directory permission
            else if (permission instanceof UserDirectoryPermission) {
                switch (permission.getType()) {

                    // Create permission
                    case CREATE:
                        systemPermissionTypes.add(MySQLConstants.SYSTEM_USER_CREATE);
                        break;

                    // Fail if unexpected type encountered
                    default:
                        assert false : "Unsupported type: " + permission.getType();

                }
            }

        } // end for each system permission

        // Finally, delete the requested system permissions for this user
        if(!systemPermissionTypes.isEmpty()) {
            SystemPermissionExample systemPermissionExample = new SystemPermissionExample();
            systemPermissionExample.createCriteria().andUser_idEqualTo(user_id)
                    .andPermissionIn(systemPermissionTypes);
            systemPermissionDAO.deleteByExample(systemPermissionExample);
        }
    }

    @Override
    @Transactional
    public void update(net.sourceforge.guacamole.net.auth.User object)
            throws GuacamoleException {

        // If user not actually from this auth provider, we can't handle updated
        // permissions.
        if (!(object instanceof MySQLUser))
            throw new GuacamoleException("User not from database.");
        
        // Validate permission to update this user is granted
        permissionCheckUtility.verifyUserUpdateAccess(this.user_id,
                object.getUsername());

        // Update the user in the database
        MySQLUser mySQLUser = (MySQLUser) object;
        userDAO.updateByPrimaryKeySelective(mySQLUser.toUserWithBLOBs());

        // Update permissions in database
        createPermissions(mySQLUser.getUserID(), mySQLUser.getNewPermissions());
        removePermissions(mySQLUser.getUserID(), mySQLUser.getRemovedPermissions());

        // The appropriate permissions have been inserted and deleted, so
        // reset the new and removed permission sets.
        mySQLUser.resetPermissions();

    }

    @Override
    @Transactional
    public void remove(String identifier) throws GuacamoleException {

        // Validate current user has permission to remove the specified user
        permissionCheckUtility.verifyUserDeleteAccess(this.user_id,
                identifier);

        // Get specified user
        MySQLUser mySQLUser = providerUtility.getExistingMySQLUser(identifier);

        // Delete all the user permissions in the database
        deleteAllPermissions(mySQLUser.getUserID());

        // Delete the user in the database
        userDAO.deleteByPrimaryKey(mySQLUser.getUserID());

    }

    /**
     * Delete all permissions associated with the provided user. This is only
     * used when deleting a user.
     *
     * @param user_id The ID of the user to delete all permissions of.
     */
    private void deleteAllPermissions(int user_id) {

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

    }

}
