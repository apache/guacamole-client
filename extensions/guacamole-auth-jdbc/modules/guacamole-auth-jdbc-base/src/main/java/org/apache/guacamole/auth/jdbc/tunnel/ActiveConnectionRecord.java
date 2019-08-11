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

import com.google.inject.Inject;
import java.util.Date;
import java.util.UUID;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.sharing.SharedConnectionMap;
import org.apache.guacamole.auth.jdbc.sharing.SharedObjectManager;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.user.RemoteAuthenticatedUser;
import org.apache.guacamole.net.AbstractGuacamoleTunnel;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.ConnectionRecord;


/**
 * A connection record implementation that describes an active connection. As
 * the associated connection has not yet ended, getEndDate() will always return
 * null. The associated start date will be the time of this objects creation.
 */
public class ActiveConnectionRecord implements ConnectionRecord {

    /**
     * The user that connected to the connection associated with this connection
     * record.
     */
    private RemoteAuthenticatedUser user;

    /**
     * The balancing group from which the associated connection was chosen, if
     * any. If no balancing group was used, this will be null.
     */
    private ModeledConnectionGroup balancingGroup;

    /**
     * The connection associated with this connection record.
     */
    private ModeledConnection connection;

    /**
     * The sharing profile that was used to access the connection associated
     * with this connection record. If the connection was accessed directly
     * (without involving a sharing profile), this will be null.
     */
    private ModeledSharingProfile sharingProfile;

    /**
     * The time this connection record was created.
     */
    private final Date startDate = new Date();

    /**
     * The UUID that will be assigned to the underlying tunnel.
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * The connection ID of the connection as determined by guacd, not to be
     * confused with the connection identifier determined by the database. This
     * is the ID that must be supplied to guacd if joining this connection.
     */
    private String connectionID;
    
    /**
     * The GuacamoleTunnel used by the connection associated with this
     * connection record.
     */
    private GuacamoleTunnel tunnel;

    /**
     * Map of all currently-shared connections.
     */
    @Inject
    private SharedConnectionMap connectionMap;

    /**
     * Manager which tracks all share keys associated with this connection
     * record. All share keys registered with this manager will automatically be
     * removed from the common SharedConnectionMap once the manager is
     * invalidated.
     */
    private final SharedObjectManager<String> shareKeyManager =
            new SharedObjectManager<String>() {

        @Override
        protected void cleanup(String key) {
            connectionMap.remove(key);
        }

    };

    /**
     * Initializes this connection record, associating it with the given user,
     * connection, balancing connection group, and sharing profile. The given
     * balancing connection group MUST be the connection group from which the
     * given connection was chosen, and the given sharing profile MUST be the
     * sharing profile that was used to share access to the given connection.
     * The start date of this connection record will be the time of its
     * creation.
     *
     * @param user
     *     The user that connected to the connection associated with this
     *     connection record.
     *
     * @param balancingGroup
     *     The balancing group from which the given connection was chosen, or
     *     null if no balancing group is being used.
     *
     * @param connection
     *     The connection to associate with this connection record.
     *
     * @param sharingProfile
     *     The sharing profile that was used to share access to the given
     *     connection, or null if no sharing profile was used.
     */
    private void init(RemoteAuthenticatedUser user,
            ModeledConnectionGroup balancingGroup,
            ModeledConnection connection,
            ModeledSharingProfile sharingProfile) {
        this.user = user;
        this.balancingGroup = balancingGroup;
        this.connection = connection;
        this.sharingProfile = sharingProfile;
    }
   
    /**
     * Initializes this connection record, associating it with the given user,
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
    public void init(RemoteAuthenticatedUser user,
            ModeledConnectionGroup balancingGroup,
            ModeledConnection connection) {
        init(user, balancingGroup, connection, null);
    }

    /**
     * Initializes this connection record, associating it with the given user
     * and connection. The start date of this connection record will be the time
     * of its creation.
     *
     * @param user
     *     The user that connected to the connection associated with this
     *     connection record.
     *
     * @param connection
     *     The connection to associate with this connection record.
     */
    public void init(RemoteAuthenticatedUser user,
            ModeledConnection connection) {
        init(user, null, connection);
    }

    /**
     * Initializes this connection record, associating it with the given user,
     * active connection, and sharing profile. The given sharing profile MUST be
     * the sharing profile that was used to share access to the given
     * connection. The start date of this connection record will be the time of
     * its creation.
     *
     * @param user
     *     The user that connected to the connection associated with this
     *     connection record.
     *
     * @param activeConnection
     *     The active connection which is being shared to the given user via
     *     the given sharing profile.
     *
     * @param sharingProfile
     *     The sharing profile that was used to share access to the given
     *     connection, or null if no sharing profile should be used (access to
     *     the connection is unrestricted).
     */
    public void init(RemoteAuthenticatedUser user,
            ActiveConnectionRecord activeConnection,
            ModeledSharingProfile sharingProfile) {
        init(user, null, activeConnection.getConnection(), sharingProfile);
        this.connectionID = activeConnection.getConnectionID();
    }

    /**
     * Returns the user that connected to the connection associated with this
     * connection record.
     *
     * @return
     *     The user that connected to the connection associated with this
     *     connection record.
     */
    public RemoteAuthenticatedUser getUser() {
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
     * Returns the sharing profile that was used to access the connection
     * associated with this connection record. If the connection was accessed
     * directly (without involving a sharing profile), this will be null.
     *
     * @return
     *     The sharing profile that was used to access the connection
     *     associated with this connection record, or null if the connection
     *     was accessed directly.
     */
    public ModeledSharingProfile getSharingProfile() {
        return sharingProfile;
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

    /**
     * Returns whether this connection record is associated with a connection
     * being used directly, in the absence of a sharing profile. If a connection
     * is shared, this will continue to return false for the connection being
     * shared, but will return true for the connections which join that
     * connection.
     *
     * @return
     *     true if the connection associated with this connection record is
     *     being used directly, false otherwise.
     */
    public boolean isPrimaryConnection() {
        return sharingProfile == null;
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

        // Return sharing profile identifier if known
        if (sharingProfile != null)
            return sharingProfile.getIdentifier();

        // No associated sharing profile
        return null;

    }

    @Override
    public String getSharingProfileName() {

        // Return sharing profile name if known
        if (sharingProfile != null)
            return sharingProfile.getName();

        // No associated sharing profile
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
        return user.getIdentifier();
    }

    @Override
    public boolean isActive() {
        return tunnel != null && tunnel.isOpen();
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
     * @param connectionID
     *     The connection ID assigned to this connection by guacd.
     *
     * @return
     *     The newly-created tunnel associated with this connection record.
     */
    public GuacamoleTunnel assignGuacamoleTunnel(final GuacamoleSocket socket,
            String connectionID) {

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

        // Store connection ID of the primary connection only
        if (isPrimaryConnection())
            this.connectionID = connectionID;

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

    /**
     * Returns the connection ID of the in-progress connection as determined by
     * guacd, not to be confused with the connection identifier determined by
     * the database. This is the ID that must be supplied to guacd if joining
     * this connection. If the in-progress connection is joining another
     * connection, this will be the ID of the connection being joined, NOT the
     * ID of the connection directly represented by this record.
     *
     * @return
     *     The ID of the in-progress connection, as determined by guacd.
     */
    public String getConnectionID() {
        return connectionID;
    }

    /**
     * Registers the given share key with this ActiveConnectionRecord, such that
     * the key is automatically removed from the common SharedConnectionMap when
     * the connection represented by this ActiveConnectionRecord is closed. For
     * share keys to be properly invalidated when the connection being shared is
     * closed, all such share keys MUST be registered with the
     * ActiveConnectionRecord of the connection being shared.
     *
     * @param key
     *     The share key which should automatically be removed from the common
     *     SharedConnectionMap when the connection represented by this
     *     ActiveConnectionRecord is closed.
     */
    public void registerShareKey(String key) {
        shareKeyManager.register(key);
    }

    /**
     * Invalidates this ActiveConnectionRecord and all registered share keys. If
     * any additional share keys are registered after this function is invoked,
     * those keys will be immediately invalidated. This function MUST be invoked
     * when the connection represented by this ActiveConnectionRecord is
     * closing.
     */
    public void invalidate() {
        shareKeyManager.invalidate();
    }

}
