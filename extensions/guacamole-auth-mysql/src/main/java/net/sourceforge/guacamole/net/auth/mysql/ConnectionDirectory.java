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
package net.sourceforge.guacamole.net.auth.mysql;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
import net.sourceforge.guacamole.net.auth.mysql.service.ProviderService;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.mybatis.guice.transactional.Transactional;

/**
 *
 * @author James Muehlner
 */
public class ConnectionDirectory implements Directory<String, Connection>{

    /**
     * The user who this connection directory belongs to.
     * Access is based on his/her permission settings.
     */
    private MySQLUser user;

    @Inject
    PermissionCheckService permissionCheckUtility;

    @Inject
    ProviderService providerUtility;

    @Inject
    ConnectionMapper connectionDAO;

    @Inject
    ConnectionPermissionMapper connectionPermissionDAO;

    @Inject
    ConnectionParameterMapper connectionParameterDAO;

    /**
     * Set the user for this directory.
     * @param user
     */
    void init(MySQLUser user) {
        this.user = user;
    }

    @Transactional
    @Override
    public Connection get(String identifier) throws GuacamoleException {
        permissionCheckUtility.verifyConnectionReadAccess(this.user.getUserID(), identifier);
        return providerUtility.getExistingMySQLConnection(identifier);
    }

    @Transactional
    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        Set<String> connectionNameSet = new HashSet<String>();
        Set<MySQLConnection> connections = permissionCheckUtility.getReadableConnections(this.user.getUserID());
        for(MySQLConnection mySQLConnection : connections) {
            connectionNameSet.add(mySQLConnection.getIdentifier());
        }
        return connectionNameSet;
    }

    @Transactional
    @Override
    public void add(Connection object) throws GuacamoleException {
        permissionCheckUtility.verifyCreateConnectionPermission(this.user.getUserID());

        MySQLConnection mySQLConnection = providerUtility.getNewMySQLConnection(object);
        connectionDAO.insert(mySQLConnection.getConnection());

        updateConfigurationValues(mySQLConnection);

        //finally, give the current user full access to the newly created connection.
        ConnectionPermissionKey newConnectionPermission = new ConnectionPermissionKey();
        newConnectionPermission.setUser_id(this.user.getUserID());
        newConnectionPermission.setConnection_id(mySQLConnection.getConnectionID());
        newConnectionPermission.setPermission(MySQLConstants.USER_READ);
        connectionPermissionDAO.insert(newConnectionPermission);
        newConnectionPermission.setPermission(MySQLConstants.USER_UPDATE);
        connectionPermissionDAO.insert(newConnectionPermission);
        newConnectionPermission.setPermission(MySQLConstants.USER_DELETE);
        connectionPermissionDAO.insert(newConnectionPermission);
        newConnectionPermission.setPermission(MySQLConstants.USER_ADMINISTER);
        connectionPermissionDAO.insert(newConnectionPermission);
    }

    /**
     * Saves the values of the configuration to the database
     * @param connection
     */
    private void updateConfigurationValues(MySQLConnection mySQLConnection) {
        GuacamoleConfiguration configuration = mySQLConnection.getConfiguration();
        Map<String, String> existingConfiguration = new HashMap<String, String>();
        ConnectionParameterExample example = new ConnectionParameterExample();
        example.createCriteria().andConnection_idEqualTo(mySQLConnection.getConnectionID());
        List<ConnectionParameter> connectionParameters = connectionParameterDAO.selectByExample(example);
        for(ConnectionParameter parameter : connectionParameters)
            existingConfiguration.put(parameter.getParameter_name(), parameter.getParameter_value());

        List<ConnectionParameter> parametersToInsert = new ArrayList<ConnectionParameter>();
        List<ConnectionParameter> parametersToUpdate = new ArrayList<ConnectionParameter>();

        Set<String> parameterNames = configuration.getParameterNames();

        for(String parameterName : parameterNames) {
            String parameterValue = configuration.getParameter(parameterName);
            if(existingConfiguration.containsKey(parameterName)) {
                String existingValue = existingConfiguration.get(parameterName);
                // the value is different; we'll have to update this one in the database
                if(!parameterValue.equals(existingValue)) {
                    ConnectionParameter parameterToUpdate = new ConnectionParameter();
                    parameterToUpdate.setConnection_id(mySQLConnection.getConnectionID());
                    parameterToUpdate.setParameter_name(parameterName);
                    parameterToUpdate.setParameter_value(parameterValue);
                    parametersToUpdate.add(parameterToUpdate);
                }
            } else {
                // the value is new, we need to insert it
                ConnectionParameter parameterToInsert = new ConnectionParameter();
                parameterToInsert.setConnection_id(mySQLConnection.getConnectionID());
                parameterToInsert.setParameter_name(parameterName);
                parameterToInsert.setParameter_value(parameterValue);
                parametersToInsert.add(parameterToInsert);
            }
        }

        // First, delete all parameters that are not in the new configuration.
        example.clear();
        example.createCriteria().
            andConnection_idEqualTo(mySQLConnection.getConnectionID()).
            andParameter_nameNotIn(Lists.newArrayList(existingConfiguration.keySet()));

        //Second, update all the parameters that need to be modified.
        for(ConnectionParameter parameter : parametersToUpdate) {
            example.clear();
            example.createCriteria().
                andConnection_idEqualTo(mySQLConnection.getConnectionID()).
                andParameter_nameEqualTo(parameter.getParameter_name());

            connectionParameterDAO.updateByExample(parameter, example);
        }

        //Finally, insert any new parameters.
        for(ConnectionParameter parameter : parametersToInsert) {
            example.clear();
            example.createCriteria().
                andConnection_idEqualTo(mySQLConnection.getConnectionID()).
                andParameter_nameEqualTo(parameter.getParameter_name());

            connectionParameterDAO.insert(parameter);
        }
    }

    @Transactional
    @Override
    public void update(Connection object) throws GuacamoleException {
        permissionCheckUtility.verifyConnectionUpdateAccess(this.user.getUserID(), object.getIdentifier());

        MySQLConnection mySQLConnection = providerUtility.getExistingMySQLConnection(object);
        connectionDAO.updateByPrimaryKey(mySQLConnection.getConnection());

        updateConfigurationValues(mySQLConnection);
    }

    @Transactional
    @Override
    public void remove(String identifier) throws GuacamoleException {
        permissionCheckUtility.verifyConnectionDeleteAccess(this.user.getUserID(), identifier);

        MySQLConnection mySQLConnection = providerUtility.getExistingMySQLConnection(identifier);

        // delete all configuration values
        ConnectionParameterExample connectionParameterExample = new ConnectionParameterExample();
        connectionParameterExample.createCriteria().andConnection_idEqualTo(mySQLConnection.getConnectionID());
        connectionParameterDAO.deleteByExample(connectionParameterExample);

        // delete all permissions that refer to this connection
        ConnectionPermissionExample connectionPermissionExample = new ConnectionPermissionExample();
        connectionPermissionExample.createCriteria().andConnection_idEqualTo(mySQLConnection.getConnectionID());
        connectionPermissionDAO.deleteByExample(connectionPermissionExample);

        // delete the connection itself
        connectionDAO.deleteByPrimaryKey(mySQLConnection.getConnectionID());
    }

}
