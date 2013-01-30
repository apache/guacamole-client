package net.sourceforge.guacamole.net.auth;

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

import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.permission.Permission;


/**
 * Provides access to a collection of all permissions, and allows permission
 * manipulation and removal.
 *
 * @author Michael Jumper
 */
public interface PermissionDirectory {

    /**
     * Lists all permissions given to the specified user.
     * 
     * @param user The username of the user to list permissions of.
     * @return A Set of all permissions granted to the specified user.
     * 
     * @throws GuacamoleException  If an error occurs while retrieving
     *                             permissions, or if reading all permissions
     *                             is not allowed.
     */
    Set<Permission> getPermissions(String user) throws GuacamoleException;
    
    /**
     * Tests whether the specified user has the specified permission.
     * 
     * @param user The username of the user to check permissions for.
     * @param permission The permission to check.
     * @return true if the permission is granted to the user specified, false
     *         otherwise.
     * 
     * @throws GuacamoleException If an error occurs while checking permissions,
     *                            or if permissions cannot be checked due to
     *                            lack of permissions to do so.
     */
    boolean hasPermission(String user, Permission permission)
            throws GuacamoleException;
    
    /**
     * Adds the specified permission to the specified user.
     * 
     * @param user The username of the user to add the permission to.
     * @param permission The permission to add.
     * 
     * @throws GuacamoleException If an error occurs while adding the
     *                            permission. or if permission to add
     *                            permissions is denied.
     */
    void addPermission(String user, Permission permission)
            throws GuacamoleException;

    /**
     * Removes the specified permission from the specified user.
     * 
     * @param user The username of the user to remove the permission from.
     * @param permission The permission to remove.
     * 
     * @throws GuacamoleException If an error occurs while removing the
     *                            permission. or if permission to remove
     *                            permissions is denied.
     */
    void removePermission(String user, Permission permission)
            throws GuacamoleException;

}
