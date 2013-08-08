
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
import com.google.inject.Provider;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.auth.AbstractConnectionGroup;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.mysql.service.ConnectionGroupService;
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;

/**
 * A MySQL based implementation of the ConnectionGroup object.
 * @author James Muehlner
 */
public class MySQLConnectionGroup extends AbstractConnectionGroup {

    /**
     * The ID associated with this connection group in the database.
     */
    private Integer connectionGroupID;

    /**
     * The ID of the parent connection group for this connection group.
     */
    private Integer parentID;
    
    /**
     * The type of this connection group.
     */
    private String type;

    /**
     * The ID of the user who queried or created this connection group.
     */
    private int userID;
    
    /**
     * A Directory of connections that have this connection group as a parent.
     */
    private ConnectionDirectory connectionDirectory = null;
    
    /**
     * A Directory of connection groups that have this connection group as a parent.
     */
    private ConnectionGroupDirectory connectionGroupDirectory = null;

    /**
     * Service managing connection groups.
     */
    @Inject
    private ConnectionGroupService connectionGroupService;

    /**
     * Service for checking permissions.
     */
    @Inject
    private PermissionCheckService permissionCheckService;
    
    /**
     * Service for creating new ConnectionDirectory objects.
     */
    @Inject Provider<ConnectionDirectory> connectionDirectoryProvider;
    
    /**
     * Service for creating new ConnectionGroupDirectory objects.
     */
    @Inject Provider<ConnectionGroupDirectory> connectionGroupDirectoryProvider;

    /**
     * Create a default, empty connection group.
     */
    public MySQLConnectionGroup() {
    }

    /**
     * Get the ID of the corresponding connection group record.
     * @return The ID of the corresponding connection group, if any.
     */
    public Integer getConnectionGroupID() {
        return connectionGroupID;
    }

    /**
     * Sets the ID of the corresponding connection group record.
     * @param connectionID The ID to assign to this connection group.
     */
    public void setConnectionID(Integer connectionGroupID) {
        this.connectionGroupID = connectionGroupID;
    }

    /**
     * Get the ID of the parent connection group for this connection group, if any.
     * @return The ID of the parent connection group for this connection group, if any.
     */
    public Integer getParentID() {
        return parentID;
    }

    /**
     * Sets the ID of the parent connection group for this connection group.
     * @param connectionID The ID of the parent connection group for this connection group.
     */
    public void setParentID(Integer parentID) {
        this.parentID = parentID;
    }

    /**
     * Initialize from explicit values.
     *
     * @param connectionGroupID The ID of the associated database record, if any.
     * @param parentID The ID of the parent connection group for this connection group, if any.
     * @param identifier The unique identifier associated with this connection group.
     * @param type The type of this connection group.
     * @param userID The IID of the user who queried this connection.
     */
    public void init(Integer connectionGroupID, Integer parentID, String name, 
            String identifier, String type, int userID) {
        this.connectionGroupID = connectionGroupID;
        this.parentID = parentID;
        setName(name);
        setIdentifier(identifier);
        this.type = type;
        this.userID = userID;
        
        connectionDirectory = connectionDirectoryProvider.get();
        connectionDirectory.init(userID, parentID);
        
        connectionGroupDirectory = connectionGroupDirectoryProvider.get();
        connectionGroupDirectory.init(userID, parentID);
    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        
        // Verify permission to use the connection group for balancing purposes
        permissionCheckService.verifyConnectionGroupUsageAccess
                (this.connectionGroupID, this.userID, MySQLConstants.CONNECTION_GROUP_BALANCING);

        // Verify permission to delete
        permissionCheckService.verifyConnectionGroupAccess(this.userID,
                this.connectionGroupID,
                MySQLConstants.CONNECTION_GROUP_READ);
        
        return connectionGroupService.connect(this, info, userID);
    }
    
    @Override
    public Directory<String, Connection> getConnectionDirectory() throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<String, ConnectionGroup> getConnectionGroupDirectory() throws GuacamoleException {
        return connectionGroupDirectory;
    }
    
    /**
     * Returns the connection group type.
     * @return the connection group type.
     */
    public String getType() {
        return type;
    }
    
    /**
     * Sets the connection group type.
     * @param type the connection group type.
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setBalancing(boolean balancing) {
        if(balancing)
            this.type = MySQLConstants.CONNECTION_GROUP_BALANCING;
        else
            this.type = MySQLConstants.CONNECTION_GROUP_ORGANIZATIONAL;
    }

    @Override
    public boolean isBalancing() {
        return MySQLConstants.CONNECTION_GROUP_BALANCING.equals(this.type);
    }

}
