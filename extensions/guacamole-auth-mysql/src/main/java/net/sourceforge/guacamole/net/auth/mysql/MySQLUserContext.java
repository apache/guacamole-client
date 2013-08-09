
package net.sourceforge.guacamole.net.auth.mysql;

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

import com.google.inject.Inject;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.auth.mysql.service.UserService;

/**
 * The MySQL representation of a UserContext.
 * @author James Muehlner
 */
public class MySQLUserContext implements UserContext {

    /**
     * The ID of the user owning this context. The permissions of this user
     * dictate the access given via the user and connection directories.
     */
    private int user_id;

    /**
     * User directory restricted by the permissions of the user associated
     * with this context.
     */
    @Inject
    private UserDirectory userDirectory;
    
    /**
     * The root connection group.
     */
    @Inject
    private MySQLConnectionGroup rootConnectionGroup;

    /**
     * Service for accessing users.
     */
    @Inject
    private UserService userService;

    /**
     * Initializes the user and directories associated with this context.
     *
     * @param user_id The ID of the user owning this context.
     */
    public void init(int user_id) {
        this.user_id = user_id;
        userDirectory.init(user_id);
        rootConnectionGroup.init(null, null, 
                MySQLConstants.CONNECTION_GROUP_ROOT_IDENTIFIER, 
                MySQLConstants.CONNECTION_GROUP_ROOT_IDENTIFIER, 
                ConnectionGroup.Type.ORGANIZATIONAL, user_id);
    }

    @Override
    public User self() {
        return userService.retrieveUser(user_id);
    }

    @Override
    public Directory<String, User> getUserDirectory() throws GuacamoleException {
        return userDirectory;
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() throws GuacamoleException {
        return rootConnectionGroup;
    }

}
