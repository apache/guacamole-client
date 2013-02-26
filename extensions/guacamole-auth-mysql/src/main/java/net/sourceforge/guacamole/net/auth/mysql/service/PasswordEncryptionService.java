
package net.sourceforge.guacamole.net.auth.mysql.service;

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

/**
 * A service to perform password encryption and checking.
 * @author James Muehlner
 */
public interface PasswordEncryptionService {

    /**
     * Checks whether the provided, unhashed password matches the given
     * hash/salt pair.
     *
     * @param credentials The credentials to validate.
     * @param hashedPassword The hashed password to compare the given password
     *                       against.
     * @param salt The salt used when the hashed password given was created.
     * @return true if the provided credentials match the values given, false
     *         otherwise.
     */
    public boolean checkPassword(String password, byte[] hashedPassword,
            byte[] salt);

    /**
     * Creates a password hash based on the provided username, password, and
     * salt.
     *
     * @param password The password to hash.
     * @param salt The salt to use when hashing the password.
     * @return The generated password hash.
     */
    public byte[] createPasswordHash(String password, byte[] salt);
}
