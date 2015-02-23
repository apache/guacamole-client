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

package net.sourceforge.guacamole.net.auth.mysql;

import java.util.Collections;
import java.util.List;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionModel;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleUnsupportedException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * A MySQL based implementation of the Connection object.
 * @author James Muehlner
 */
public class MySQLConnection implements Connection, DirectoryObject<ConnectionModel> {

    /**
     * The user this connection belongs to. Access is based on his/her permission
     * settings.
     */
    private AuthenticatedUser currentUser;

    /**
     * The internal model object containing the values which represent this
     * connection in the database.
     */
    private ConnectionModel connectionModel;

    /**
     * Creates a new, empty MySQLConnection.
     */
    public MySQLConnection() {
    }

    @Override
    public void init(AuthenticatedUser currentUser, ConnectionModel connectionModel) {
        this.currentUser = currentUser;
        setModel(connectionModel);
    }

    @Override
    public AuthenticatedUser getCurrentUser() {
        return currentUser;
    }

    @Override
    public void setCurrentUser(AuthenticatedUser currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public ConnectionModel getModel() {
        return connectionModel;
    }

    @Override
    public void setModel(ConnectionModel userModel) {
        this.connectionModel = userModel;
    }

    @Override
    public String getIdentifier() {
        return connectionModel.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        connectionModel.setIdentifier(identifier);
    }

    @Override
    public String getName() {
        return connectionModel.getName();
    }

    @Override
    public void setName(String name) {
        connectionModel.setName(name);
    }

    @Override
    public String getParentIdentifier() {

        // Translate null parent to proper identifier
        String parentIdentifier = connectionModel.getParentIdentifier();
        if (parentIdentifier == null)
            return MySQLConstants.CONNECTION_GROUP_ROOT_IDENTIFIER;

        return parentIdentifier;
        
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {

        // Translate root identifier back into null
        if (parentIdentifier != null
                && parentIdentifier.equals(MySQLConstants.CONNECTION_GROUP_ROOT_IDENTIFIER))
            parentIdentifier = null;

        connectionModel.setParentID(parentIdentifier);

    }

    @Override
    public GuacamoleConfiguration getConfiguration() {

        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(connectionModel.getProtocol());

        /* FIXME: Set parameters, if available */

        return config;
        
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) {

        /* FIXME: Set parameters, if available */

        connectionModel.setProtocol(config.getProtocol());
        
    }

    @Override
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException {
        /* STUB */
        return Collections.EMPTY_LIST;
    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        /* STUB */
        throw new GuacamoleUnsupportedException("STUB - connecting not implemented at the moment");
    }

    @Override
    public int getActiveConnections() {
        /* STUB */
        return 0;
    }

}
