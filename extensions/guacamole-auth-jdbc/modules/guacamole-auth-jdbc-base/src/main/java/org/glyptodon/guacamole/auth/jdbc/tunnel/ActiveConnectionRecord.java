/*
 * Copyright (C) 2015 Glyptodon LLC
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

package org.glyptodon.guacamole.auth.jdbc.tunnel;

import java.util.Date;
import java.util.UUID;
import org.glyptodon.guacamole.auth.jdbc.connection.ModeledConnection;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.net.AbstractGuacamoleTunnel;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;


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
    public String getIdentifier() {
        return connection.getIdentifier();
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

    @Override
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
