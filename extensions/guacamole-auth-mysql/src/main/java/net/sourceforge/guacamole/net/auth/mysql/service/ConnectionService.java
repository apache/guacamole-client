/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.sourceforge.guacamole.net.auth.mysql.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import net.sourceforge.guacamole.net.auth.mysql.AuthenticatedUser;
import net.sourceforge.guacamole.net.auth.mysql.MySQLConnection;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.DirectoryObjectMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionModel;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connections.
 *
 * @author Michael Jumper, James Muehlner
 */
public class ConnectionService extends DirectoryObjectService<MySQLConnection, Connection, ConnectionModel> {

    /**
     * Mapper for accessing connections.
     */
    @Inject
    private ConnectionMapper connectionMapper;

    /**
     * Provider for creating connections.
     */
    @Inject
    private Provider<MySQLConnection> mySQLConnectionProvider;

    @Override
    protected DirectoryObjectMapper<ConnectionModel> getObjectMapper() {
        return connectionMapper;
    }

    @Override
    protected MySQLConnection getObjectInstance(AuthenticatedUser currentUser,
            ConnectionModel model) {
        MySQLConnection connection = mySQLConnectionProvider.get();
        connection.init(currentUser, model);
        return connection;
    }

    @Override
    protected ConnectionModel getModelInstance(AuthenticatedUser currentUser,
            final Connection object) {

        // Create new MySQLConnection backed by blank model
        ConnectionModel model = new ConnectionModel();
        MySQLConnection connection = getObjectInstance(currentUser, model);

        // Set model contents through MySQLConnection, copying the provided connection
        connection.setIdentifier(object.getIdentifier());
        connection.setParentIdentifier(object.getParentIdentifier());
        connection.setName(object.getName());
        connection.setConfiguration(object.getConfiguration());

        return model;
        
    }

    @Override
    protected boolean hasCreatePermission(AuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit user creation permission
        SystemPermissionSet permissionSet = user.getUser().getSystemPermissions();
        return permissionSet.hasPermission(SystemPermission.Type.CREATE_CONNECTION);

    }

    @Override
    protected ObjectPermissionSet getPermissionSet(AuthenticatedUser user)
            throws GuacamoleException {

        // Return permissions related to connections 
        return user.getUser().getConnectionPermissions();

    }

    @Override
    protected void validateNewObject(AuthenticatedUser user, Connection object)
            throws GuacamoleException {

        // Name must not be blank
        if (object.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("Connection names must not be blank.");
        
        // FIXME: Do not attempt to create duplicate connections

    }

    @Override
    protected void validateExistingObject(AuthenticatedUser user,
            MySQLConnection object) throws GuacamoleException {

        // Name must not be blank
        if (object.getIdentifier().trim().isEmpty())
            throw new GuacamoleClientException("Connection names must not be blank.");
        
        // FIXME: Check whether such a connection is already present
        
    }

}
