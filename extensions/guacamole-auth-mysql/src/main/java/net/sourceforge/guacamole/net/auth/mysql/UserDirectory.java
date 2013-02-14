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
package net.sourceforge.guacamole.net.auth.mysql;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.SystemPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.model.UserExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.utility.PermissionCheckUtility;
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
public class UserDirectory implements Directory<String, User> {
    
    /**
     * The user who this user directory belongs to.
     * Access is based on his/her permission settings.
     */
    private MySQLUser user;
    
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
    PermissionCheckUtility permissionCheckUtility;
    
    @Inject
    Provider<MySQLUser> mySQLUserProvider;
    
    /**
     * Set the user for this directory.
     * @param user 
     */
    void init(MySQLUser user) {
        this.user = user;
    }
    
    /**
     * Create a new user based on the provided object.
     * @param user
     * @return
     * @throws GuacamoleException 
     */
    private MySQLUser getNewMySQLUser(User user) throws GuacamoleException {
        MySQLUser mySQLUser = mySQLUserProvider.get();
        mySQLUser.initNew(user);
        return mySQLUser;
    }
    
    /**
     * Get the user based on the username of the provided object.
     * @param user
     * @return
     * @throws GuacamoleException 
     */
    private MySQLUser getExistingMySQLUser(User user) throws GuacamoleException {
        return getExistingMySQLUser(user.getUsername());
    }
    
    /**
     * Get the user based on the username of the provided object.
     * @param user
     * @return
     * @throws GuacamoleException 
     */
    private MySQLUser getExistingMySQLUser(String name) throws GuacamoleException {
        MySQLUser mySQLUser = mySQLUserProvider.get();
        mySQLUser.initExisting(name);
        return mySQLUser;
    }
    
    @Transactional
    @Override
    public User get(String identifier) throws GuacamoleException {
        permissionCheckUtility.verifyUserReadAccess(this.user.getUserID(), identifier);
        return getExistingMySQLUser(identifier);
    }

    @Transactional
    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        Set<String> userNameSet = new HashSet<String>();
        List<MySQLUser> users = permissionCheckUtility.getReadableUsers(user.getUserID());
        for(MySQLUser mySQLUser : users) {
            userNameSet.add(mySQLUser.getUsername());
        }
        return userNameSet;
    }

    @Override
    @Transactional
    public void add(User object) throws GuacamoleException {
        permissionCheckUtility.verifyCreateUserPermission(this.user.getUserID());
        Preconditions.checkNotNull(object);
        permissionCheckUtility.verifyUserUpdateAccess(user.getUserID(), object.getUsername());
        
        //create user in database
        MySQLUser mySQLUser = getNewMySQLUser(object);
        userDAO.insert(mySQLUser.getUser());
        
        //create permissions in database
        updatePermissions(mySQLUser);
    }
    
    /**
     * Update all the permissions for a given user to be only those specified in the user object.
     * Delete any permissions not in the list, and create any in the list that do not exist
     * in the database.
     * @param user
     * @throws GuacamoleException 
     */
    private void updatePermissions(MySQLUser user) throws GuacamoleException {
        List<UserPermission> userPermissions = new ArrayList<UserPermission>();
        List<ConnectionPermission> connectionPermissions = new ArrayList<ConnectionPermission>();
        List<SystemPermission> systemPermissions = new ArrayList<SystemPermission>();
        
        for(Permission permission : user.getPermissions()) {
            if(permission instanceof UserPermission)
                userPermissions.add((UserPermission)permission);
            else if(permission instanceof ConnectionPermission)
                connectionPermissions.add((ConnectionPermission)permission);
            else if(permission instanceof SystemPermission)
                systemPermissions.add((SystemPermission)permission);
        }
        
        updateUserPermissions(userPermissions, user);
        updateConnectionPermissions(connectionPermissions, user);
        updateSystemPermissions(systemPermissions, user);
    }
    
    /**
     * Update all the permissions having to do with users for a given user.
     * @param permissions
     * @param user 
     */
    private void updateUserPermissions(Iterable<UserPermission> permissions, MySQLUser user) throws GuacamoleException {
        
        List<String> usernames = new ArrayList<String>();
        for(UserPermission permission : permissions) {
            usernames.add(permission.getObjectIdentifier());
        }
            
        // find all the users by username
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUsernameIn(usernames);
        List<net.sourceforge.guacamole.net.auth.mysql.model.User> dbUsers = userDAO.selectByExample(userExample);
        List<Integer> userIDs = new ArrayList<Integer>();
        
        Map<String, net.sourceforge.guacamole.net.auth.mysql.model.User> dbUserMap = new HashMap<String, net.sourceforge.guacamole.net.auth.mysql.model.User>();
        for(net.sourceforge.guacamole.net.auth.mysql.model.User dbUser : dbUsers) {
            dbUserMap.put(dbUser.getUsername(), dbUser);
            userIDs.add(dbUser.getUser_id());
        }
        
        // find any user permissions that may already exist
        UserPermissionExample userPermissionExample = new UserPermissionExample();
        userPermissionExample.createCriteria().andAffected_user_idIn(userIDs);
        List<UserPermissionKey> existingPermissions = userPermissionDAO.selectByExample(userPermissionExample);
        Set<Integer> existingUserIDs = new HashSet<Integer>();
        for(UserPermissionKey userPermission : existingPermissions) {
            existingUserIDs.add(userPermission.getAffected_user_id());
        }
        
        // delete any permissions that are not in the provided list
        userPermissionExample.clear();
        userPermissionExample.createCriteria().andAffected_user_idNotIn(userIDs);
        userPermissionDAO.deleteByExample(userPermissionExample);
        
        // finally, insert the new permissions
        for(UserPermission permission : permissions) {
            net.sourceforge.guacamole.net.auth.mysql.model.User dbAffectedUser = dbUserMap.get(permission.getObjectIdentifier());
            if(dbAffectedUser == null)
                throw new GuacamoleException("User '" + permission.getObjectIdentifier() + "' not found.");
            
            // the permission for this user already exists, we don't need to create it again
            if(existingUserIDs.contains(dbAffectedUser.getUser_id()))
                continue;
            
            UserPermissionKey newPermission = new UserPermissionKey();
            newPermission.setAffected_user_id(dbAffectedUser.getUser_id());
            newPermission.setPermission(permission.getType().name());
            newPermission.setUser_id(user.getUserID());
            userPermissionDAO.insert(newPermission);
        }
    }
    
    /**
     * Update all the permissions having to do with connections for a given user.
     * @param permissions
     * @param user 
     */
    private void updateConnectionPermissions(Iterable<ConnectionPermission> permissions, MySQLUser user) throws GuacamoleException {
        
        List<String> connectionnames = new ArrayList<String>();
        for(ConnectionPermission permission : permissions) {
            connectionnames.add(permission.getObjectIdentifier());
        }
            
        // find all the connections by connectionname
        ConnectionExample connectionExample = new ConnectionExample();
        connectionExample.createCriteria().andConnection_nameIn(connectionnames);
        List<net.sourceforge.guacamole.net.auth.mysql.model.Connection> dbConnections = connectionDAO.selectByExample(connectionExample);
        List<Integer> connectionIDs = new ArrayList<Integer>();
        
        Map<String, net.sourceforge.guacamole.net.auth.mysql.model.Connection> dbConnectionMap = new HashMap<String, net.sourceforge.guacamole.net.auth.mysql.model.Connection>();
        for(net.sourceforge.guacamole.net.auth.mysql.model.Connection dbConnection : dbConnections) {
            dbConnectionMap.put(dbConnection.getConnection_name(), dbConnection);
            connectionIDs.add(dbConnection.getConnection_id());
        }
        
        // find any connection permissions that may already exist
        ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
        connectionPermissionExample.createCriteria().andConnection_idIn(connectionIDs);
        List<ConnectionPermissionKey> existingPermissions = connectionPermissionDAO.selectByExample(connectionPermissionExample);
        Set<Integer> existingConnectionIDs = new HashSet<Integer>();
        for(ConnectionPermissionKey connectionPermission : existingPermissions) {
            existingConnectionIDs.add(connectionPermission.getConnection_id());
        }
        
        // delete any permissions that are not in the provided list
        connectionPermissionExample.clear();
        connectionPermissionExample.createCriteria().andConnection_idNotIn(connectionIDs);
        connectionPermissionDAO.deleteByExample(connectionPermissionExample);
        
        // finally, insert the new permissions
        for(ConnectionPermission permission : permissions) {
            net.sourceforge.guacamole.net.auth.mysql.model.Connection dbConnection = dbConnectionMap.get(permission.getObjectIdentifier());
            if(dbConnection == null)
                throw new GuacamoleException("Connection '" + permission.getObjectIdentifier() + "' not found.");
            
            // the permission for this connection already exists, we don't need to create it again
            if(existingConnectionIDs.contains(dbConnection.getConnection_id()))
                continue;
            
            ConnectionPermissionKey newPermission = new ConnectionPermissionKey();
            newPermission.setConnection_id(dbConnection.getConnection_id());
            newPermission.setPermission(permission.getType().name());
            newPermission.setConnection_id(user.getUserID());
            connectionPermissionDAO.insert(newPermission);
        }
    }
    
    /**
     * Update all system permissions for a given user.
     * @param permissions
     * @param user 
     */
    private void updateSystemPermissions(Iterable<SystemPermission> permissions, MySQLUser user) {
        List<String> systemPermissionTypes = new ArrayList<String>();
        for(SystemPermission permission : permissions) {
            String operation = permission.getType().name();
            if(permission instanceof ConnectionDirectoryPermission)
                systemPermissionTypes.add(operation + "_CONNECTION");
            else if(permission instanceof UserDirectoryPermission)
                systemPermissionTypes.add(operation + "_USER");
        }
        
        //delete all system permissions not in the list
        SystemPermissionExample systemPermissionExample = new SystemPermissionExample();
        systemPermissionExample.createCriteria().andUser_idEqualTo(user.getUserID()).andPermissionNotIn(systemPermissionTypes);
        systemPermissionDAO.deleteByExample(systemPermissionExample);
        
        // find all existing system permissions
        systemPermissionExample.clear();
        systemPermissionExample.createCriteria().andUser_idEqualTo(user.getUserID()).andPermissionIn(systemPermissionTypes);
        List<SystemPermissionKey> existingPermissions = systemPermissionDAO.selectByExample(systemPermissionExample);
        Set<String> existingPermissionTypes = new HashSet<String>();
        for(SystemPermissionKey existingPermission : existingPermissions) {
            existingPermissionTypes.add(existingPermission.getPermission());
        }
        
        // finally, insert any new system permissions for this user
        for(String systemPermissionType : systemPermissionTypes) {
            //do not insert the permission if it already exists 
            if(existingPermissionTypes.contains(systemPermissionType))
                continue;
            
            SystemPermissionKey newSystemPermission = new SystemPermissionKey();
            newSystemPermission.setUser_id(user.getUserID());
            newSystemPermission.setPermission(systemPermissionType);
            systemPermissionDAO.insert(newSystemPermission);
        }
    }

    @Override
    @Transactional
    public void update(User object) throws GuacamoleException {
        permissionCheckUtility.verifyUserUpdateAccess(this.user.getUserID(), object.getUsername());
        //update the user in the database
        MySQLUser mySQLUser = getExistingMySQLUser(object);
        userDAO.updateByPrimaryKey(mySQLUser.getUser());
        
        //update permissions in database
        updatePermissions(mySQLUser);
    }

    @Override
    @Transactional
    public void remove(String identifier) throws GuacamoleException {
        permissionCheckUtility.verifyUserDeleteAccess(this.user.getUserID(), identifier);
        
        MySQLUser mySQLUser = getExistingMySQLUser(identifier);
        
        //delete all the user permissions in the database
        deleteAllPermissions(mySQLUser);
                
        //delete the user in the database
        userDAO.deleteByPrimaryKey(mySQLUser.getUserID());
    }
    
    /**
     * Delete all permissions associated with the provided user.
     * @param user 
     */
    private void deleteAllPermissions(MySQLUser user) {
        //delete all user permissions
        UserPermissionExample userPermissionExample = new UserPermissionExample();
        userPermissionExample.createCriteria().andUser_idEqualTo(user.getUserID());
        userPermissionDAO.deleteByExample(userPermissionExample);
        
        //delete all connection permissions
        ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
        connectionPermissionExample.createCriteria().andUser_idEqualTo(user.getUserID());
        connectionPermissionDAO.deleteByExample(connectionPermissionExample);
        
        //delete all system permissions
        SystemPermissionExample systemPermissionExample = new SystemPermissionExample();
        systemPermissionExample.createCriteria().andUser_idEqualTo(user.getUserID());
        systemPermissionDAO.deleteByExample(systemPermissionExample);
    }
}
