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
import org.glyptodon.guacamole.auth.jdbc.base.ModeledDirectoryObject;
import org.glyptodon.guacamole.auth.jdbc.security.PasswordEncryptionService;
import org.glyptodon.guacamole.auth.jdbc.security.SaltService;
import org.glyptodon.guacamole.auth.jdbc.permission.SystemPermissionService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.jdbc.permission.ConnectionGroupPermissionService;
import org.glyptodon.guacamole.auth.jdbc.permission.ConnectionPermissionService;
import org.glyptodon.guacamole.auth.jdbc.permission.UserPermissionService;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * An implementation of the User object which is backed by a database model.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class ModeledUser extends ModeledDirectoryObject<UserModel> implements User {

    /**
     * Service for hashing passwords.
     */
    @Inject
    private PasswordEncryptionService encryptionService;

    /**
     * Service for providing secure, random salts.
     */
    @Inject
    private SaltService saltService;

    /**
     * Service for retrieving system permissions.
     */
    @Inject
    private SystemPermissionService systemPermissionService;

    /**
     * Service for retrieving connection permissions.
     */
    @Inject
    private ConnectionPermissionService connectionPermissionService;

    /**
     * Service for retrieving connection group permissions.
     */
    @Inject
    private ConnectionGroupPermissionService connectionGroupPermissionService;

    /**
     * Service for retrieving user permissions.
     */
    @Inject
    private UserPermissionService userPermissionService;

    /**
     * The plaintext password previously set by a call to setPassword(), if
     * any. The password of a user cannot be retrieved once saved into the
     * database, so this serves to ensure getPassword() returns a reasonable
     * value if setPassword() is called. If no password has been set, or the
     * user was retrieved from the database, this will be null.
     */
    private String password = null;
    
    /**
     * Creates a new, empty ModeledUser.
     */
    public ModeledUser() {
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {

        UserModel userModel = getModel();
        
        // Store plaintext password internally
        this.password = password;

        // If no password provided, clear password salt and hash
        if (password == null) {
            userModel.setPasswordSalt(null);
            userModel.setPasswordHash(null);
        }

        // Otherwise generate new salt and hash given password using newly-generated salt
        else {
            byte[] salt = saltService.generateSalt();
            byte[] hash = encryptionService.createPasswordHash(password, salt);

            // Set stored salt and hash
            userModel.setPasswordSalt(salt);
            userModel.setPasswordHash(hash);
        }

    }

    /**
     * Returns whether this user is a system administrator, and thus is not
     * restricted by permissions.
     *
     * @return
     *    true if this user is a system administrator, false otherwise.
     *
     * @throws GuacamoleException 
     *    If an error occurs while determining the user's system administrator
     *    status.
     */
    public boolean isAdministrator() throws GuacamoleException {
        SystemPermissionSet systemPermissionSet = getSystemPermissions();
        return systemPermissionSet.hasPermission(SystemPermission.Type.ADMINISTER);
    }
    
    @Override
    public SystemPermissionSet getSystemPermissions()
            throws GuacamoleException {
        return systemPermissionService.getPermissionSet(getCurrentUser(), this);
    }

    @Override
    public ObjectPermissionSet getConnectionPermissions()
            throws GuacamoleException {
        return connectionPermissionService.getPermissionSet(getCurrentUser(), this);
    }

    @Override
    public ObjectPermissionSet getConnectionGroupPermissions()
            throws GuacamoleException {
        return connectionGroupPermissionService.getPermissionSet(getCurrentUser(), this);
    }

    @Override
    public ObjectPermissionSet getUserPermissions()
            throws GuacamoleException {
        return userPermissionService.getPermissionSet(getCurrentUser(), this);
    }

}
