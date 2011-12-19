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
 * The Original Code is guacamole-common-auth.
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
 * Simple class containing username and password Strings. This class can be
 * used along with AuthenticationProvider to provide username/password
 * authentication.
 * 
 * @author Michael Jumper
 */
public class UsernamePassword {

    /**
     * An arbitrary username.
     */
    private String username;

    /**
     * An arbitrary password.
     */
    private String password;

    /**
     * Returns the password associated with this username/password pair.
     * @return The password associated with this username/password pair, or
     *         null if no password has been set.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password associated with this username/password pair.
     * @param password The password to associate with this username/password
     *                 pair.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the username associated with this username/password pair.
     * @return The username associated with this username/password pair, or
     *         null if no username has been set.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username associated with this username/password pair.
     * @param username The username to associate with this username/password
     *                 pair.
     */
    public void setUsername(String username) {
        this.username = username;
    }

}
