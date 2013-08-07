
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
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleClientException;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionParameterMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionParameter;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionParameterExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.service.ConnectionGroupService;
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.mybatis.guice.transactional.Transactional;

/**
 * A MySQL-based implementation of the connection group directory.
 *
 * @author James Muehlner
 */
public class ConnectionGroupDirectory implements Directory<String, ConnectionGroup>{

    /**
     * The ID of the user who this connection directory belongs to.
     * Access is based on his/her permission settings.
     */
    private int user_id;

    /**
     * The ID of the parent connection for this connection.
     */
    private Integer parentID;

    /**
     * Service for checking permissions.
     */
    @Inject
    private PermissionCheckService permissionCheckService;

    /**
     * Service managing connection groups.
     */
    @Inject
    private ConnectionGroupService connectionGroupService;

    /**
     * Service for manipulating connection permissions in the database.
     */
    @Inject
    private ConnectionPermissionMapper connectionPermissionDAO;

    /**
     * Service for manipulating connection parameters in the database.
     */
    @Inject
    private ConnectionParameterMapper connectionParameterDAO;

    @Transactional
    @Override
    public ConnectionGroup get(String name) throws GuacamoleException {

        // Get connection
        MySQLConnectionGroup connectionGroup =
                connectionGroupService.retrieveConnectionGroup(name, parentID, user_id);

        // Verify access is granted
        permissionCheckService.verifyConnectionGroupAccess(
                this.user_id,
                connectionGroup.getConnectionGroupID(),
                MySQLConstants.CONNECTION_GROUP_READ);

        // Return connection group
        return connectionGroup;

    }

    @Transactional
    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        
        // Verify permission to use the connection group for organizational purposes
        permissionCheckService.verifyConnectionGroupUsageAccess
                (parentID, user_id, MySQLConstants.CONNECTION_GROUP_ORGANIZATIONAL);
        
        return permissionCheckService.retrieveConnectionGroupNames(user_id, 
                parentID, MySQLConstants.CONNECTION_GROUP_READ);
    }

    @Override
    public void add(ConnectionGroup object) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(ConnectionGroup object) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(String identifier) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
