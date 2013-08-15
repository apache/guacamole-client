
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
import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.ConnectionGroup.Type;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionGroupPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionGroupPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.service.ConnectionGroupService;
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
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
     * The ID of the parent connection group.
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
     * Service for manipulating connection group permissions in the database.
     */
    @Inject
    private ConnectionGroupPermissionMapper connectionGroupPermissionDAO;

    /**
     * Set the user and parentID for this directory.
     *
     * @param user_id The ID of the user owning this connection group directory.
     * @param parentID The ID of the parent connection group.
     */
    public void init(int user_id, Integer parentID) {
        this.parentID = parentID;
        this.user_id = user_id;
    }

    @Transactional
    @Override
    public ConnectionGroup get(String identifier) throws GuacamoleException {

        // Get connection
        MySQLConnectionGroup connectionGroup =
                connectionGroupService.retrieveConnectionGroup(identifier, user_id);
        
        if(connectionGroup == null)
            return null;
        
        // Verify permission to use the parent connection group for organizational purposes
        permissionCheckService.verifyConnectionGroupUsageAccess
                (connectionGroup.getParentID(), user_id, MySQLConstants.CONNECTION_GROUP_ORGANIZATIONAL);

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
        
        return permissionCheckService.retrieveConnectionGroupIdentifiers(user_id, 
                parentID, MySQLConstants.CONNECTION_GROUP_READ);
    }

    @Transactional
    @Override
    public void add(ConnectionGroup object) throws GuacamoleException {

        String name = object.getName().trim();
        if(name.isEmpty())
            throw new GuacamoleClientException("The connection group name cannot be blank.");
        
        Type type = object.getType();
        
        String mySQLType = MySQLConstants.getConnectionGroupTypeConstant(type);
        
        // Verify permission to create
        permissionCheckService.verifySystemAccess(this.user_id,
                MySQLConstants.SYSTEM_CONNECTION_GROUP_CREATE);
        
        // Verify permission to edit the parent connection group
        permissionCheckService.verifyConnectionGroupAccess(this.user_id, 
                this.parentID, MySQLConstants.CONNECTION_GROUP_UPDATE);
        
        // Verify permission to use the parent connection group for organizational purposes
        permissionCheckService.verifyConnectionGroupUsageAccess
                (parentID, user_id, MySQLConstants.CONNECTION_GROUP_ORGANIZATIONAL);

        // Verify that no connection already exists with this name.
        MySQLConnectionGroup previousConnectionGroup =
                connectionGroupService.retrieveConnectionGroup(name, parentID, user_id);
        if(previousConnectionGroup != null)
            throw new GuacamoleClientException("That connection group name is already in use.");

        // Create connection group
        MySQLConnectionGroup connectionGroup = connectionGroupService
                .createConnectionGroup(name, user_id, parentID, mySQLType);

        // Finally, give the current user full access to the newly created
        // connection group.
        ConnectionGroupPermissionKey newConnectionGroupPermission = new ConnectionGroupPermissionKey();
        newConnectionGroupPermission.setUser_id(this.user_id);
        newConnectionGroupPermission.setConnection_group_id(connectionGroup.getConnectionGroupID());

        // Read permission
        newConnectionGroupPermission.setPermission(MySQLConstants.CONNECTION_GROUP_READ);
        connectionGroupPermissionDAO.insert(newConnectionGroupPermission);

        // Update permission
        newConnectionGroupPermission.setPermission(MySQLConstants.CONNECTION_GROUP_UPDATE);
        connectionGroupPermissionDAO.insert(newConnectionGroupPermission);

        // Delete permission
        newConnectionGroupPermission.setPermission(MySQLConstants.CONNECTION_GROUP_DELETE);
        connectionGroupPermissionDAO.insert(newConnectionGroupPermission);

        // Administer permission
        newConnectionGroupPermission.setPermission(MySQLConstants.CONNECTION_GROUP_ADMINISTER);
        connectionGroupPermissionDAO.insert(newConnectionGroupPermission);

    }

    @Transactional
    @Override
    public void update(ConnectionGroup object) throws GuacamoleException {

        // If connection not actually from this auth provider, we can't handle
        // the update
        if (!(object instanceof MySQLConnectionGroup))
            throw new GuacamoleException("Connection not from database.");

        MySQLConnectionGroup mySQLConnectionGroup = (MySQLConnectionGroup) object;

        // Verify permission to update
        permissionCheckService.verifyConnectionAccess(this.user_id,
                mySQLConnectionGroup.getConnectionGroupID(),
                MySQLConstants.CONNECTION_UPDATE);

        // Perform update
        connectionGroupService.updateConnectionGroup(mySQLConnectionGroup);
    }

    @Transactional
    @Override
    public void remove(String identifier) throws GuacamoleException {

        // Get connection
        MySQLConnectionGroup mySQLConnectionGroup =
                connectionGroupService.retrieveConnectionGroup(identifier, user_id);
        
        if(mySQLConnectionGroup == null)
            throw new GuacamoleException("Connection group not found.");
        
        // Verify permission to use the parent connection group for organizational purposes
        permissionCheckService.verifyConnectionGroupUsageAccess
                (mySQLConnectionGroup.getParentID(), user_id, MySQLConstants.CONNECTION_GROUP_ORGANIZATIONAL);

        // Verify permission to delete
        permissionCheckService.verifyConnectionGroupAccess(this.user_id,
                mySQLConnectionGroup.getConnectionGroupID(),
                MySQLConstants.CONNECTION_GROUP_DELETE);

        // Delete the connection group itself
        connectionGroupService.deleteConnectionGroup
                (mySQLConnectionGroup.getConnectionGroupID());

    }

    @Override
    public void move(String identifier, Directory<String, ConnectionGroup> directory) 
            throws GuacamoleException {
        
        if(MySQLConstants.CONNECTION_GROUP_ROOT_IDENTIFIER.equals(identifier))
            throw new GuacamoleClientException("The root connection group cannot be moved.");
        
        if(!(directory instanceof ConnectionGroupDirectory))
            throw new GuacamoleClientException("Directory not from database");
        
        Integer toConnectionGroupID = ((ConnectionGroupDirectory)directory).parentID;

        // Get connection group
        MySQLConnectionGroup mySQLConnectionGroup =
                connectionGroupService.retrieveConnectionGroup(identifier, user_id);
        
        if(mySQLConnectionGroup == null)
            throw new GuacamoleClientException("Connection group not found.");

        // Verify permission to update the connection
        permissionCheckService.verifyConnectionAccess(this.user_id,
                mySQLConnectionGroup.getConnectionGroupID(),
                MySQLConstants.CONNECTION_GROUP_UPDATE);
        
        // Verify permission to use the from connection group for organizational purposes
        permissionCheckService.verifyConnectionGroupUsageAccess
                (mySQLConnectionGroup.getParentID(), user_id, MySQLConstants.CONNECTION_GROUP_ORGANIZATIONAL);

        // Verify permission to update the from connection group
        permissionCheckService.verifyConnectionGroupAccess(this.user_id,
                mySQLConnectionGroup.getParentID(), MySQLConstants.CONNECTION_GROUP_UPDATE);
        
        // Verify permission to use the to connection group for organizational purposes
        permissionCheckService.verifyConnectionGroupUsageAccess
                (toConnectionGroupID, user_id, MySQLConstants.CONNECTION_GROUP_ORGANIZATIONAL);

        // Verify permission to update the to connection group
        permissionCheckService.verifyConnectionGroupAccess(this.user_id,
                toConnectionGroupID, MySQLConstants.CONNECTION_GROUP_UPDATE);

        // Verify that no connection already exists with this name.
        MySQLConnectionGroup previousConnectionGroup =
                connectionGroupService.retrieveConnectionGroup(mySQLConnectionGroup.getName(), 
                toConnectionGroupID, user_id);
        if(previousConnectionGroup != null)
            throw new GuacamoleClientException("That connection group name is already in use.");
        
        // Verify that moving this connectionGroup would not cause a cycle
        Integer relativeParentID = toConnectionGroupID;
        while(relativeParentID != null) {
            if(relativeParentID == mySQLConnectionGroup.getConnectionGroupID())
                throw new GuacamoleClientException("Connection group cycle detected.");
            
            MySQLConnectionGroup relativeParentGroup = connectionGroupService.
                    retrieveConnectionGroup(relativeParentID, user_id);
            
            relativeParentID = relativeParentGroup.getParentID();
        }
        
        // Update the connection
        mySQLConnectionGroup.setParentID(toConnectionGroupID);
        connectionGroupService.updateConnectionGroup(mySQLConnectionGroup);
    }

}
