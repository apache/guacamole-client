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
package net.sourceforge.guacamole.net.auth.mysql.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnection;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConstants;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.Connection;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionKey;
import net.sourceforge.guacamole.net.auth.permission.ConnectionPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.net.auth.permission.UserPermission;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * A service to retrieve information about what objects a user has permission to.
 * @author James Muehlner
 */
public class PermissionCheckService {

    @Inject
    private UserService userService;

    @Inject
    private ConnectionMapper connectionDAO;

    @Inject
    private UserPermissionMapper userPermissionDAO;

    @Inject
    private ConnectionPermissionMapper connectionPermissionDAO;

    @Inject
    private SystemPermissionMapper systemPermissionDAO;

    @Inject
    private Provider<MySQLUser> mySQLUserProvider;

    @Inject
    private Provider<MySQLConnection> mySQLConnectionProvider;

    /**
     * Verifies that the user has read access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUserID
     * @throws GuacamoleSecurityException
     */
    public void verifyUserReadAccess(int userID, int affectedUserID) throws GuacamoleSecurityException {
        if(!checkUserReadAccess(userID, affectedUserID))
            throw new GuacamoleSecurityException("User " + userID + " does not have read access to user " + affectedUserID);
    }

    /**
     * Verifies that the user has update access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUserID
     * @throws GuacamoleSecurityException
     */
    public void verifyUserUpdateAccess(int userID, int affectedUserID) throws GuacamoleSecurityException {
        if(!checkUserUpdateAccess(userID, affectedUserID))
            throw new GuacamoleSecurityException("User " + userID + " does not have update access to user " + affectedUserID);
    }

    /**
     * Verifies that the user has delete access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUserID
     * @throws GuacamoleSecurityException
     */
    public void verifyUserDeleteAccess(int userID, int affectedUserID) throws GuacamoleSecurityException {
        if(!checkUserDeleteAccess(userID, affectedUserID))
            throw new GuacamoleSecurityException("User " + userID + " does not have delete access to user " + affectedUserID);
    }

    /**
     * Verifies that the user has administer access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUserID
     * @throws GuacamoleSecurityException
     */
    public void verifyUserAdministerAccess(int userID, int affectedUserID) throws GuacamoleSecurityException {
        if(!checkUserAdministerAccess(userID, affectedUserID))
            throw new GuacamoleSecurityException("User " + userID + " does not have administer access to user " + affectedUserID);
    }

    /**
     * Verifies that the user has read access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUsername
     * @throws GuacamoleSecurityException
     */
    public void verifyUserReadAccess(int userID, String affectedUsername) throws GuacamoleSecurityException {
        if(!checkUserReadAccess(userID, affectedUsername))
            throw new GuacamoleSecurityException("User " + userID + " does not have read access to user '" + affectedUsername + "'");
    }

    /**
     * Verifies that the user has update access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUsername
     * @throws GuacamoleSecurityException
     */
    public void verifyUserUpdateAccess(int userID, String affectedUsername) throws GuacamoleSecurityException {
        if(!checkUserUpdateAccess(userID, affectedUsername))
            throw new GuacamoleSecurityException("User " + userID + " does not have update access to user '" + affectedUsername + "'");
    }

    /**
     * Verifies that the user has delete access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUsername
     * @throws GuacamoleSecurityException
     */
    public void verifyUserDeleteAccess(int userID, String affectedUsername) throws GuacamoleSecurityException {
        if(!checkUserDeleteAccess(userID, affectedUsername))
            throw new GuacamoleSecurityException("User " + userID + " does not have delete access to user '" + affectedUsername + "'");
    }

    /**
     * Verifies that the user has administer access to the given user. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedUsername
     * @throws GuacamoleSecurityException
     */
    public void verifyUserAdministerAccess(int userID, String affectedUsername) throws GuacamoleSecurityException {
        if(!checkUserAdministerAccess(userID, affectedUsername))
            throw new GuacamoleSecurityException("User " + userID + " does not have administer access to user '" + affectedUsername + "'");
    }

    /**
     * Checks if the user has read access to the given user.
     * @param userID
     * @param affectedUserID
     * @return true if the user has access to this user.
     */
    public boolean checkUserReadAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_READ);
    }

    /**
     * Checks if the user has update access to the given user.
     * @param userID
     * @param affectedUserID
     * @return true if the user has access to this user.
     */
    public boolean checkUserUpdateAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_UPDATE);
    }

    /**
     * Checks if the user has delete access to the given user.
     * @param userID
     * @param affectedUserID
     * @return true if the user has access to this user.
     */
    public boolean checkUserDeleteAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_DELETE);
    }

    /**
     * Checks if the user has administer access to the given user.
     * @param userID
     * @param affectedUserID
     * @return true if the user has access to this user.
     */
    public boolean checkUserAdministerAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_ADMINISTER);
    }

    /**
     * Checks if the user has read access to the given user.
     * @param userID
     * @param affectedUsername
     * @return true if the user has access to this user.
     */
    public boolean checkUserReadAccess(int userID, String affectedUsername) {
        return checkUserAccess(userID, affectedUsername, MySQLConstants.USER_READ);
    }

    /**
     * Checks if the user has update access to the given user.
     * @param userID
     * @param affectedUsername
     * @return true if the user has access to this user.
     */
    public boolean checkUserUpdateAccess(int userID, String affectedUsername) {
        return checkUserAccess(userID, affectedUsername, MySQLConstants.USER_UPDATE);
    }

    /**
     * Checks if the user has delete access to the given user.
     * @param userID
     * @param affectedUsername
     * @return true if the user has access to this user.
     */
    public boolean checkUserDeleteAccess(int userID, String affectedUsername) {
        return checkUserAccess(userID, affectedUsername, MySQLConstants.USER_DELETE);
    }

    /**
     * Checks if the user has administer access to the given user.
     * @param userID
     * @param affectedUsername
     * @return true if the user has access to this user.
     */
    public boolean checkUserAdministerAccess(int userID, String affectedUsername) {
        return checkUserAccess(userID, affectedUsername, MySQLConstants.USER_ADMINISTER);
    }

    /**
     * Check if the user has the selected type of access to the affected user.
     * @param userID
     * @param affectedUsername
     * @param permissionType
     * @return
     */
    private boolean checkUserAccess(int userID, String affectedUsername, String permissionType) {
        MySQLUser affectedUser = userService.retrieveUser(affectedUsername);
        if(affectedUser != null)
            return checkUserAccess(userID, affectedUser.getUserID(), permissionType);

        return false;
    }

    /**
     * Check if the user has the selected type of access to the affected user.
     * @param userID
     * @param affectedUserID
     * @param permissionType
     * @return
     */
    private boolean checkUserAccess(int userID, Integer affectedUserID, String permissionType) {
        UserPermissionExample example = new UserPermissionExample();
        example.createCriteria().andUser_idEqualTo(userID).andAffected_user_idEqualTo(affectedUserID).andPermissionEqualTo(permissionType);
        int count = userPermissionDAO.countByExample(example);
        return count > 0;
    }

    /**
     * Find the list of all user IDs a user has permission to administer.
     * @param userID
     * @return the list of all user IDs this user has administer access to
     */
    public Set<Integer> getAdministerableUserIDs(int userID) {
        return getUserIDs(userID, MySQLConstants.USER_ADMINISTER);
    }

    /**
     * Find the list of all user IDs a user has permission to delete.
     * @param userID
     * @return the list of all user IDs this user has delete access to
     */
    public Set<Integer> getDeletableUserIDs(int userID) {
        return getUserIDs(userID, MySQLConstants.USER_DELETE);
    }

    /**
     * Find the list of all user IDs a user has permission to write.
     * @param userID
     * @return the list of all user IDs this user has write access to
     */
    public Set<Integer> getUpdateableUserIDs(int userID) {
        return getUserIDs(userID, MySQLConstants.USER_UPDATE);
    }

    /**
     * Find the list of all user IDs a user has permission to read.
     * @param userID
     * @return the list of all user IDs this user has read access to
     */
    public Set<Integer> getReadableUserIDs(int userID) {
        return getUserIDs(userID, MySQLConstants.USER_READ);
    }

    /**
     * Find the list of all users a user has permission to administer.
     * @param userID
     * @return the list of all users this user has administer access to
     */
    public Set<MySQLUser> getAdministerableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_ADMINISTER);
    }

    /**
     * Find the list of all users a user has permission to delete.
     * @param userID
     * @return the list of all users this user has delete access to
     */
    public Set<MySQLUser> getDeletableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_DELETE);
    }

    /**
     * Find the list of all users a user has permission to write.
     * @param userID
     * @return the list of all users this user has write access to
     */
    public Set<MySQLUser> getUpdateableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_UPDATE);
    }

    /**
     * Find the list of all users a user has permission to read.
     * @param userID
     * @return the list of all users this user read has access to
     */
    public Set<MySQLUser> getReadableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_READ);
    }

    /**
     * Find the list of all users a user has permission to.
     * The access type is defined by permissionType.
     * @param userID
     * @param permissionType
     * @return the list of all users this user has access to
     */
    @Deprecated /* FIXME: Totally useless (we only ever need usernames, and querying ALL USER DATA will take ages) */
    private Set<MySQLUser> getUsers(int userID, String permissionType) {

        // Get all IDs of all users that the given user can perform the given
        // operation on
        Set<Integer> affectedUserIDs = getUserIDs(userID, permissionType);

        // If no affected users at all, return empty set
        if (affectedUserIDs.isEmpty())
            return Collections.EMPTY_SET;

        // Query corresponding user data for each retrieved ID
        return new HashSet<MySQLUser>(userService.retrieveUsersByID(
                Lists.newArrayList(affectedUserIDs)));

    }

    /**
     * Find the list of the IDs of all users a user has permission to.
     * The access type is defined by permissionType.
     * @param userID
     * @param permissionType
     * @return the list of all user IDs this user has access to
     */
    private Set<Integer> getUserIDs(int userID, String permissionType) {
        Set<Integer> userIDs = new HashSet<Integer>();
        UserPermissionExample example = new UserPermissionExample();
        example.createCriteria().andUser_idEqualTo(userID).andPermissionEqualTo(permissionType);
        List<UserPermissionKey> userPermissions = userPermissionDAO.selectByExample(example);
        for(UserPermissionKey permission : userPermissions)
            userIDs.add(permission.getAffected_user_id());

        return userIDs;
    }

    /**
     * Verifies that the user has read access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionID
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionReadAccess(int userID, int affectedConnectionID) throws GuacamoleSecurityException {
        if(!checkConnectionReadAccess(userID, affectedConnectionID))
            throw new GuacamoleSecurityException("User " + userID + " does not have read access to connection " + affectedConnectionID);
    }

    /**
     * Verifies that the user has update access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionID
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionUpdateAccess(int userID, int affectedConnectionID) throws GuacamoleSecurityException {
        if(!checkConnectionUpdateAccess(userID, affectedConnectionID))
            throw new GuacamoleSecurityException("User " + userID + " does not have update access to connection " + affectedConnectionID);
    }

    /**
     * Verifies that the user has delete access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionID
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionDeleteAccess(int userID, int affectedConnectionID) throws GuacamoleSecurityException {
        if(!checkConnectionDeleteAccess(userID, affectedConnectionID))
            throw new GuacamoleSecurityException("User " + userID + " does not have delete access to connection " + affectedConnectionID);
    }

    /**
     * Verifies that the user has administer access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionID
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionAdministerAccess(int userID, int affectedConnectionID) throws GuacamoleSecurityException {
        if(!checkConnectionAdministerAccess(userID, affectedConnectionID))
            throw new GuacamoleSecurityException("User " + userID + " does not have administer access to connection " + affectedConnectionID);
    }

    /**
     * Verifies that the user has read access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionName
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionReadAccess(int userID, String affectedConnectionName) throws GuacamoleSecurityException {
        if(!checkConnectionReadAccess(userID, affectedConnectionName))
            throw new GuacamoleSecurityException("User " + userID + " does not have read access to connection '" + affectedConnectionName + "'");
    }

    /**
     * Verifies that the user has update access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionName
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionUpdateAccess(int userID, String affectedConnectionName) throws GuacamoleSecurityException {
        if(!checkConnectionUpdateAccess(userID, affectedConnectionName))
            throw new GuacamoleSecurityException("User " + userID + " does not have update access to connection '" + affectedConnectionName + "'");
    }

    /**
     * Verifies that the user has delete access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionName
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionDeleteAccess(int userID, String affectedConnectionName) throws GuacamoleSecurityException {
        if(!checkConnectionDeleteAccess(userID, affectedConnectionName))
            throw new GuacamoleSecurityException("User " + userID + " does not have delete access to connection '" + affectedConnectionName + "'");
    }

    /**
     * Verifies that the user has administer access to the given connection. If not, throws a GuacamoleSecurityException.
     * @param userID
     * @param affectedConnectionName
     * @throws GuacamoleSecurityException
     */
    public void verifyConnectionAdministerAccess(int userID, String affectedConnectionName) throws GuacamoleSecurityException {
        if(!checkConnectionAdministerAccess(userID, affectedConnectionName))
            throw new GuacamoleSecurityException("User " + userID + " does not have administer access to connection '" + affectedConnectionName + "'");
    }

    /**
     * Checks if the user has read access to the given connection.
     * @param userID
     * @param affectedConnectionID
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionReadAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_READ);
    }

    /**
     * Checks if the user has update access to the given connection.
     * @param userID
     * @param affectedConnectionID
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionUpdateAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_UPDATE);
    }

    /**
     * Checks if the user has delete access to the given connection.
     * @param userID
     * @param affectedConnectionID
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionDeleteAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_DELETE);
    }

    /**
     * Checks if the user has administer access to the given connection.
     * @param userID
     * @param affectedConnectionID
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionAdministerAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_ADMINISTER);
    }

    /**
     * Checks if the user has read access to the given connection.
     * @param userID
     * @param affectedConnectionName
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionReadAccess(int userID, String affectedConnectionName) {
        return checkConnectionAccess(userID, affectedConnectionName, MySQLConstants.CONNECTION_READ);
    }

    /**
     * Checks if the user has update access to the given connection.
     * @param userID
     * @param affectedConnectionName
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionUpdateAccess(int userID, String affectedConnectionName) {
        return checkConnectionAccess(userID, affectedConnectionName, MySQLConstants.CONNECTION_UPDATE);
    }

    /**
     * Checks if the user has delete access to the given connection.
     * @param userID
     * @param affectedConnectionID
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionDeleteAccess(int userID, String affectedConnectionname) {
        return checkConnectionAccess(userID, affectedConnectionname, MySQLConstants.CONNECTION_DELETE);
    }

    /**
     * Checks if the user has administer access to the given connection.
     * @param userID
     * @param affectedConnectionName
     * @return true if the user has access to this connection.
     */
    public boolean checkConnectionAdministerAccess(int userID, String affectedConnectionName) {
        return checkConnectionAccess(userID, affectedConnectionName, MySQLConstants.CONNECTION_ADMINISTER);
    }

    /**
     * Check if the user has the selected type of access to the affected connection.
     * @param connectionID
     * @param affectedConnectionname
     * @param permissionType
     * @return
     */
    private boolean checkConnectionAccess(int userID, String affectedConnectionName, String permissionType) {
        Connection connection = getConnection(affectedConnectionName);
        if(connection != null)
            return checkConnectionAccess(userID, connection.getConnection_id(), permissionType);

        return false;
    }

    /**
     * Check if the user has the selected type of access to the affected connection.
     * @param connectionID
     * @param affectedConnectionID
     * @param permissionType
     * @return
     */
    private boolean checkConnectionAccess(int userID, Integer affectedConnectionID, String permissionType) {
        ConnectionPermissionExample example = new ConnectionPermissionExample();
        example.createCriteria().andUser_idEqualTo(userID).andConnection_idEqualTo(affectedConnectionID).andPermissionEqualTo(permissionType);
        int count = connectionPermissionDAO.countByExample(example);
        return count > 0;
    }

    /**
     * Find the list of all connection IDs a user has permission to administer.
     * @param userID
     * @return the list of all connection IDs this user has administer access to
     */
    public Set<Integer> getAdministerableConnectionIDs(int userID) {
        return getConnectionIDs(userID, MySQLConstants.CONNECTION_ADMINISTER);
    }

    /**
     * Find the list of all connection IDs a user has permission to delete.
     * @param userID
     * @return the list of all connection IDs this user has delete access to
     */
    public Set<Integer> getDeletableConnectionIDs(int userID) {
        return getConnectionIDs(userID, MySQLConstants.CONNECTION_DELETE);
    }

    /**
     * Find the list of all connection IDs a user has permission to write.
     * @param userID
     * @return the list of all connection IDs this user has write access to
     */
    public Set<Integer> getUpdateableConnectionIDs(int userID) {
        return getConnectionIDs(userID, MySQLConstants.CONNECTION_UPDATE);
    }

    /**
     * Find the list of all connection IDs a user has permission to read.
     * @param userID
     * @return the list of all connection IDs this user has ready access to
     */
    public Set<Integer> getReadableConnectionIDs(int userID) {
        return getConnectionIDs(userID, MySQLConstants.CONNECTION_READ);
    }

    /**
     * Find the list of all connections a user has permission to administer.
     * @param userID
     * @return the list of all connections this user has administer access to
     */
    public Set<MySQLConnection> getAdministerableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_ADMINISTER);
    }

    /**
     * Find the list of all connections a user has permission to delete.
     * @param userID
     * @return the list of all connections this user has delete access to
     */
    public Set<MySQLConnection> getDeletableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_DELETE);
    }

    /**
     * Find the list of all connections a user has permission to write.
     * @param userID
     * @return the list of all connections this user has write access to
     */
    public Set<MySQLConnection> getUpdateableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_UPDATE);
    }

    /**
     * Find the list of all connections a user has permission to read.
     * @param userID
     * @return the list of all connections this user has read access to
     */
    public Set<MySQLConnection> getReadableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_READ);
    }

    /**
     * Find the list of all connections a user has permission to.
     * The access type is defined by permissionType.
     * @param connectionID
     * @param permissionType
     * @return the list of all connections this user has access to
     */
    @Deprecated /* FIXME: Totally useless (we only ever need identifiers, and querying ALL CONNECTION DATA will take ages) */
    private Set<MySQLConnection> getConnections(int userID, String permissionType) {

        // If connections available, query them
        Set<Integer> affectedConnectionIDs = getConnectionIDs(userID, permissionType);
        if (!affectedConnectionIDs.isEmpty()) {

            // Query available connections
            ConnectionExample example = new ConnectionExample();
            example.createCriteria().andConnection_idIn(Lists.newArrayList(affectedConnectionIDs));
            List<Connection> connectionDBOjects = connectionDAO.selectByExample(example);

            // Add connections to final set
            Set<MySQLConnection> affectedConnections = new HashSet<MySQLConnection>();
            for(Connection affectedConnection : connectionDBOjects) {
                MySQLConnection mySQLConnection = mySQLConnectionProvider.get();
                mySQLConnection.init(
                    affectedConnection.getConnection_id(),
                    affectedConnection.getConnection_name(),
                    new GuacamoleConfiguration(),
                    Collections.EMPTY_LIST
                );
                affectedConnections.add(mySQLConnection);
            }

            return affectedConnections;
        }

        // Otherwise, no connections available
        return Collections.EMPTY_SET;

    }

    /**
     * Find the list of the IDs of all connections a user has permission to.
     * The access type is defined by permissionType.
     * @param connectionID
     * @param permissionType
     * @return the list of all connection IDs this user has access to
     */
    private Set<Integer> getConnectionIDs(int userID, String permissionType) {
        Set<Integer> connectionIDs = new HashSet<Integer>();
        ConnectionPermissionExample example = new ConnectionPermissionExample();
        example.createCriteria().andUser_idEqualTo(userID).andPermissionEqualTo(permissionType);
        List<ConnectionPermissionKey> connectionPermissions = connectionPermissionDAO.selectByExample(example);
        for(ConnectionPermissionKey permission : connectionPermissions)
            connectionIDs.add(permission.getConnection_id());

        return connectionIDs;
    }

    public void verifyCreateUserPermission(int userID) throws GuacamoleSecurityException {
        if(!checkCreateUserPermission(userID))
            throw new GuacamoleSecurityException("User " + userID + " does not have permission to create users.");
    }

    public void verifyCreateConnectionPermission(int userID) throws GuacamoleSecurityException {
        if(!checkCreateConnectionPermission(userID))
            throw new GuacamoleSecurityException("User " + userID + " does not have permission to create connections.");
    }

    /**
     * Check if the user has the permission to create users.
     * @param userID
     * @return
     */
    public boolean checkCreateUserPermission(int userID) {
        return checkSystemPermission(userID, MySQLConstants.SYSTEM_USER_CREATE);
    }

    /**
     * Check if the user has the permission to create connections.
     * @param userID
     * @return
     */
    public boolean checkCreateConnectionPermission(int userID) {
        return checkSystemPermission(userID, MySQLConstants.SYSTEM_CONNECTION_CREATE);
    }

    /**
     * Check if the user has the selected system permission.
     * @param userID
     * @param systemPermissionType
     * @return
     */
    private boolean checkSystemPermission(int userID, String systemPermissionType) {
        SystemPermissionExample example = new SystemPermissionExample();
        example.createCriteria().andUser_idEqualTo(userID).andPermissionEqualTo(systemPermissionType);
        int count = systemPermissionDAO.countByExample(example);
        return count > 0;
    }

    /**
     * Get a connection object by name.
     * @param name
     * @return
     */
    private Connection getConnection(String name) {
        ConnectionExample example = new ConnectionExample();
        example.createCriteria().andConnection_nameEqualTo(name);
        List<Connection> connections = connectionDAO.selectByExample(example);
        if(connections.isEmpty())
            return null;

        return connections.get(0);
    }

    /**
     * Get all permissions a given user has.
     * @param userID
     * @return all permissions a user has.
     */
    public Set<Permission> getAllPermissions(int userID) {
        Set<Permission> allPermissions = new HashSet<Permission>();

        // First, user permissions
        UserPermissionExample userPermissionExample = new UserPermissionExample();
        userPermissionExample.createCriteria().andUser_idEqualTo(userID);
        List<UserPermissionKey> userPermissions =
                userPermissionDAO.selectByExample(userPermissionExample);

        // If user permissions present, add permissions
        if (!userPermissions.isEmpty()) {

            // Get list of affected user IDs
            List<Integer> affectedUserIDs = new ArrayList<Integer>();
            for(UserPermissionKey userPermission : userPermissions)
                affectedUserIDs.add(userPermission.getAffected_user_id());

            // Query all affected users, store in map indexed by user ID
            List<MySQLUser> users = userService.retrieveUsersByID(affectedUserIDs);
            Map<Integer, MySQLUser> userMap = new HashMap<Integer, MySQLUser>();
            for (MySQLUser user : users)
                userMap.put(user.getUserID(), user);

            // Add user permissions
            for(UserPermissionKey userPermission : userPermissions) {
                MySQLUser affectedUser = userMap.get(userPermission.getAffected_user_id());
                UserPermission newPermission = new UserPermission(
                    UserPermission.Type.valueOf(userPermission.getPermission()),
                    affectedUser.getUsername()
                );
                allPermissions.add(newPermission);
            }

        }

        // Secondly, connection permissions
        ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
        connectionPermissionExample.createCriteria().andUser_idEqualTo(userID);
        List<ConnectionPermissionKey> connectionPermissions =
                connectionPermissionDAO.selectByExample(connectionPermissionExample);

        // If connection permissions present, add permissions
        if (!connectionPermissions.isEmpty()) {

            // Get list of affected connection IDs
            List<Integer> affectedConnectionIDs = new ArrayList<Integer>();
            for(ConnectionPermissionKey connectionPermission : connectionPermissions)
                affectedConnectionIDs.add(connectionPermission.getConnection_id());

            // Query connections, store in map indexed by connection ID
            ConnectionExample connectionExample = new ConnectionExample();
            connectionExample.createCriteria().andConnection_idIn(affectedConnectionIDs);
            List<Connection> connections = connectionDAO.selectByExample(connectionExample);
            Map<Integer, Connection> connectionMap = new HashMap<Integer, Connection>();
            for(Connection connection : connections)
                connectionMap.put(connection.getConnection_id(), connection);

            // Add connection permissions
            for(ConnectionPermissionKey connectionPermission : connectionPermissions) {
                Connection affectedConnection = connectionMap.get(connectionPermission.getConnection_id());
                ConnectionPermission newPermission = new ConnectionPermission(
                    ConnectionPermission.Type.valueOf(connectionPermission.getPermission()),
                    affectedConnection.getConnection_name()
                );
                allPermissions.add(newPermission);
            }

        }

        // And finally, system permissions
        SystemPermissionExample systemPermissionExample = new SystemPermissionExample();
        systemPermissionExample.createCriteria().andUser_idEqualTo(userID);
        List<SystemPermissionKey> systemPermissions =
                systemPermissionDAO.selectByExample(systemPermissionExample);
        for(SystemPermissionKey systemPermission : systemPermissions) {

            // User creation permission
            if(systemPermission.getPermission().equals(MySQLConstants.SYSTEM_USER_CREATE))
                allPermissions.add(new SystemPermission(SystemPermission.Type.CREATE_USER));

            // System creation permission
            else if(systemPermission.getPermission().equals(MySQLConstants.SYSTEM_CONNECTION_CREATE))
                allPermissions.add(new SystemPermission(SystemPermission.Type.CREATE_CONNECTION));

        }

        return allPermissions;
    }
}
