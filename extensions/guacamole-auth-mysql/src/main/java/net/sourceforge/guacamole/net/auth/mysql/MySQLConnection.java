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

import com.google.inject.Inject;
import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.ConnectionRecord;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionExample;
import net.sourceforge.guacamole.net.auth.mysql.utility.ProviderUtility;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * A MySQL based implementation of the Connection object.
 * @author James Muehlner
 */
public class MySQLConnection implements Connection {
    
    @Inject
    ConnectionMapper connectionDAO;
    
    @Inject
    ProviderUtility providerUtility;
    
    private net.sourceforge.guacamole.net.auth.mysql.model.Connection connection;
    
    private GuacamoleConfiguration configuration;
    
    /**
     * Create a default, empty connection.
     */
    MySQLConnection() {
        connection = new net.sourceforge.guacamole.net.auth.mysql.model.Connection();
    }
    
    /**
     * Get the ID of the underlying connection record.
     * @return the ID of the underlying connection
     */
    public int getConnectionID() {
        return connection.getConnection_id();
    }
    
    /**
     * Get the underlying connection database record.
     * @return the underlying connection record.
     */
    public net.sourceforge.guacamole.net.auth.mysql.model.Connection getConnection() {
        return connection;
    }
    
    /**
     * Create a new MySQLConnection from this new connection. This is a connection that has not yet been inserted.
     * @param connection 
     */
    public void initNew(Connection connection) {
        configuration = connection.getConfiguration();
        this.connection.setConnection_name(connection.getIdentifier());
        this.configuration = connection.getConfiguration();
    }
    
    /**
     * Load an existing connection by name.
     * @param connectionName 
     */
    public void initExisting(String connectionName) throws GuacamoleException {
        ConnectionExample example = new ConnectionExample();
        example.createCriteria().andConnection_nameEqualTo(connectionName);
        List<net.sourceforge.guacamole.net.auth.mysql.model.Connection> connections;
        connections = connectionDAO.selectByExample(example);
        if(connections.size() > 1) // the unique constraint should prevent this from happening
            throw new GuacamoleException("Multiple connections found named '" + connectionName + "'.");
        else if(connections.isEmpty())
            throw new GuacamoleException("No connection found named '" + connectionName + "'.");
        
        connection = connections.get(0);
    }
    
    /**
     * Initialize from a database record.
     * @param connection 
     */
    public void init(net.sourceforge.guacamole.net.auth.mysql.model.Connection connection) {
        this.connection = connection;
    }

    @Override
    public String getIdentifier() {
        return connection.getConnection_name();
    }

    @Override
    public void setIdentifier(String identifier) {
        connection.setConnection_name(identifier);
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) throws GuacamoleException {
        this.configuration = config;
    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public boolean equals(Object other) {
        if(!(other instanceof MySQLConnection))
            return false;
        return ((MySQLConnection)other).getConnectionID() == this.getConnectionID();
    }
    
    @Override
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException {
        return providerUtility.getExistingMySQLConnectionRecords(connection.getConnection_id());
    }
}
