
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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnection;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnectionGroup;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionGroupMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.Connection;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionGroupExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionGroupExample.Criteria;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connection groups.
 *
 * @author James Muehlner
 */
public class ConnectionGroupService {
    
    /**
     * DAO for accessing connection groups.
     */
    @Inject
    private ConnectionGroupMapper connectionGroupDAO;

    /**
     * Provider which creates MySQLConnectionGroups.
     */
    @Inject
    private Provider<MySQLConnectionGroup> mysqlConnectionGroupProvider;
    
    

    /**
     * Retrieves the connection group having the given 
     * name from the database.
     *
     * @param name The name of the connection to return.
     * @param parentID The ID of the parent connection group.
     * @param userID The ID of the user who queried this connection group.
     * @return The connection having the given name, or null if no such
     *         connection group could be found.
     */
    public MySQLConnectionGroup retrieveConnectionGroup(String name, Integer parentID,
            int userID) {

        // Create criteria
        ConnectionGroupExample example = new ConnectionGroupExample();
        Criteria criteria = example.createCriteria().andConnection_group_nameEqualTo(name);
        if(parentID != null)
            criteria.andParent_idEqualTo(parentID);
        else
            criteria.andParent_idIsNull();
        
        // Query connection group by name and parentID
        List<ConnectionGroup> connectionGroups =
                connectionGroupDAO.selectByExample(example);

        // If no connection group found, return null
        if(connectionGroups.isEmpty())
            return null;

        // Otherwise, return found connection
        return toMySQLConnectionGroup(connectionGroups.get(0), userID);

    }
    
    /**
     * Retrieves the connection group having the given ID from the database.
     *
     * @param id The ID of the connection group to retrieve.
     * @param userID The ID of the user who queried this connection.
     * @return The connection group having the given ID, or null if no such
     *         connection was found.
     */
    public MySQLConnectionGroup retrieveConnectionGroup(int id, int userID) {

        // Query connection by ID
        ConnectionGroup connectionGroup = connectionGroupDAO.selectByPrimaryKey(id);

        // If no connection found, return null
        if(connectionGroup == null)
            return null;

        // Otherwise, return found connection
        return toMySQLConnectionGroup(connectionGroup, userID);
    }

    public GuacamoleSocket connect(MySQLConnectionGroup group, 
            GuacamoleClientInformation info, int userID) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Retrieves a map of all connection group names for the given IDs.
     *
     * @param ids The IDs of the connection groups to retrieve the names of.
     * @return A map containing the names of all connection groups and their
     *         corresponding IDs.
     */
    public Map<Integer, String> retrieveNames(Collection<Integer> ids) {

        // If no IDs given, just return empty map
        if (ids.isEmpty())
            return Collections.EMPTY_MAP;

        // Map of all names onto their corresponding IDs.
        Map<Integer, String> names = new HashMap<Integer, String>();

        // Get all connection groups having the given IDs
        ConnectionGroupExample example = new ConnectionGroupExample();
        example.createCriteria().andConnection_group_idIn(Lists.newArrayList(ids));
        List<ConnectionGroup> connectionGroups = connectionGroupDAO.selectByExample(example);

        // Produce set of names
        for (ConnectionGroup connectionGroup : connectionGroups)
            names.put(connectionGroup.getConnection_group_id(),
                      connectionGroup.getConnection_group_name());

        return names;

    }

    /**
     * Get the names of all the connection groups defined in the system.
     *
     * @return A Set of names of all the connection groups defined in the system.
     */
    public Set<String> getAllConnectionGroupNames() {

        // Set of all present connection group names
        Set<String> names = new HashSet<String>();

        // Query all connection group names
        List<ConnectionGroup> connectionGroups =
                connectionGroupDAO.selectByExample(new ConnectionGroupExample());
        for (ConnectionGroup connectionGroup : connectionGroups)
            names.add(connectionGroup.getConnection_group_name());

        return names;

    }

    /**
     * Retrieves a translation map of connection group names to their 
     * corresponding IDs.
     *
     * @param ids The IDs of the connection groups to retrieve the names of.
     * @return A map containing the names of all connection groups and their
     *         corresponding IDs.
     */
    public Map<String, Integer> translateNames(List<Integer> ids) {

        // If no IDs given, just return empty map
        if (ids.isEmpty())
            return Collections.EMPTY_MAP;

        // Map of all names onto their corresponding IDs.
        Map<String, Integer> names = new HashMap<String, Integer>();

        // Get all connections having the given IDs
        ConnectionGroupExample example = new ConnectionGroupExample();
        example.createCriteria().andConnection_group_idIn(ids);
        List<ConnectionGroup> connectionGroups = connectionGroupDAO.selectByExample(example);

        // Produce set of names
        for (ConnectionGroup connectionGroup : connectionGroups)
            names.put(connectionGroup.getConnection_group_name(),
                      connectionGroup.getConnection_group_id());

        return names;

    }
    
    /**
     * Returns a list of the IDs of all connection groups with a given parent ID.
     * @param parentID The ID of the parent for all the queried connection groups.
     * @return a list of the IDs of all connection groups with a given parent ID.
     */
    public List<Integer> getAllConnectionGroupIDs(Integer parentID) {
        
        // Create criteria
        ConnectionGroupExample example = new ConnectionGroupExample();
        Criteria criteria = example.createCriteria();
        
        if(parentID != null)
            criteria.andConnection_group_idEqualTo(parentID);
        else
            criteria.andConnection_group_idIsNull();
        
        // Query the connections
        List<ConnectionGroup> connectionGroups = connectionGroupDAO.selectByExample(example);
        
        // List of IDs of connections with the given parent
        List<Integer> connectionGroupIDs = new ArrayList<Integer>();
        
        for(ConnectionGroup connectionGroup : connectionGroups) {
            connectionGroupIDs.add(connectionGroup.getConnection_group_id());
        }
        
        return connectionGroupIDs;
    }

    /**
     * Convert the given database-retrieved Connection into a MySQLConnection.
     * The parameters of the given connection will be read and added to the
     * MySQLConnection in the process.
     *
     * @param connection The connection to convert.
     * @param userID The user who queried this connection.
     * @return A new MySQLConnection containing all data associated with the
     *         specified connection.
     */
    private MySQLConnectionGroup toMySQLConnectionGroup(ConnectionGroup connectionGroup, int userID) {

        // Create new MySQLConnection from retrieved data
        MySQLConnectionGroup mySQLConnectionGroup = mysqlConnectionGroupProvider.get();
        mySQLConnectionGroup.init(
            connectionGroup.getConnection_group_id(),
            connectionGroup.getParent_id(),
            connectionGroup.getConnection_group_name(),
            Integer.toString(connectionGroup.getConnection_group_id()),
            connectionGroup.getType(),
            userID
        );

        return mySQLConnectionGroup;

    }

    /**
     * Get the connection group IDs of all the connection groups defined in the system.
     *
     * @return A list of connection group IDs of all the connection groups defined in the system.
     */
    public List<Integer> getAllConnectionGroupIDs() {

        // Set of all present connection group IDs
        List<Integer> connectionGroupIDs = new ArrayList<Integer>();

        // Query all connection IDs
        List<ConnectionGroup> connections =
                connectionGroupDAO.selectByExample(new ConnectionGroupExample());
        for (ConnectionGroup connection : connections)
            connectionGroupIDs.add(connection.getConnection_group_id());

        return connectionGroupIDs;

    }
}
