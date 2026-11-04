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

package org.apache.guacamole.auth.jdbc.sharing.connection;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.sharing.SharedObjectManager;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.tunnel.ActiveConnectionRecord;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the semantics/restrictions of a shared connection by associating an
 * active connection with an optional sharing profile. The sharing profile, if
 * present, defines the access provided to users of the shared active
 * connection through its connection parameters. If no sharing profile is
 * present, the shared connection has the same level of access as the original
 * connection.
 */
public class SharedConnectionDefinition {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(SharedConnectionDefinition.class);

    /**
     * The active connection being shared.
     */
    private final ActiveConnectionRecord activeConnection;

    /**
     * The sharing profile which dictates the level of access provided to a user
     * of the shared connection.
     */
    private final ModeledSharingProfile sharingProfile;

    /**
     * The unique key with which a user may access the shared connection.
     */
    private final String shareKey;

    /**
     * The connection being shared. Held directly so that a definition
     * reconstructed from cluster state, whose originating session lives on
     * another replica, can answer without a local record.
     */
    private final ModeledConnection connection;

    /**
     * The connection ID issued by guacd for the session being shared.
     */
    private final String guacdConnectionId;

    /**
     * The identifier of the user who shared the connection.
     */
    private final String sharedBy;

    /**
     * Manager which tracks all tunnels associated with this shared connection
     * definition. All tunnels registered with this manager will be
     * automatically closed once the manager is invalidated.
     */
    private final SharedObjectManager<GuacamoleTunnel> tunnels =
            new SharedObjectManager<GuacamoleTunnel>() {

        @Override
        protected void cleanup(GuacamoleTunnel tunnel) {

            try {
                tunnel.close();
            }
            catch (GuacamoleException e) {
                logger.debug("Unable to close tunnel of shared connection.", e);
            }

        }

    };

    /**
     * Creates a new SharedConnectionDefinition which describes an active
     * connection that can be joined, including the restrictions dictated by a
     * given sharing profile.
     *
     * @param activeConnection
     *     The active connection being shared.
     *
     * @param sharingProfile
     *     A sharing profile whose associated parameters dictate the level of
     *     access provided to the shared connection, or null if the connection
     *     should be given full access.
     *
     * @param shareKey
     *     The unique key with which a user may access the shared connection.
     */
    public SharedConnectionDefinition(ActiveConnectionRecord activeConnection,
            ModeledSharingProfile sharingProfile, String shareKey) {
        this.activeConnection = activeConnection;
        this.sharingProfile = sharingProfile;
        this.shareKey = shareKey;

        // Copied out of the record so that callers can read them the same
        // way whether or not the session is owned by this replica
        this.connection = activeConnection.getConnection();
        this.guacdConnectionId = activeConnection.getConnectionID();
        this.sharedBy = activeConnection.getUser().getIdentifier();
    }

    /**
     * Creates a new SharedConnectionDefinition describing a session owned by
     * another replica, described entirely by cluster state rather than by a
     * record of that session, which exists only on the replica owning it.
     *
     * @param connection
     *     The connection being shared.
     *
     * @param guacdConnectionId
     *     The connection ID issued by guacd for the session being shared.
     *
     * @param sharedBy
     *     The identifier of the user who shared the connection.
     *
     * @param sharingProfile
     *     A sharing profile whose associated parameters dictate the level of
     *     access provided to the shared connection, or null if the connection
     *     should be given full access.
     *
     * @param shareKey
     *     The unique key with which a user may access the shared connection.
     */
    public SharedConnectionDefinition(ModeledConnection connection,
            String guacdConnectionId, String sharedBy,
            ModeledSharingProfile sharingProfile, String shareKey) {
        // No local record exists for a session owned by another replica
        this.activeConnection = null;
        this.sharingProfile = sharingProfile;
        this.shareKey = shareKey;
        this.connection = connection;
        this.guacdConnectionId = guacdConnectionId;
        this.sharedBy = sharedBy;
    }

    /**
     * Returns the ActiveConnectionRecord of the actual in-progress connection
     * being shared, if that session is owned by this replica.
     *
     * @return
     *     The ActiveConnectionRecord being shared, or null if the session being
     *     shared is owned by another replica. Callers needing the connection,
     *     the guacd connection ID or the sharing user must use getConnection(),
     *     getGuacdConnectionId() and getSharedBy(), which answer in both cases.
     */
    public ActiveConnectionRecord getActiveConnection() {
        return activeConnection;
    }

    /**
     * Returns the connection being shared.
     *
     * @return
     *     The connection being shared.
     */
    public ModeledConnection getConnection() {
        return connection;
    }

    /**
     * Returns the connection ID issued by guacd for the session being shared.
     *
     * @return
     *     The connection ID issued by guacd for the session being shared.
     */
    public String getGuacdConnectionId() {
        return guacdConnectionId;
    }

    /**
     * Returns the identifier of the user who shared the connection.
     *
     * @return
     *     The identifier of the user who shared the connection.
     */
    public String getSharedBy() {
        return sharedBy;
    }

    /**
     * Returns the ModeledSharingProfile whose associated parameters dictate the
     * level of access granted to users of the shared connection.
     *
     * @return
     *     A ModeledSharingProfile whose associated parameters dictate the
     *     level of access granted to users of the shared connection.
     */
    public ModeledSharingProfile getSharingProfile() {
        return sharingProfile;
    }

    /**
     * Returns the unique key with which a user may access the shared
     * connection.
     *
     * @return
     *     The unique key with which a user may access the shared connection.
     */
    public String getShareKey() {
        return shareKey;
    }

    /**
     * Registers the given tunnel with this SharedConnectionDefinition, such
     * that the tunnel is automatically closed when this
     * SharedConnectionDefinition is invalidated. For shared connections to be
     * properly closed when the associated share key ceases being valid, the
     * tunnels resulting from the use of the share key MUST be registered to the
     * SharedConnectionDefinition associated with that share key.
     *
     * @param tunnel
     *     The tunnel which should automatically be closed when this
     *     SharedConnectionDefinition is invalidated.
     */
    public void registerTunnel(GuacamoleTunnel tunnel) {
        tunnels.register(tunnel);
    }

    /**
     * Invalidates this SharedConnectionDefinition and closes all registered
     * tunnels. If any additional tunnels are registered after this function is
     * invoked, those tunnels will be immediately closed. This function MUST be
     * invoked when the share key associated with this
     * SharedConnectionDefinition will no longer be used.
     */
    public void invalidate() {
        tunnels.invalidate();
    }

}
