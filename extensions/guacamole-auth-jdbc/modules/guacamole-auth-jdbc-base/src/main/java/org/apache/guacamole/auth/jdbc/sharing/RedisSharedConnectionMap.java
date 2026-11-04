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

package org.apache.guacamole.auth.jdbc.sharing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.connection.ConnectionMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionModel;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.sharing.connection.SharedConnectionDefinition;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileMapper;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileModel;
import org.apache.guacamole.cluster.ClusterShareRevocationHandler;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.SharedConnectionEntry;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SharedConnectionMap which publishes every share key to the cluster store,
 * so that a key issued on one replica can be redeemed on any other. This
 * implementation is bound in place of HashSharedConnectionMap when cluster
 * coordination is enabled.
 *
 * Keys issued locally are also kept in memory: a local definition owns live
 * tunnel state, registering tunnels and invalidating them on revocation, which
 * cluster state cannot hold. A key issued by another replica is instead
 * resolved from the cluster store and rebuilt from the shared database each
 * time it is redeemed.
 */
@Singleton
public class RedisSharedConnectionMap implements SharedConnectionMap {

    /**
     * Logger for this class.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(RedisSharedConnectionMap.class);

    /**
     * The cluster store shared by every replica, in which share keys are
     * published, looked up, and revoked.
     */
    @Inject
    private ClusterStore clusterStore;

    /**
     * Mapper for reading the connection behind a share key which was issued
     * by another replica.
     */
    @Inject
    private ConnectionMapper connectionMapper;

    /**
     * Provider for ModeledConnection objects, used to wrap the connection
     * model read for a share key issued by another replica.
     */
    @Inject
    private Provider<ModeledConnection> connectionProvider;

    /**
     * Mapper for reading the sharing profile behind a share key which was
     * issued by another replica.
     */
    @Inject
    private SharingProfileMapper sharingProfileMapper;

    /**
     * Provider for ModeledSharingProfile objects, used to wrap the sharing
     * profile model read for a share key issued by another replica.
     */
    @Inject
    private Provider<ModeledSharingProfile> sharingProfileProvider;

    /**
     * The environment of the Guacamole server, used to determine whether
     * identifiers are compared case-sensitively when querying the database.
     */
    @Inject
    private JDBCEnvironment environment;

    /**
     * The definitions of all share keys issued by this replica. These are
     * retained in memory because they own the tunnels joined through them,
     * and must close those tunnels when the key is revoked.
     */
    private final HashSharedConnectionMap local = new HashSharedConnectionMap();

    @Override
    public void add(SharedConnectionDefinition definition) {

        // Keep the live definition locally, so tunnels joined on this replica
        // are registered against it
        local.add(definition);

        // Publish just enough for another replica to rebuild the definition:
        // the owning session's seat token (to check it is still live), the
        // guacd connection to join, and the database identifiers involved
        clusterStore.putShareKey(definition.getShareKey(), new SharedConnectionEntry(
                definition.getActiveConnection() != null
                        ? definition.getActiveConnection().getClusterSeatToken() : null,
                definition.getGuacdConnectionId(),
                definition.getConnection().getIdentifier(),
                definition.getSharingProfile() != null
                        ? definition.getSharingProfile().getIdentifier() : null,
                definition.getSharedBy()));

    }

    @Override
    public SharedConnectionDefinition get(String key) {

        // A locally-issued key keeps its live definition
        SharedConnectionDefinition definition = local.get(key);
        if (definition != null)
            return definition;

        // Otherwise the key may have been issued by another replica
        SharedConnectionEntry entry = clusterStore.getShareKey(key);
        if (entry == null)
            return null;

        // The session may have ended on its owning replica, in which case the
        // key is dead and is cleaned up here rather than lingering
        if (entry.getSeatToken() == null
                || !clusterStore.isTunnelLive(entry.getSeatToken())) {
            clusterStore.removeShareKey(key);
            return null;
        }

        return buildRemoteDefinition(key, entry);

    }

    @Override
    public SharedConnectionDefinition remove(String key) {

        // Remove the key cluster-wide, then notify every other replica so that
        // any tunnel joined there through this key is closed as well
        clusterStore.removeShareKey(key);
        clusterStore.publishShareRevocation(key);

        // Closes any tunnels joined on this replica
        return local.remove(key);

    }

    /**
     * Subscribes this replica to share key revocations issued by any replica,
     * so that tunnels joined here through a revoked key are closed. Invoked
     * by Guice once this map has been constructed and its fields injected.
     */
    @Inject
    public void registerRevocationHandler() {
        clusterStore.onShareRevoked(new ClusterShareRevocationHandler() {

            @Override
            public void shareRevoked(String shareKey) {
                // Closes any tunnels registered against a definition this
                // replica issued for the key
                local.remove(shareKey);
            }

        });
    }

    /**
     * Rebuilds a definition for a session owned by another replica.
     *
     * @param key
     *     The share key being redeemed.
     *
     * @param entry
     *     The cluster state that key resolves to.
     *
     * @return
     *     A definition describing the shared session, or null if the connection
     *     behind it can no longer be read.
     */
    private SharedConnectionDefinition buildRemoteDefinition(String key,
            SharedConnectionEntry entry) {

        // The connection and sharing profile both live in the database, which
        // every replica shares
        Collection<ConnectionModel> models = connectionMapper.select(
                Collections.singleton(entry.getConnectionIdentifier()),
                environment.getCaseSensitivity());

        if (models.isEmpty()) {
            logger.warn("Share key \"{}\" refers to connection \"{}\", which no "
                    + "longer exists. The key is now invalid.", key,
                    entry.getConnectionIdentifier());
            return null;
        }

        ConnectionModel model = models.iterator().next();
        ModeledConnection connection = connectionProvider.get();

        // No user is associated: reading the connection behind a share key
        // is exactly the access the key itself grants, and the redeeming
        // user legitimately has no permission on that connection. Nothing
        // on the join path consults the user except getConfiguration(),
        // which is supplied explicitly below.
        connection.init(null, model);

        // A permission-controlled configuration would call through to the
        // (absent) user; the join needs only the protocol, and the tunnel
        // service reads the real parameters straight from the mappers.
        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(model.getProtocol());
        connection.setConfiguration(config);

        ModeledSharingProfile sharingProfile = null;
        if (entry.getSharingProfileIdentifier() != null) {

            Collection<SharingProfileModel> profiles = sharingProfileMapper.select(
                    Collections.singleton(entry.getSharingProfileIdentifier()),
                    environment.getCaseSensitivity());

            if (profiles.isEmpty()) {
                logger.warn("Share key \"{}\" refers to sharing profile \"{}\", "
                        + "which no longer exists. The key is now invalid.", key,
                        entry.getSharingProfileIdentifier());
                return null;
            }

            sharingProfile = sharingProfileProvider.get();
            sharingProfile.init(null, profiles.iterator().next());

        }

        // The remote definition joins the guacd connection named in the
        // entry, rather than an active connection held by this replica
        return new SharedConnectionDefinition(connection,
                entry.getGuacdConnectionId(), entry.getSharedBy(),
                sharingProfile, key);

    }

}
