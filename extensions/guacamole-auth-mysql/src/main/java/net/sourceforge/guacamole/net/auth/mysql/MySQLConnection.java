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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.InetGuacamoleSocket;
import net.sourceforge.guacamole.net.auth.AbstractConnection;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.ConnectionRecord;
import net.sourceforge.guacamole.net.auth.mysql.properties.MySQLGuacamoleProperties;
import net.sourceforge.guacamole.net.auth.mysql.service.ProviderService;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.protocol.ConfiguredGuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * A MySQL based implementation of the Connection object.
 * @author James Muehlner
 */
public class MySQLConnection extends AbstractConnection {

    /**
     * The ID associated with this connection in the database.
     */
    private Integer connectionID;

    /**
     * History of this connection.
     */
    private List<ConnectionRecord> history = new ArrayList<ConnectionRecord>();

    /**
     * Service for creating and retrieving objects.
     */
    @Inject
    private ProviderService providerService;

    /**
     * Set of all currently active connections.
     */
    @Inject
    private ActiveConnectionSet activeConnectionSet;

    /**
     * Create a default, empty connection.
     */
    public MySQLConnection() {
    }

    /**
     * Get the ID of the corresponding connection record.
     * @return The ID of the corresponding connection, if any.
     */
    public Integer getConnectionID() {
        return connectionID;
    }

    /**
     * Sets the ID of the corresponding connection record.
     * @param connectionID The ID to assign to this connection.
     */
    public void setConnectionID(Integer connectionID) {
        this.connectionID = connectionID;
    }

    /**
     * Initialize a new MySQLConnection from this existing connection.
     *
     * @param connection The connection to use when populating the identifier
     *                   and configuration of this connection.
     * @throws GuacamoleException If permission to read the given connection's
     *                            history is denied.
     */
    public void init(Connection connection) throws GuacamoleException {
        init(null, connection.getIdentifier(), connection.getConfiguration(), connection.getHistory());
    }

    /**
     * Initialize from explicit values.
     *
     * @param connectionID The ID of the associated database record, if any.
     * @param identifier The unique identifier associated with this connection.
     * @param config The GuacamoleConfiguration associated with this connection.
     * @param history All ConnectionRecords associated with this connection.
     */
    public void init(Integer connectionID, String identifier,
            GuacamoleConfiguration config,
            List<? extends ConnectionRecord> history) {

        this.connectionID = connectionID;
        setIdentifier(identifier);
        setConfiguration(config);
        this.history.addAll(history);

    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {

        // If the current connection is active, and multiple simultaneous connections are not allowed.
        if(GuacamoleProperties.getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS, false)
                && activeConnectionSet.contains(getConnectionID()))
            throw new GuacamoleException("Cannot connect. This connection is in use.");

        // Get guacd connection information
        String host = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_HOSTNAME);
        int port = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_PORT);

        // Get socket
        GuacamoleSocket socket = providerService.getMySQLGuacamoleSocket(
            new ConfiguredGuacamoleSocket(
                    new InetGuacamoleSocket(host, port),
                    getConfiguration()
            ),
            getConnectionID()
        );

        // Mark this connection as active
        activeConnectionSet.add(getConnectionID());

        return socket;
    }

    @Override
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException {
        return Collections.unmodifiableList(history);
    }

}
