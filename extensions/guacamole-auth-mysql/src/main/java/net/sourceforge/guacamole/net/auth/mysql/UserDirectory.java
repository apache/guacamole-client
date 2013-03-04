
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleClientException;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.service.ConnectionService;
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
import net.sourceforge.guacamole.net.auth.mysql.service.UserService;
import net.sourceforge.guacamole.net.auth.permission.ConnectionPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
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
     * Service for accessing users.
     */
    @Inject
    private UserService userService;

    /**
     * Service for accessing connections.
     */
    @Inject
    private ConnectionService connectionService;

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
     * Service for checking various permissions, which will be injected.
     */
    @Inject
    private PermissionCheckService permissionCheckService;

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

        // Get user
        MySQLUser user = userService.retrieveUser(identifier);

        // Verify access is granted
        permissionCheckService.verifyUserAccess(this.user_id,
                user.getUserID(),
                MySQLConstants.USER_READ);

        // Return user
        return userService.retrieveUser(identifier);

    }

    @Transactional
    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        return permissionCheckService.retrieveUsernames(user_id,
                MySQLConstants.USER_READ);
    }

    @Override
    @Transactional
    public void add(net.sourceforge.guacamole.net.auth.User object)
            throws GuacamoleException {

        String username = object.getUsername().trim();
        if(username.isEmpty())
            throw new GuacamoleClientException("The username cannot be blank.");

        // Verify current user has permission to create users
        permissionCheckService.verifySystemAccess(this.user_id,
                MySQLConstants.SYSTEM_USER_CREATE);
        Preconditions.checkNotNull(object);

        // Verify that no user already exists with this username.
        MySQLUser previousUser = userService.retrieveUser(username);
        if(previousUser != null)
            throw new GuacamoleClientException("That username is already in use.");

        // Create new user
        MySQLUser user = userService.createUser(username, object.getPassword());

        // Create permissions of new user in database
        createPermissions(user.getUserID(), object.getPermissions());

        // Give the current user full access to the newly created user.
        UserPermissionKey newUserPermission = new UserPermissionKey();
        newUserPermission.setUser_id(this.user_id);
        newUserPermission.setAffected_user_id(user.getUserID());

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
     * Add the given permissions to the given user.
     *
     * @param user_id The ID of the user whose permissions should be updated.
     * @param permissions The permissions to add.
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
        createUserPermissions(user_id, newUserPermissions);
        createConnectionPermissions(user_id, newConnectionPermissions);
        createSystemPermissions(user_id, newSystemPermissions);

    }



    /**
     * Remove the given permissions from the given user.
     *
     * @param user_id The ID of the user whose permissions should be updated.
     * @param permissions The permissions to remove.
     * @throws GuacamoleException If an error occurs while updating the
     *                            permissions of the given user.
     */
    private void removePermissions(int user_id, Set<Permission> permissions)
            throws GuacamoleException {

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
        deleteUserPermissions(user_id, removedUserPermissions);
        deleteConnectionPermissions(user_id, removedConnectionPermissions);
        deleteSystemPermissions(user_id, removedSystemPermissions);

    }

    /**
     * Create the given user permissions for the given user.
     *
     * @param user_id The ID of the user to change the permissions of.
     * @param permissions The new permissions the given user should have when
     *                    this operation completes.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is denied.
     */
    private void createUserPermissions(int user_id,
            Collection<UserPermission> permissions)
            throws GuacamoleException {

        // If no permissions given, stop now
        if(permissions.isEmpty())
            return;

        // Get list of administerable user IDs
        List<Integer> administerableUserIDs =
            permissionCheckService.retrieveUserIDs(this.user_id,
                MySQLConstants.USER_ADMINISTER);

        // Get set of usernames corresponding to administerable users
        Map<String, Integer> administerableUsers =
                userService.translateUsernames(administerableUserIDs);

        // Insert all given permissions
        for (UserPermission permission : permissions) {

            // Get original ID
            Integer affected_id =
                    administerableUsers.get(permission.getObjectIdentifier());

            // Verify that the user actually has permission to administrate
            // every one of these users
            if (affected_id == null)
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate user "
                    + permission.getObjectIdentifier());

            // Create new permission
            UserPermissionKey newPermission = new UserPermissionKey();
            newPermission.setUser_id(user_id);
            newPermission.setPermission(MySQLConstants.getUserConstant(permission.getType()));
            newPermission.setAffected_user_id(affected_id);
            userPermissionDAO.insert(newPermission);

         }

    }

    /**
     * Delete permissions having to do with users for a given user.
     *
     * @param user_id The ID of the user to change the permissions of.
     * @param permissions The permissions the given user should no longer have
     *                    when this operation completes.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is denied.
     */
    private void deleteUserPermissions(int user_id,
            Collection<UserPermission> permissions)
            throws GuacamoleException {

        // If no permissions given, stop now
        if(permissions.isEmpty())
            return;

        // Get list of administerable user IDs
        List<Integer> administerableUserIDs =
            permissionCheckService.retrieveUserIDs(this.user_id,
                MySQLConstants.USER_ADMINISTER);

        // Get set of usernames corresponding to administerable users
        Map<String, Integer> administerableUsers =
                userService.translateUsernames(administerableUserIDs);

        // Delete requested permissions
        for (UserPermission permission : permissions) {

            // Get original ID
            Integer affected_id =
                    administerableUsers.get(permission.getObjectIdentifier());

            // Verify that the user actually has permission to administrate
            // every one of these users
            if (affected_id == null)
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate user "
                    + permission.getObjectIdentifier());

            // Delete requested permission
            UserPermissionExample userPermissionExample = new UserPermissionExample();
            userPermissionExample.createCriteria()
                .andUser_idEqualTo(user_id)
                .andPermissionEqualTo(MySQLConstants.getUserConstant(permission.getType()))
                .andAffected_user_idEqualTo(affected_id);
            userPermissionDAO.deleteByExample(userPermissionExample);

        }

    }

    /**
     * Create any new permissions having to do with connections for a given
     * user.
     *
     * @param user_id The ID of the user to assign or remove permissions from.
     * @param permissions The new permissions the user should have after this
     *                    operation completes.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is deniedD
     */
    private void createConnectionPermissions(int user_id,
            Collection<ConnectionPermission> permissions)
            throws GuacamoleException {

        // If no permissions given, stop now
        if(permissions.isEmpty())
            return;

        // Get list of administerable connection IDs
        List<Integer> administerableConnectionIDs =
            permissionCheckService.retrieveConnectionIDs(this.user_id,
                MySQLConstants.CONNECTION_ADMINISTER);

        // Get set of names corresponding to administerable connections
        Map<String, Integer> administerableConnections =
                connectionService.translateNames(administerableConnectionIDs);

        // Insert all given permissions
        for (ConnectionPermission permission : permissions) {

            // Get original ID
            Integer connection_id =
                    administerableConnections.get(permission.getObjectIdentifier());

            // Throw exception if permission to administer this connection
            // is not granted
            if (connection_id == null)
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate connection "
                    + permission.getObjectIdentifier());


            // Create new permission
            ConnectionPermissionKey newPermission = new ConnectionPermissionKey();
            newPermission.setUser_id(user_id);
            newPermission.setPermission(MySQLConstants.getConnectionConstant(permission.getType()));
            newPermission.setConnection_id(connection_id);
            connectionPermissionDAO.insert(newPermission);

        }
    }

    /**
     * Delete permissions having to do with connections for a given user.
     *
     * @param user_id The ID of the user to change the permissions of.
     * @param permissions The permissions the given user should no longer have
     *                    when this operation completes.
     * @throws GuacamoleException If permission to alter the access permissions
     *                            of affected objects is denied.
     */
    private void deleteConnectionPermissions(int user_id,
            Collection<ConnectionPermission> permissions)
            throws GuacamoleException {

        // If no permissions given, stop now
        if(permissions.isEmpty())
            return;

        // Get list of administerable connection IDs
        List<Integer> administerableConnectionIDs =
            permissionCheckService.retrieveConnectionIDs(this.user_id,
                MySQLConstants.CONNECTION_ADMINISTER);

        // Get set of names corresponding to administerable connections
        Map<String, Integer> administerableConnections =
                connectionService.translateNames(administerableConnectionIDs);

        // Delete requested permissions
        for (ConnectionPermission permission : permissions) {

            // Get original ID
            Integer connection_id =
                    administerableConnections.get(permission.getObjectIdentifier());

            // Verify that the user actually has permission to administrate
            // every one of these connections
            if (connection_id == null)
                throw new GuacamoleSecurityException(
                      "User #" + this.user_id
                    + " does not have permission to administrate connection "
                    + permission.getObjectIdentifier());

            ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
            connectionPermissionExample.createCriteria()
                .andUser_idEqualTo(user_id)
                .andPermissionEqualTo(MySQLConstants.getConnectionConstant(permission.getType()))
                .andConnection_idEqualTo(connection_id);
            connectionPermissionDAO.deleteByExample(connectionPermissionExample);

        }

    }

    /**
     * Create any new system permissions for a given user. All permissions in
     * the given list will be inserted.
     *
     * @param user_id The ID of the user whose permissions should be updated.
     * @param permissions The new system permissions that the given user should
     *                    have when this operation completes.
     */
    private void createSystemPermissions(int user_id,
            Collection<SystemPermission> permissions) throws GuacamoleException {

        // If no permissions given, stop now
        if(permissions.isEmpty())
            return;

        // Only a system administrator can add system permissions.
        permissionCheckService.verifySystemAccess(
                this.user_id, SystemPermission.Type.ADMINISTER.name());

        // Insert all requested permissions
        for (SystemPermission permission : permissions) {
            
            // Insert permission
            SystemPermissionKey newSystemPermission = new SystemPermissionKey();
            newSystemPermission.setUser_id(user_id);
            newSystemPermission.setPermission(MySQLConstants.getSystemConstant(permission.getType()));
            systemPermissionDAO.insert(newSystemPermission);

        }

    }

    /**
     * Delete system permissions for a given user. All permissions in
     * the given list will be removed from the user.
     *
     * @param user_id The ID of the user whose permissions should be updated.
     * @param permissions The permissions the given user should no longer have
     *                    when this operation completes.
     */
    private void deleteSystemPermissions(int user_id,
            Collection<SystemPermission> permissions) {

        // If no permissions given, stop now
        if (permissions.isEmpty())
            return;

        // Build list of requested system permissions
        List<String> systemPermissionTypes = new ArrayList<String>();
        for (SystemPermission permission : permissions)
            systemPermissionTypes.add(MySQLConstants.getSystemConstant(permission.getType()));

        // Delete the requested system permissions for this user
        SystemPermissionExample systemPermissionExample = new SystemPermissionExample();
        systemPermissionExample.createCriteria().andUser_idEqualTo(user_id)
                .andPermissionIn(systemPermissionTypes);
        systemPermissionDAO.deleteByExample(systemPermissionExample);

    }

    @Override
    @Transactional
    public void update(net.sourceforge.guacamole.net.auth.User object)
            throws GuacamoleException {

        // If user not actually from this auth provider, we can't handle updated
        // permissions.
        if (!(object instanceof MySQLUser))
            throw new GuacamoleException("User not from database.");

        MySQLUser mySQLUser = (MySQLUser) object;

        // Validate permission to update this user is granted
        permissionCheckService.verifyUserAccess(this.user_id,
                mySQLUser.getUserID(),
                MySQLConstants.USER_UPDATE);

        // Update the user in the database
        userService.updateUser(mySQLUser);

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

        // Get user pending deletion
        MySQLUser user = userService.retrieveUser(identifier);

        // Validate current user has permission to remove the specified user
        permissionCheckService.verifyUserAccess(this.user_id,
                user.getUserID(),
                MySQLConstants.USER_DELETE);

        // Delete specified user
        userService.deleteUser(user.getUserID());

    }

}
