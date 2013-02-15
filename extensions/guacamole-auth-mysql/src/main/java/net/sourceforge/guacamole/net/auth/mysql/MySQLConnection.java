/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.guacamole.net.auth.mysql;

import com.google.inject.Inject;
import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionExample;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * A MySQL based implementation of the Connection object.
 * @author James Muehlner
 */
public class MySQLConnection implements Connection {
    
    @Inject
    ConnectionMapper connectionDAO;
    
    private net.sourceforge.guacamole.net.auth.mysql.model.Connection connection;
    
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
     * Load an existing connection by name.
     * @param connectionName 
     */
    public void init(String connectionName) throws GuacamoleException {
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) throws GuacamoleException {
        throw new UnsupportedOperationException("Not supported yet.");
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
}
