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

import com.google.common.base.Preconditions;
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
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionPermissionKey;
import net.sourceforge.guacamole.net.auth.mysql.utility.PermissionCheckUtility;
import net.sourceforge.guacamole.net.auth.mysql.utility.ProviderUtility;
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
    PermissionCheckUtility permissionCheckUtility;
    
    @Inject
    ProviderUtility providerUtility;
    
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
        Preconditions.checkNotNull(object);
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
        List<ConnectionParameter> connectionParameters = connectionParameterDAO.selectByExample(example);
        for(ConnectionParameter parameter : connectionParameters)
            existingConfiguration.put(parameter.getParameter_name(), parameter.getParameter_value());
        
        List<ConnectionParameter> parametersToInsert = new ArrayList<ConnectionParameter>();
        List<ConnectionParameter> parametersToUpdate = new ArrayList<ConnectionParameter>();
        
        Set<String> parameterNames = configuration.getParameterNames();
        
        for(String parameterName : parameterNames) {
            
        }
    }

    @Transactional
    @Override
    public void update(Connection object) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Transactional
    @Override
    public void remove(String identifier) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
