
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
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionParameterMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionParameter;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionParameterExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.service.ConnectionService;
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.mybatis.guice.transactional.Transactional;

/**
 * A MySQL-based implementation of the connection directory.
 *
 * @author James Muehlner
 */
public class ConnectionDirectory implements Directory<String, Connection>{

    /**
     * The ID of the user who this connection directory belongs to.
     * Access is based on his/her permission settings.
     */
    private int user_id;

    /**
     * Service for checking permissions.
     */
    @Inject
    private PermissionCheckService permissionCheckService;

    /**
     * Service managing connections.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * Service for manipulating connections in the database.
     */
    @Inject
    private ConnectionMapper connectionDAO;

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

    /**
     * Set the user for this directory.
     *
     * @param user_id The ID of the user owning this connection directory.
     */
    public void init(int user_id) {
        this.user_id = user_id;
    }

    @Transactional
    @Override
    public Connection get(String identifier) throws GuacamoleException {
        permissionCheckService.verifyConnectionReadAccess(this.user_id, identifier);
        return connectionService.retrieveConnection(identifier);
    }

    @Transactional
    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        Set<String> connectionNameSet = new HashSet<String>();
        Set<MySQLConnection> connections = permissionCheckService.getReadableConnections(this.user_id);
        for(MySQLConnection mySQLConnection : connections) {
            connectionNameSet.add(mySQLConnection.getIdentifier());
        }
        return connectionNameSet;
    }

    @Transactional
    @Override
    public void add(Connection object) throws GuacamoleException {

        // Verify permission to create
        permissionCheckService.verifyCreateConnectionPermission(this.user_id);

        // Create database object for insert
        net.sourceforge.guacamole.net.auth.mysql.model.Connection connection =
            new net.sourceforge.guacamole.net.auth.mysql.model.Connection();

        connection.setConnection_name(object.getIdentifier());
        connection.setProtocol(object.getConfiguration().getProtocol());
        connectionDAO.insert(connection);

        // Add connection parameters
        createConfigurationValues(connection.getConnection_id(),
                object.getConfiguration());

        // Finally, give the current user full access to the newly created
        // connection.
        ConnectionPermissionKey newConnectionPermission = new ConnectionPermissionKey();
        newConnectionPermission.setUser_id(this.user_id);
        newConnectionPermission.setConnection_id(connection.getConnection_id());

        // Read permission
        newConnectionPermission.setPermission(MySQLConstants.CONNECTION_READ);
        connectionPermissionDAO.insert(newConnectionPermission);

        // Update permission
        newConnectionPermission.setPermission(MySQLConstants.CONNECTION_UPDATE);
        connectionPermissionDAO.insert(newConnectionPermission);

        // Delete permission
        newConnectionPermission.setPermission(MySQLConstants.CONNECTION_DELETE);
        connectionPermissionDAO.insert(newConnectionPermission);

        // Administer permission
        newConnectionPermission.setPermission(MySQLConstants.CONNECTION_ADMINISTER);
        connectionPermissionDAO.insert(newConnectionPermission);

    }

    /**
     * Inserts all parameter values from the given configuration into the
     * database, associating them with the connection having the givenID.
     *
     * @param connection_id The ID of the connection to associate all
     *                      parameters with.
     * @param config The GuacamoleConfiguration to read parameters from.
     */
    private void createConfigurationValues(int connection_id,
            GuacamoleConfiguration config) {

        // Insert new parameters for each parameter in the config
        for (String name : config.getParameterNames()) {

            // Create a ConnectionParameter based on the current parameter
            ConnectionParameter parameter = new ConnectionParameter();
            parameter.setConnection_id(connection_id);
            parameter.setParameter_name(name);
            parameter.setParameter_value(config.getParameter(name));

            // Insert connection parameter
            connectionParameterDAO.insert(parameter);

        }

    }

    @Transactional
    @Override
    public void update(Connection object) throws GuacamoleException {

        // If connection not actually from this auth provider, we can't handle
        // the update
        if (!(object instanceof MySQLConnection))
            throw new GuacamoleException("Connection not from database.");

        // Verify permission to update
        permissionCheckService.verifyConnectionUpdateAccess(this.user_id, object.getIdentifier());

        MySQLConnection mySQLConnection = (MySQLConnection) object;

        // Create database object for insert
        net.sourceforge.guacamole.net.auth.mysql.model.Connection connection =
            new net.sourceforge.guacamole.net.auth.mysql.model.Connection();

        connection.setConnection_id(mySQLConnection.getConnectionID());
        connection.setConnection_name(object.getIdentifier());
        connection.setProtocol(object.getConfiguration().getProtocol());
        connectionDAO.updateByPrimaryKey(connection);

        // Delete old connection parameters
        ConnectionParameterExample parameterExample = new ConnectionParameterExample();
        parameterExample.createCriteria().andConnection_idEqualTo(connection.getConnection_id());
        connectionParameterDAO.deleteByExample(parameterExample);

        // Add connection parameters
        createConfigurationValues(connection.getConnection_id(),
                object.getConfiguration());

    }

    @Transactional
    @Override
    public void remove(String identifier) throws GuacamoleException {

        // Verify permission to delete
        permissionCheckService.verifyConnectionDeleteAccess(this.user_id, identifier);

        // Get connection
        MySQLConnection mySQLConnection =
                connectionService.retrieveConnection(identifier);

        // Delete all configuration values
        ConnectionParameterExample connectionParameterExample = new ConnectionParameterExample();
        connectionParameterExample.createCriteria().andConnection_idEqualTo(mySQLConnection.getConnectionID());
        connectionParameterDAO.deleteByExample(connectionParameterExample);

        // Delete all permissions that refer to this connection
        ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
        connectionPermissionExample.createCriteria().andConnection_idEqualTo(mySQLConnection.getConnectionID());
        connectionPermissionDAO.deleteByExample(connectionPermissionExample);

        // Delete the connection itself
        connectionDAO.deleteByPrimaryKey(mySQLConnection.getConnectionID());

    }

}
