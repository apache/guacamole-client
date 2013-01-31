
package net.sourceforge.guacamole.net.auth.simple;

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

import java.util.Map;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * An extremely simple UserContext implementation which provides access to
 * a defined and restricted set of GuacamoleConfigurations. Access to
 * querying or modifying either users or permissions is denied.
 * 
 * @author Michael Jumper
 */
public class SimpleUserContext implements UserContext {

    /**
     * Reference to the user whose permissions dictate the configurations
     * accessible within this UserContext.
     */
    private final User self;
    
    /**
     * The Directory with access only to those Connections that the User
     * associated with this UserContext has access to.
     */
    private final Directory<String, Connection> connectionDirectory;

    /**
     * The Directory with access only to the User associated with this
     * UserContext.
     */
    private final Directory<String, User> userDirectory;
    
    /**
     * Creates a new SimpleUserContext which provides access to only those
     * configurations within the given Map. The User given must be the user
     * that owns this UserContext, and the Map given must contain only
     * GuacamoleConfigurations that the given User has read access to.
     * 
     * @param self The owner of this UserContext.
     * @param configs A Map of all configurations for which the user associated
     *                with this UserContext has read access.
     */
    public SimpleUserContext(User self,
            Map<String, GuacamoleConfiguration> configs) {

        this.self = self;

        this.connectionDirectory =
                new SimpleConnectionDirectory(configs);
        
        this.userDirectory = new SimpleUserDirectory(self);
        
    }
    
    @Override
    public User self() {
        return self;
    }

    @Override
    public Directory<String, Connection> getConnectionDirectory()
            throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<String, User> getUserDirectory()
            throws GuacamoleException {
        return userDirectory;
    }

}
