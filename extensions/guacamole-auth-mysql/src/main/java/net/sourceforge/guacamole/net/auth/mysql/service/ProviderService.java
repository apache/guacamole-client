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
package net.sourceforge.guacamole.net.auth.mysql.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnection;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnectionRecord;
import net.sourceforge.guacamole.net.auth.mysql.MySQLGuacamoleSocket;
import net.sourceforge.guacamole.net.auth.mysql.MySQLUser;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionHistoryMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionParameterMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionHistory;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionHistoryExample;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionParameter;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionParameterExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserExample;
import net.sourceforge.guacamole.net.auth.mysql.model.UserWithBLOBs;
import net.sourceforge.guacamole.protocol.ConfiguredGuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * Provides convenient provider methods for MySQL specific implementations.
 * @author James Muehlner
 */
public class ProviderService {

    @Inject
    private UserMapper userDAO;

    @Inject
    private ConnectionMapper connectionDAO;

    @Inject
    private ConnectionParameterMapper connectionParameterDAO;

    @Inject
    private ConnectionHistoryMapper connectionHistoryDAO;

    @Inject
    private Provider<MySQLUser> mySQLUserProvider;

    @Inject
    private Provider<MySQLConnection> mySQLConnectionProvider;

    @Inject
    private Provider<MySQLConnectionRecord> mySQLConnectionRecordProvider;

    @Inject
    private Provider<MySQLGuacamoleSocket> mySQLGuacamoleSocketProvider;

    /**
     * Service for checking permissions.
     */
    @Inject
    private PermissionCheckService permissionCheckService;

    /**
     * Create a new user based on the provided object.
     * @param user
     * @return the new MySQLUser object.
     * @throws GuacamoleException
     */
    public MySQLUser getNewMySQLUser(User user) throws GuacamoleException {
        MySQLUser mySQLUser = mySQLUserProvider.get();
        mySQLUser.init(user);
        return mySQLUser;
    }

    /**
     * Get the user based on the username of the provided object.
     * @param user
     * @return the new MySQLUser object.
     * @throws GuacamoleException
     */
    public MySQLUser getExistingMySQLUser(User user) throws GuacamoleException {
        return getExistingMySQLUser(user.getUsername());
    }

    /**
     * Get the user based on the username of the provided object.
     * @param name
     * @return the new MySQLUser object.
     * @throws GuacamoleException
     */
    public MySQLUser getExistingMySQLUser(String name) throws GuacamoleException {

        // Query user by ID
        UserExample example = new UserExample();
        example.createCriteria().andUsernameEqualTo(name);
        List<UserWithBLOBs> users = userDAO.selectByExampleWithBLOBs(example);

        // If no user found, return null
        if(users.isEmpty())
            return null;

        // Otherwise, return found user
        return getExistingMySQLUser(users.get(0));

    }

    /**
     * Get an existing MySQLUser from a user database record.
     * @param user
     * @return the existing MySQLUser object.
     */
    public MySQLUser getExistingMySQLUser(UserWithBLOBs user) {
        MySQLUser mySQLUser = mySQLUserProvider.get();
        mySQLUser.init(
            user.getUser_id(),
            user.getUsername(),
            null,
            permissionCheckService.getAllPermissions(user.getUser_id())
        );

        return mySQLUser;
    }

    /**
     * Get an existing MySQLUser from a user ID.
     * @param id
     * @return the existing MySQLUser object if found, null if not.
     */
    public MySQLUser getExistingMySQLUser(Integer id) {

        // Query user by ID
        UserWithBLOBs user = userDAO.selectByPrimaryKey(id);

        // If no user found, return null
        if(user == null)
            return null;

        // Otherwise, return found user
        return getExistingMySQLUser(user);

    }

    /**
     * Get the connection based on the connection name of the provided object.
     * @param connection
     * @return the new Connection object.
     * @throws GuacamoleException
     */
    public MySQLConnection getExistingMySQLConnection(Connection connection) throws GuacamoleException {
        return getExistingMySQLConnection(connection.getIdentifier());
    }

    /**
     * Get the connection based on the connection name of the provided object.
     * @param name
     * @return the new Connection object.
     * @throws GuacamoleException
     */
    public MySQLConnection getExistingMySQLConnection(String name) throws GuacamoleException {

        // Query connection by ID
        ConnectionExample example = new ConnectionExample();
        example.createCriteria().andConnection_nameEqualTo(name);
        List<net.sourceforge.guacamole.net.auth.mysql.model.Connection> connections =
                connectionDAO.selectByExample(example);

        // If no connection found, return null
        if(connections.isEmpty())
            return null;

        // Otherwise, return found connection
        return getExistingMySQLConnection(connections.get(0));

    }

    /**
     * Get an existing MySQLConnection from a connection database record.
     * @param connection
     * @return the existing MySQLConnection object.
     */
    public MySQLConnection getExistingMySQLConnection(net.sourceforge.guacamole.net.auth.mysql.model.Connection connection) {

        // Build configuration
        GuacamoleConfiguration config = new GuacamoleConfiguration();

        // Query parameters for configuration
        ConnectionParameterExample connectionParameterExample = new ConnectionParameterExample();
        connectionParameterExample.createCriteria().andConnection_idEqualTo(connection.getConnection_id());
        List<ConnectionParameter> connectionParameters =
                connectionParameterDAO.selectByExample(connectionParameterExample);

        // Set protocol
        config.setProtocol(connection.getProtocol());

        // Set all values for all parameters
        for (ConnectionParameter parameter : connectionParameters)
            config.setParameter(parameter.getParameter_name(),
                    parameter.getParameter_value());

        // Create new MySQLConnection from retrieved data
        MySQLConnection mySQLConnection = mySQLConnectionProvider.get();
        mySQLConnection.init(
            connection.getConnection_id(),
            connection.getConnection_name(),
            config,
            Collections.EMPTY_LIST // TODO: Read history
        );

        return mySQLConnection;

    }

    /**
     * Get an existing MySQLConnection from a connection ID.
     * @param id
     * @return the existing MySQLConnection object if found, null if not.
     */
    public MySQLConnection getExistingMySQLConnection(Integer id) {

        // Query connection by ID
        net.sourceforge.guacamole.net.auth.mysql.model.Connection connection =
                connectionDAO.selectByPrimaryKey(id);

        // If no connection found, return null
        if(connection == null)
            return null;

        // Otherwise, return found connection
        return getExistingMySQLConnection(connection);

    }

    /**
     * Gets a list of existing MySQLConnectionRecord from the database. These represent
     * the history records of the connection.
     * @param connectionID
     * @return the list of MySQLConnectionRecord related to this connectionID.
     */
    public List<MySQLConnectionRecord> getExistingMySQLConnectionRecords(Integer connectionID) {
        ConnectionHistoryExample example = new ConnectionHistoryExample();
        example.createCriteria().andConnection_idEqualTo(connectionID);
        // we want to return the newest records first
        example.setOrderByClause("start_date DESC");
        List<ConnectionHistory> connectionHistories = connectionHistoryDAO.selectByExample(example);
        List<MySQLConnectionRecord> connectionRecords = new ArrayList<MySQLConnectionRecord>();
        for(ConnectionHistory history : connectionHistories) {
            connectionRecords.add(getExistingMySQLConnectionRecord(history));
        }
        return connectionRecords;
    }

    /**
     * Create a MySQLConnectionRecord object around a single ConnectionHistory database record.
     * @param history
     * @return the new MySQLConnectionRecord object.
     */
    public MySQLConnectionRecord getExistingMySQLConnectionRecord(ConnectionHistory history) {
        MySQLConnectionRecord record = mySQLConnectionRecordProvider.get();
        record.init(history);
        return record;
    }

    /**
     * Create a MySQLGuacamoleSocket using the provided ConfiguredGuacamoleSocket and connection ID.
     * @param socket
     * @param connectionID
     * @return
     */
    public MySQLGuacamoleSocket getMySQLGuacamoleSocket(ConfiguredGuacamoleSocket socket, int connectionID) {
        MySQLGuacamoleSocket mySQLGuacamoleSocket = mySQLGuacamoleSocketProvider.get();
        mySQLGuacamoleSocket.init(socket, connectionID);
        return mySQLGuacamoleSocket;
    }
}
