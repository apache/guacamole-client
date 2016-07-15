/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.jdbc.tunnel;

import java.util.Date;
import java.util.UUID;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.apache.guacamole.net.AbstractGuacamoleTunnel;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.ConnectionRecord;


/**
 * A connection record implementation that describes an active connection. As
 * the associated connection has not yet ended, getEndDate() will always return
 * null, and isActive() will always return true. The associated start date will
 * be the time of this objects creation.
 *
 * @author Michael Jumper
 */
public class ActiveConnectionRecord implements ConnectionRecord {

    /**
     * The user that connected to the connection associated with this connection
     * record.
     */
    private final AuthenticatedUser user;

    /**
     * The balancing group from which the associated connection was chosen, if
     * any. If no balancing group was used, this will be null.
     */
    private final ModeledConnectionGroup balancingGroup;

    /**
     * The connection associated with this connection record.
     */
    private final ModeledConnection connection;

    /**
     * The time this connection record was created.
     */
    private final Date startDate = new Date();

    /**
     * The UUID that will be assigned to the underlying tunnel.
     */
    private final UUID uuid = UUID.randomUUID();
    
    /**
     * The GuacamoleTunnel used by the connection associated with this
     * connection record.
     */
    private GuacamoleTunnel tunnel;
    
    /**
     * Creates a new connection record associated with the given user,
     * connection, and balancing connection group. The given balancing
     * connection group MUST be the connection group from which the given
     * connection was chosen. The start date of this connection record will be
     * the time of its creation.
     *
     * @param user
     *     The user that connected to the connection associated with this
     *     connection record.
     *
     * @param balancingGroup
     *     The balancing group from which the given connection was chosen.
     *
     * @param connection
     *     The connection to associate with this connection record.
     */
    public ActiveConnectionRecord(AuthenticatedUser user,
            ModeledConnectionGroup balancingGroup,
            ModeledConnection connection) {
        this.user = user;
        this.balancingGroup = balancingGroup;
        this.connection = connection;
    }

    /**
     * Creates a new connection record associated with the given user and
     * connection. The start date of this connection record will be the time of
     * its creation.
     *
     * @param user
     *     The user that connected to the connection associated with this
     *     connection record.
     *
     * @param connection
     *     The connection to associate with this connection record.
     */
    public ActiveConnectionRecord(AuthenticatedUser user,
            ModeledConnection connection) {
        this(user, null, connection);
    }

    /**
     * Returns the user that connected to the connection associated with this
     * connection record.
     *
     * @return
     *     The user that connected to the connection associated with this
     *     connection record.
     */
    public AuthenticatedUser getUser() {
        return user;
    }

    /**
     * Returns the balancing group from which the connection associated with
     * this connection record was chosen.
     *
     * @return
     *     The balancing group from which the connection associated with this
     *     connection record was chosen.
     */
    public ModeledConnectionGroup getBalancingGroup() {
        return balancingGroup;
    }

    /**
     * Returns the connection associated with this connection record.
     *
     * @return
     *     The connection associated with this connection record.
     */
    public ModeledConnection getConnection() {
        return connection;
    }

    /**
     * Returns whether the connection associated with this connection record
     * was chosen from a balancing group.
     *
     * @return
     *     true if the connection associated with this connection record was
     *     chosen from a balancing group, false otherwise.
     */
    public boolean hasBalancingGroup() {
        return balancingGroup != null;
    }

    @Override
    public String getConnectionIdentifier() {
        return connection.getIdentifier();
    }

    @Override
    public String getConnectionName() {
        return connection.getName();
    }

    @Override
    public String getSharingProfileIdentifier() {
        return null;
    }

    @Override
    public String getSharingProfileName() {
        return null;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {

        // Active connections have not yet ended
        return null;
        
    }

    @Override
    public String getRemoteHost() {
        return user.getRemoteHost();
    }

    @Override
    public String getUsername() {
        return user.getUser().getIdentifier();
    }

    @Override
    public boolean isActive() {

        // Active connections are active by definition
        return true;
        
    }

    /**
     * Returns the GuacamoleTunnel currently associated with the active
     * connection represented by this connection record.
     *
     * @return
     *     The GuacamoleTunnel currently associated with the active connection
     *     represented by this connection record.
     */
    public GuacamoleTunnel getTunnel() {
        return tunnel;
    }

    /**
     * Associates a new GuacamoleTunnel with this connection record using the
     * given socket.
     *
     * @param socket
     *     The GuacamoleSocket to use to create the tunnel associated with this
     *     connection record.
     * 
     * @return
     *     The newly-created tunnel associated with this connection record.
     */
    public GuacamoleTunnel assignGuacamoleTunnel(final GuacamoleSocket socket) {

        // Create tunnel with given socket
        this.tunnel = new AbstractGuacamoleTunnel() {

            @Override
            public GuacamoleSocket getSocket() {
                return socket;
            }
            
            @Override
            public UUID getUUID() {
                return uuid;
            }

        };

        // Return newly-created tunnel
        return this.tunnel;
        
    }

    /**
     * Returns the UUID of the underlying tunnel. If there is no underlying
     * tunnel, this will be the UUID assigned to the underlying tunnel when the
     * tunnel is set.
     *
     * @return
     *     The current or future UUID of the underlying tunnel.
     */
    public UUID getUUID() {
        return uuid;
    }
    
}
