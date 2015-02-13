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

package net.sourceforge.guacamole.net.auth.mysql;

import com.google.inject.Inject;
import net.sourceforge.guacamole.net.auth.mysql.model.UserModel;
import net.sourceforge.guacamole.net.auth.mysql.service.PasswordEncryptionService;
import net.sourceforge.guacamole.net.auth.mysql.service.SaltService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
import org.glyptodon.guacamole.net.auth.simple.SimpleObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.simple.SimpleSystemPermissionSet;

/**
 * A MySQL based implementation of the User object.
 * @author James Muehlner
 */
public class MySQLUser implements User, DirectoryObject<UserModel> {

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
     * The internal model object containing the values which represent this
     * user in the database.
     */
    private UserModel userModel;

    /**
     * The plaintext password previously set by a call to setPassword(), if
     * any. The password of a user cannot be retrieved once saved into the
     * database, so this serves to ensure getPassword() returns a reasonable
     * value if setPassword() is called. If no password has been set, or the
     * user was retrieved from the database, this will be null.
     */
    private String password = null;
    
    /**
     * Creates a new, empty MySQLUser.
     */
    public MySQLUser() {
    }

    /**
     * Creates a new MySQLUser backed by the given user model object. Changes
     * to this model object will affect the new MySQLUser even after creation,
     * and changes to the new MySQLUser will affect this model object.
     * 
     * @param userModel
     *     The user model object to use to back this MySQLUser.
     */
    public MySQLUser(UserModel userModel) {
        this.userModel = userModel;
    }

    @Override
    public UserModel getModel() {
        return userModel;
    }

    @Override
    public void setModel(UserModel userModel) {
        this.userModel = userModel;
        this.password = null;
    }

    @Override
    public String getUsername() {
        return userModel.getUsername();
    }

    @Override
    public void setUsername(String username) {
        userModel.setUsername(username);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {

        // Store plaintext password internally
        this.password = password;
        
        // Generate new salt and hash given password using newly-generated salt
        byte[] salt = saltService.generateSalt();
        byte[] hash = encryptionService.createPasswordHash(password, salt);

        // Set stored salt and hash
        userModel.setPasswordSalt(salt);
        userModel.setPasswordHash(hash);

    }

    @Override
    public SystemPermissionSet getSystemPermissions()
            throws GuacamoleException {
        // STUB
        return new SimpleSystemPermissionSet();
    }

    @Override
    public ObjectPermissionSet<String> getConnectionPermissions()
            throws GuacamoleException {
        // STUB
        return new SimpleObjectPermissionSet<String>();
    }

    @Override
    public ObjectPermissionSet<String> getConnectionGroupPermissions()
            throws GuacamoleException {
        // STUB
        return new SimpleObjectPermissionSet<String>();
    }

    @Override
    public ObjectPermissionSet<String> getUserPermissions()
            throws GuacamoleException {
        // STUB
        return new SimpleObjectPermissionSet<String>();
    }

}
