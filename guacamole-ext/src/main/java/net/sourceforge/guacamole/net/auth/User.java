
package net.sourceforge.guacamole.net.auth;

import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.permission.Permission;

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
 * The Original Code is guacamole-ext.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
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


/**
 * A user of the Guacamole web application.
 * 
 * @author Michael Jumper 
 */
public interface User {

    /**
     * Returns the name of this user, which must be unique across all users.
     * 
     * @return The name of this user.
     */
    public String getUsername();

    /**
     * Sets the name of this user, which must be unique across all users.
     * 
     * @param username  The name of this user.
     */
    public void setUsername(String username);

    /**
     * Returns this user's password. Note that the password returned may be
     * hashed or completely arbitrary.
     * 
     * @return A String which may (or may not) be the user's password.
     */
    public String getPassword();

    /**
     * Sets this user's password. Note that while this function is guaranteed
     * to change the password of this User object, there is no guarantee that
     * getPassword() will return the value given to setPassword().
     * 
     * @param password The password to set.
     */
    public void setPassword(String password);

    /**
     * Lists all permissions given to this user.
     * 
     * @return A Set of all permissions granted to this user.
     * 
     * @throws GuacamoleException  If an error occurs while retrieving
     *                             permissions, or if reading all permissions
     *                             is not allowed.
     */
    Set<Permission> getPermissions() throws GuacamoleException;
    
    /**
     * Tests whether this user has the specified permission.
     * 
     * @param permission The permission to check.
     * @return true if the permission is granted to this user, false otherwise.
     * 
     * @throws GuacamoleException If an error occurs while checking permissions,
     *                            or if permissions cannot be checked due to
     *                            lack of permissions to do so.
     */
    boolean hasPermission(Permission permission) throws GuacamoleException;
    
    /**
     * Adds the specified permission to this user.
     * 
     * @param permission The permission to add.
     * 
     * @throws GuacamoleException If an error occurs while adding the
     *                            permission. or if permission to add
     *                            permissions is denied.
     */
    void addPermission(Permission permission) throws GuacamoleException;

    /**
     * Removes the specified permission from this specified user.
     * 
     * @param permission The permission to remove.
     * 
     * @throws GuacamoleException If an error occurs while removing the
     *                            permission. or if permission to remove
     *                            permissions is denied.
     */
    void removePermission(Permission permission) throws GuacamoleException;


}
