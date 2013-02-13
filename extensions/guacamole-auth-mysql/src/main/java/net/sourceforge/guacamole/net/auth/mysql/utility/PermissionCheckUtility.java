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
package net.sourceforge.guacamole.net.auth.mysql.utility;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnection;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConstants;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
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
import net.sourceforge.guacamole.net.auth.permission.ConnectionDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.ConnectionPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.net.auth.permission.UserDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.UserPermission;

/**
 * A utility to retrieve information about what objects a user has permission to.
 * @author James Muehlner
 */
public class PermissionCheckUtility {
    
    @Inject
    UserMapper userDAO;
    
    @Inject
    ConnectionMapper connectionDAO;
    
    @Inject
    UserPermissionMapper userPermissionDAO;
    
    @Inject
    ConnectionPermissionMapper connectionPermissionDAO;
    
    @Inject
    SystemPermissionMapper systemPermissionDAO;
    
    @Inject
    Provider<MySQLUser> mySQLUserProvider;
    
    @Inject
    Provider<MySQLConnection> mySQLConnectionProvider;
    
    public boolean checkUserReadAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_READ);
    }
    
    public boolean checkUserUpdateAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_UPDATE);
    }
    
    public boolean checkUserDeleteAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_DELETE);
    }
    
    public boolean checkUserAdministerAccess(int userID, int affectedUserID) {
        return checkUserAccess(userID, affectedUserID, MySQLConstants.USER_ADMINISTER);
    }
    
    public boolean checkUserReadAccess(int userID, String affectedUsername) {
        return checkUserAccess(userID, affectedUsername, MySQLConstants.USER_READ);
    }
    
    public boolean checkUserUpdateAccess(int userID, String affectedUsername) {
        return checkUserAccess(userID, affectedUsername, MySQLConstants.USER_UPDATE);
    }
    
    public boolean checkUserDeleteAccess(int userID, String affectedUsername) {
        return checkUserAccess(userID, affectedUsername, MySQLConstants.USER_DELETE);
    }
    
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
        User affectedUser = getUser(affectedUsername);
        if(affectedUser != null)
            return checkUserAccess(userID, affectedUser.getUser_id(), permissionType);
        
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
     * Find the list of all users a user has permission to administer.
     * @param userID
     * @return the list of all users this user has administer access to
     */
    public List<MySQLUser> getAdministerableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_ADMINISTER);
    }
    
    /**
     * Find the list of all users a user has permission to delete.
     * @param userID
     * @return the list of all users this user has delete access to
     */
    public List<MySQLUser> getDeletableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_DELETE);
    }
    
    /**
     * Find the list of all users a user has permission to write.
     * @param userID
     * @return the list of all users this user has write access to
     */
    public List<MySQLUser> getUpdateableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_UPDATE);
    }
    
    /**
     * Find the list of all users a user has permission to read.
     * @param userID
     * @return the list of all users this user read has access to
     */
    public List<MySQLUser> getReadableUsers(int userID) {
        return getUsers(userID, MySQLConstants.USER_READ);
    }
    
    /**
     * Find the list of all users a user has permission to.
     * The access type is defined by permissionType.
     * @param userID
     * @param permissionType
     * @return the list of all users this user has access to
     */
    private List<MySQLUser> getUsers(int userID, String permissionType) {
        List<Integer> affectedUserIDs = getUserIDs(userID, permissionType);
        UserExample example = new UserExample();
        example.createCriteria().andUser_idIn(affectedUserIDs);
        List<UserWithBLOBs> userDBOjects = userDAO.selectByExampleWithBLOBs(example);
        List<MySQLUser> affectedUsers = new ArrayList<MySQLUser>();
        for(UserWithBLOBs affectedUser : userDBOjects) {
            MySQLUser mySQLUser = mySQLUserProvider.get();
            mySQLUser.init(affectedUser);
            affectedUsers.add(mySQLUser);
        }
        
        return affectedUsers;
    }
    
    /**
     * Find the list of the IDs of all users a user has permission to.
     * The access type is defined by permissionType.
     * @param userID
     * @param permissionType
     * @return the list of all user IDs this user has access to
     */
    private List<Integer> getUserIDs(int userID, String permissionType) {
        List<Integer> userIDs = new ArrayList<Integer>();
        UserPermissionExample example = new UserPermissionExample();
        example.createCriteria().andUser_idEqualTo(userID).andPermissionEqualTo(permissionType);
        List<UserPermissionKey> userPermissions = userPermissionDAO.selectByExample(example);
        for(UserPermissionKey permission : userPermissions)
            userIDs.add(permission.getAffected_user_id());
        
        return userIDs;
    }
    
    public boolean checkConnectionReadAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_READ);
    }
    
    public boolean checkConnectionUpdateAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_UPDATE);
    }
    
    public boolean checkConnectionDeleteAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_DELETE);
    }
    
    public boolean checkConnectionAdministerAccess(int userID, int affectedConnectionID) {
        return checkConnectionAccess(userID, affectedConnectionID, MySQLConstants.CONNECTION_ADMINISTER);
    }
    
    public boolean checkConnectionReadAccess(int userID, String affectedConnectionname) {
        return checkConnectionAccess(userID, affectedConnectionname, MySQLConstants.CONNECTION_READ);
    }
    
    public boolean checkConnectionUpdateAccess(int userID, String affectedConnectionname) {
        return checkConnectionAccess(userID, affectedConnectionname, MySQLConstants.CONNECTION_UPDATE);
    }
    
    public boolean checkConnectionDeleteAccess(int userID, String affectedConnectionname) {
        return checkConnectionAccess(userID, affectedConnectionname, MySQLConstants.CONNECTION_DELETE);
    }
    
    public boolean checkConnectionAdministerAccess(int userID, String affectedConnectionname) {
        return checkConnectionAccess(userID, affectedConnectionname, MySQLConstants.CONNECTION_ADMINISTER);
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
     * Find the list of all connections a user has permission to administer.
     * @param connectionID
     * @return the list of all connections this connection has administer access to
     */
    public List<MySQLConnection> getAdministerableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_ADMINISTER);
    }
    
    /**
     * Find the list of all connections a user has permission to delete.
     * @param connectionID
     * @return the list of all connections this connection has delete access to
     */
    public List<MySQLConnection> getDeletableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_DELETE);
    }
    
    /**
     * Find the list of all connections a user has permission to write.
     * @param connectionID
     * @return the list of all connections this connection has write access to
     */
    public List<MySQLConnection> getUpdateableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_UPDATE);
    }
    
    /**
     * Find the list of all connections a user has permission to read.
     * @param connectionID
     * @return the list of all connections this connection read has access to
     */
    public List<MySQLConnection> getReadableConnections(int userID) {
        return getConnections(userID, MySQLConstants.CONNECTION_READ);
    }
    
    /**
     * Find the list of all connections a user has permission to.
     * The access type is defined by permissionType.
     * @param connectionID
     * @param permissionType
     * @return the list of all connections this user has access to
     */
    private List<MySQLConnection> getConnections(int userID, String permissionType) {
        List<Integer> affectedConnectionIDs = getConnectionIDs(userID, permissionType);
        ConnectionExample example = new ConnectionExample();
        example.createCriteria().andConnection_idIn(affectedConnectionIDs);
        List<Connection> connectionDBOjects = connectionDAO.selectByExample(example);
        List<MySQLConnection> affectedConnections = new ArrayList<MySQLConnection>();
        for(Connection affectedConnection : connectionDBOjects) {
            MySQLConnection mySQLConnection = mySQLConnectionProvider.get();
            mySQLConnection.init(affectedConnection);
            affectedConnections.add(mySQLConnection);
        }
        
        return affectedConnections;
    }
    
    /**
     * Find the list of the IDs of all connections a user has permission to.
     * The access type is defined by permissionType.
     * @param connectionID
     * @param permissionType
     * @return the list of all connection IDs this user has access to
     */
    private List<Integer> getConnectionIDs(int userID, String permissionType) {
        List<Integer> connectionIDs = new ArrayList<Integer>();
        ConnectionPermissionExample example = new ConnectionPermissionExample();
        example.createCriteria().andUser_idEqualTo(userID).andPermissionEqualTo(permissionType);
        List<ConnectionPermissionKey> connectionPermissions = connectionPermissionDAO.selectByExample(example);
        for(ConnectionPermissionKey permission : connectionPermissions)
            connectionIDs.add(permission.getConnection_id());
        
        return connectionIDs;
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
     * Get a user object by username.
     * @param userName
     * @return 
     */
    private User getUser(String username) {
        UserExample example = new UserExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<User> users = userDAO.selectByExample(example);
        if(users.isEmpty())
            return null;
        
        return users.get(0);
    }
    
    /**
     * Get all permissions a given user has.
     * @param userID
     * @return all permissions a user has. 
     */
    public Set<Permission> getAllPermissions(int userID) {
        Set<Permission> allPermissions = new HashSet<Permission>();
        
        // first, user permissions
        UserPermissionExample userPermissionExample = new UserPermissionExample();
        userPermissionExample.createCriteria().andUser_idEqualTo(userID);
        List<UserPermissionKey> userPermissions = userPermissionDAO.selectByExample(userPermissionExample);
        List<Integer> affectedUserIDs = new ArrayList<Integer>();
        for(UserPermissionKey userPermission : userPermissions) {
            affectedUserIDs.add(userPermission.getAffected_user_id());
        }
        
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUser_idIn(affectedUserIDs);
        List<User> users = userDAO.selectByExample(userExample);
        Map<Integer, User> userMap = new HashMap<Integer, User>();
        for(User user : users) {
            userMap.put(user.getUser_id(), user);
        }
        
        for(UserPermissionKey userPermission : userPermissions) {
            User affectedUser = userMap.get(userPermission.getAffected_user_id());
            UserPermission newPermission = new UserPermission(
                UserPermission.Type.valueOf(userPermission.getPermission()),
                affectedUser.getUsername()
            );
            allPermissions.add(newPermission);
        }
        
        //secondly, connection permissions
        ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
        connectionPermissionExample.createCriteria().andUser_idEqualTo(userID);
        List<ConnectionPermissionKey> connectionPermissions = connectionPermissionDAO.selectByExample(connectionPermissionExample);
        List<Integer> affectedConnectionIDs = new ArrayList<Integer>();
        for(ConnectionPermissionKey connectionPermission : connectionPermissions) {
            affectedConnectionIDs.add(connectionPermission.getConnection_id());
        }
        
        ConnectionExample connectionExample = new ConnectionExample();
        connectionExample.createCriteria().andConnection_idIn(affectedConnectionIDs);
        List<Connection> connections = connectionDAO.selectByExample(connectionExample);
        Map<Integer, Connection> connectionMap = new HashMap<Integer, Connection>();
        for(Connection connection : connections) {
            connectionMap.put(connection.getConnection_id(), connection);
        }
        
        for(ConnectionPermissionKey connectionPermission : connectionPermissions) {
            Connection affectedConnection = connectionMap.get(connectionPermission.getConnection_id());
            ConnectionPermission newPermission = new ConnectionPermission(
                ConnectionPermission.Type.valueOf(connectionPermission.getPermission()),
                affectedConnection.getConnection_name()
            );
            allPermissions.add(newPermission);
        }
        
        //and finally, system permissions
        SystemPermissionExample systemPermissionExample = new SystemPermissionExample();
        systemPermissionExample.createCriteria().andUser_idEqualTo(userID);
        List<SystemPermissionKey> systemPermissions = systemPermissionDAO.selectByExample(systemPermissionExample);
        for(SystemPermissionKey systemPermission : systemPermissions) {
            SystemPermission newPermission = null;
            if(systemPermission.getPermission().equals(MySQLConstants.SYSTEM_USER_CREATE))
                newPermission = new UserDirectoryPermission(UserDirectoryPermission.Type.CREATE);
            else if(systemPermission.getPermission().equals(MySQLConstants.SYSTEM_CONNECTION_CREATE))
                newPermission = new ConnectionDirectoryPermission(ConnectionDirectoryPermission.Type.CREATE);
            
            if(newPermission != null)
                allPermissions.add(newPermission);
        }
        
        return allPermissions;
    }
}
