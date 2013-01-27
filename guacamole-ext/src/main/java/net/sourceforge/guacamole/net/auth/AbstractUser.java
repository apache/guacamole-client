
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

import net.sourceforge.guacamole.GuacamoleException;


/**
 * Basic implementation of a Guacamole user which uses the username to
 * determine equality. Username comparison is case-sensitive.
 * 
 * @author Michael Jumper 
 */
public class AbstractUser extends RestrictedObject
    implements User, Comparable<AbstractUser> {

    /**
     * The name of this user.
     */
    private String username;
    
    /**
     * This user's password. Note that while this provides a means for the
     * password to be set, the data stored in this String is not necessarily
     * the user's actual password. It may be hashed, it may be arbitrary.
     */
    private String password;

    @Override
    public String getUsername() throws GuacamoleException {
        return username;
    }

    @Override
    public void setUsername(String username) throws GuacamoleException {
        this.username = username;
    }

    @Override
    public String getPassword() throws GuacamoleException {
        return password;
    }

    @Override
    public void setPassword(String password) throws GuacamoleException {
        this.password = password;
    }


    @Override
    public int hashCode() {
        if (username == null) return 0;
        return username.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        // Not equal if null or not a User
        if (obj == null) return false;
        if (!(obj instanceof AbstractUser)) return false;

        // Get username
        String objUsername = ((AbstractUser) obj).username;

        // If null, equal only if this username is null 
        if (objUsername == null) return username == null;

        // Otherwise, equal only if strings are identical
        return objUsername.equals(username);

    }

    @Override
    public int compareTo(AbstractUser user) {

        // Having a username is greater than lack of a username
        if (user.username == null) {

            // If both null, then equal
            if (username == null)
                return 0;

            return 1;

        }

        // Lacking a username is less than having a username
        if (username == null)
            return -1;
       
        // Otherwise, compare strings
        return username.compareTo(user.username);
        
    }
    
}
