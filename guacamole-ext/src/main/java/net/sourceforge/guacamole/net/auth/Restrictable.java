
package net.sourceforge.guacamole.net.auth;

import net.sourceforge.guacamole.GuacamoleException;

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
 * The Original Code is guacamole-auth.
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
 * Interface which allows restricted objects to expose their restrictions.
 * 
 * @author Michael Jumper
 */
public interface Restrictable {

    /**
     * All possible permissions for a restricted object.
     */
    public enum Permission {

        /**
         * Access to read properties of the restricted object.
         */
        READ,

        /**
         * Access to write properties of the restricted object.
         */
        WRITE,

        /**
         * Access to change permissions of the restricted object.
         */
        ADMINISTER

    }

    /**
     * Checks whether the given user has the given permission on this object.
     * Depending on the credentials given, access to reading permissions may
     * be denied.
     * 
     * @param credentials The credentials to use when reading permissions.
     * @param user The user to read the permissions for.
     * @param permission The permission to check.
     * @return true if the user has the given permission, false otherwise.
     * @throws GuacamoleException If an error occurs while reading the
     *                            permissions, such as permission being denied.
     */
    public boolean hasPermission(Credentials credentials,
            User user, Permission permission) throws GuacamoleException;


    /**
     * Adds the given permission to the given user for this object. Depending
     * on the credentials given, access to administering permissions may be
     * denied.
     *
     * @param credentials The credentials to use when adding permissions.
     * @param user The user to add the permission for.
     * @param permission The permission to add.
     * @throws GuacamoleException If an error occurs while adding the
     *                            permission, such as permission being denied.
     */
    public void addPermission(Credentials credentials,
            User user, Permission permission) throws GuacamoleException;
    
   /**
     * Removes the given permission from the given user for this object.
     * Depending on the credentials given, access to administering permissions
     * may be denied.
     *
     * @param credentials The credentials to use when removing permissions.
     * @param user The user to remove the permission from.
     * @param permission The permission to add.
     * @throws GuacamoleException If an error occurs while removing the
     *                            permission, such as permission being denied.
     */
    public void removePermission(Credentials credentials,
            User user, Permission permission) throws GuacamoleException;
    
}
