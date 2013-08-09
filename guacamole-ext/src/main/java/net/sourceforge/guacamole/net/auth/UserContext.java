
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
 * Contributor(s): James Muehlner
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
 * The context of an active user. The functions of this class enforce all
 * permissions and act only within the rights of the associated user.
 *
 * @author Michael Jumper
 */
public interface UserContext {

    /**
     * Returns the User whose access rights control the operations of this
     * UserContext.
     *
     * @return The User whose access rights control the operations of this
     *         UserContext.
     */
    User self();

    /**
     * Retrieves a Directory which can be used to view and manipulate other
     * users, but only as allowed by the permissions given to the user of this
     * UserContext.
     *
     * @return A Directory whose operations are bound by the restrictions
     *         of this UserContext.
     *
     * @throws GuacamoleException If an error occurs while creating the
     *                            Directory.
     */
    Directory<String, User> getUserDirectory() throws GuacamoleException;


    /**
     * Retrieves a connection group which can be used to view and manipulate
     * connections, but only as allowed by the permissions given to the user of 
     * this UserContext.
     *
     * @return A connection group whose operations are bound by the restrictions
     *         of this UserContext.
     *
     * @throws GuacamoleException If an error occurs while creating the
     *                            Directory.
     */
    ConnectionGroup getRootConnectionGroup() throws GuacamoleException;

}
