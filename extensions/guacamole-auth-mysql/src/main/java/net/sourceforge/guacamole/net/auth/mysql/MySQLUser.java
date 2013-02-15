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

import com.google.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.UserExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserWithBLOBs;
import net.sourceforge.guacamole.net.auth.mysql.utility.PasswordEncryptionUtility;
import net.sourceforge.guacamole.net.auth.mysql.utility.PermissionCheckUtility;
import net.sourceforge.guacamole.net.auth.mysql.utility.SaltUtility;
import net.sourceforge.guacamole.net.auth.permission.Permission;

/**
 * A MySQL based implementation of the User object.
 * @author James Muehlner
 */
public class MySQLUser implements User {

    private UserWithBLOBs user;
    
    @Inject
    UserMapper userDAO;
    
    @Inject
    PasswordEncryptionUtility passwordUtility;
    
    @Inject
    SaltUtility saltUtility;
    
    @Inject
    PermissionCheckUtility permissionCheckUtility;
    
    Set<Permission> permissions;
    
    /**
     * Create a default, empty user.
     */
    MySQLUser() {
        user = new UserWithBLOBs();
        permissions = new HashSet<Permission>();
    }
    
    /**
     * Create the user, throwing an exception if the credentials do not match what's in the database.
     * @param credentials
     * @throws GuacamoleException 
     */
    void init (Credentials credentials) throws GuacamoleException {
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUsernameEqualTo(credentials.getUsername());
        List<UserWithBLOBs> users = userDAO.selectByExampleWithBLOBs(userExample);
        if(users.size() > 1)  // the unique constraint on the table should prevent this
            throw new GuacamoleException("Multiple users found with the same username: " + credentials.getUsername());
        if(users.isEmpty())
            throw new GuacamoleException("No user found with the supplied credentials");
        user = users.get(0);
        // check password
        if(!passwordUtility.checkCredentials(credentials, user.getPassword_hash(), user.getUsername(), user.getPassword_salt()))
            throw new GuacamoleException("No user found with the supplied credentials");
        
        this.permissions = permissionCheckUtility.getAllPermissions(user.getUser_id());
    }
    
    /**
     * Create a new user from the provided information. This represents a user that has not yet been inserted.
     * @param user
     * @throws GuacamoleException 
     */
    public void initNew (User user) throws GuacamoleException {
        this.setPassword(user.getPassword());
        this.setUsername(user.getUsername());
        this.permissions = user.getPermissions();
    }
    
    /**
     * Loads a user by username.
     * @param userName
     * @throws GuacamoleException 
     */
    public void initExisting (String username) throws GuacamoleException {
        UserExample example = new UserExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UserWithBLOBs> userList = userDAO.selectByExampleWithBLOBs(example);
        if(userList.size() > 1) // this should never happen; the unique constraint should prevent it
            throw new GuacamoleException("Multiple users found with username '" + username + "'.");
        if(userList.size() == 0)
            throw new GuacamoleException("No user found with username '" + username + "'.");
        
        this.user = userList.get(0);
        this.permissions = permissionCheckUtility.getAllPermissions(user.getUser_id());
    }
    
    /**
     * Initialize from a database record.
     * @param user 
     */
    public void init(UserWithBLOBs user) {
        this.user = user;
        this.permissions = permissionCheckUtility.getAllPermissions(user.getUser_id());
    }
    
    /**
     * Get the user id.
     * @return 
     */
    public int getUserID() {
        return user.getUser_id();
    }
    
    /**
     * Return the database record held by this object.
     * @return 
     */
    public UserWithBLOBs getUser() {
        return user;
    }
    
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public void setUsername(String username) {
        user.setUsername(username);
    }

    @Override
    public String getPassword() {
        try {
            return new String(user.getPassword_hash(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex); // should not happen
        }
    }

    @Override
    public void setPassword(String password) {
        byte[] salt = saltUtility.generateSalt();
        user.setPassword_salt(salt);
        byte[] hash = passwordUtility.createPasswordHash(password, salt);
        user.setPassword_hash(hash);
    }

    @Override
    public Set<Permission> getPermissions() throws GuacamoleException {
        return permissions;
    }

    @Override
    public boolean hasPermission(Permission permission) throws GuacamoleException {
        return permissions.contains(permission);
    }

    @Override
    public void addPermission(Permission permission) throws GuacamoleException {
        permissions.add(permission);
    }

    @Override
    public void removePermission(Permission permission) throws GuacamoleException {
        permissions.remove(permission);
    }
    
    @Override
    public boolean equals(Object other) {
        if(!(other instanceof MySQLUser))
            return false;
        return ((MySQLUser)other).getUserID() == this.getUserID();
    }
}
