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

package org.apache.guacamole.cluster;

import java.util.List;

import java.util.Collection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;

/**
 * Cluster-wide state shared by every Guacamole web application replica.
 *
 * Implementations must tolerate the backing store being unreachable. Methods
 * that cannot safely proceed without it throw GuacamoleException; methods whose
 * callers can degrade to replica-local behavior return a neutral value and
 * report unavailability through isAvailable().
 */
public interface ClusterStore {

    /**
     * Atomically prunes stale members, checks every limit, and takes seats.
     *
     * @param request
     *     The seats to acquire.
     *
     * @return
     *     SUCCESS if every limit was satisfied, otherwise the failure result of
     *     the first key whose limit was reached.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    SeatResult acquireSeats(SeatRequest request) throws GuacamoleException;

    /**
     * Releases seats previously acquired by acquireSeats(). Safe to call more
     * than once for the same tunnel.
     *
     * @param request
     *     The seats to release. Only the tunnel UUID and the key list are used.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void releaseSeats(SeatRequest request) throws GuacamoleException;

    /**
     * Publishes a live tunnel to the cluster, making it visible to every
     * replica and routable by its guacd connection ID.
     *
     * @param registration
     *     The tunnel to publish.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void registerTunnel(TunnelRegistration registration) throws GuacamoleException;

    /**
     * Removes a tunnel and its route from the cluster. Safe to call for a
     * tunnel that is already absent.
     *
     * @param registration
     *     The tunnel to remove.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException;

    /**
     * Refreshes the heartbeat score of every given tunnel, preventing them from
     * ageing out of the cluster indexes.
     *
     * @param registrations
     *     Every tunnel currently owned by this replica.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void heartbeat(Collection<TunnelRegistration> registrations) throws GuacamoleException;

    /**
     * Returns the guacd instance hosting the given guacd connection ID.
     *
     * @param guacdConnectionId
     *     The connection ID issued by guacd in its "ready" instruction.
     *
     * @return
     *     The guacd instance hosting that connection, or null if no live route
     *     exists.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException;

    /**
     * Returns the number of live tunnels currently assigned to the given guacd
     * instance across the whole cluster.
     *
     * @param endpoint
     *     The guacd instance to count.
     *
     * @return
     *     The number of live tunnels, or 0 if the store is unreachable.
     */
    long countTunnels(GuacdEndpoint endpoint);

    /**
     * Returns every tunnel currently live anywhere in the cluster, including
     * those owned by this replica.
     *
     * @return
     *     Every live tunnel, or an empty collection if the store is
     *     unavailable. Never null -- the administrative view degrades to a
     *     replica-local listing rather than failing.
     */
    Collection<TunnelRegistration> listTunnels();

    /**
     * Returns the seat token of the tunnel whose history record has the given
     * UUID.
     *
     * @param recordUuid
     *     The UUID of the connection history record.
     *
     * @return
     *     The seat token identifying that tunnel's cluster state, or null if
     *     no such tunnel is known or the store is unavailable.
     */
    String lookupSeatToken(String recordUuid);

    /**
     * Returns the identity of this replica within the cluster.
     *
     * @return
     *     The identity of this replica, as recorded on every tunnel this
     *     replica owns. Used to decide whether a tunnel can be closed locally
     *     or must be killed through another replica.
     */
    String getNodeId();

    /**
     * Returns whether this store actually coordinates a cluster. A no-op store
     * answers false, which is how callers know that cluster-only routing (such
     * as the join route table) does not apply and non-clustered behaviour
     * should be used instead.
     *
     * @return
     *     true if cluster state is really being shared, false if clustering is
     *     disabled.
     */
    boolean isClustered();

    /**
     * Returns whether the backing store was reachable as of the most recent
     * operation.
     *
     * @return
     *     true if the backing store was reachable as of the most recent
     *     operation, false if callers should degrade to replica-local behavior.
     */
    boolean isAvailable();

    /**
     * Registers the handler invoked when any replica requests a kill.
     *
     * @param handler
     *     The handler to invoke for every kill request received.
     */
    void onKillRequest(ClusterKillHandler handler);

    /**
     * Asks every replica to close the tunnel with the given history record
     * UUID. Only the owning replica will act.
     *
     * @param recordUuid
     *     The UUID of the history record identifying the tunnel to close.
     *
     * @throws GuacamoleException
     *     If the request cannot be published.
     */
    void requestKill(String recordUuid) throws GuacamoleException;

    /**
     * Returns whether the tunnel having the given seat token is still
     * published to the cluster, and has therefore neither been unregistered
     * nor aged out.
     *
     * @param seatToken
     *     The seat token identifying a tunnel.
     *
     * @return
     *     true if that tunnel is still published to the cluster, false if it
     *     has gone or the store is unavailable.
     */
    boolean isTunnelLive(String seatToken);

    /**
     * Publishes the given share key to the cluster, so that it can be redeemed
     * on any replica. Failure to publish is logged rather than thrown: the key
     * then works only on the replica which issued it.
     *
     * @param shareKey
     *     The share key being issued.
     *
     * @param entry
     *     Everything that key resolves to.
     */
    void putShareKey(String shareKey, SharedConnectionEntry entry);

    /**
     * Returns everything the given share key resolves to.
     *
     * @param shareKey
     *     The share key being redeemed.
     *
     * @return
     *     The cluster state behind the given share key, or null if the key is
     *     unknown or cannot be read.
     */
    SharedConnectionEntry getShareKey(String shareKey);

    /**
     * Removes the given share key from the cluster, so that it can no longer
     * be redeemed anywhere.
     *
     * @param shareKey
     *     The share key being removed.
     */
    void removeShareKey(String shareKey);

    /**
     * Registers the handler to be invoked when a share key is revoked anywhere
     * in the cluster, so that tunnels opened from that key on this replica can
     * be closed.
     *
     * @param handler
     *     The handler to invoke on revocation.
     */
    void onShareRevoked(ClusterShareRevocationHandler handler);

    /**
     * Announces to every replica that the given share key has been revoked.
     * Failure to announce is logged rather than thrown: the key is already
     * unredeemable, and only the closing of tunnels already opened from it
     * elsewhere is lost.
     *
     * @param shareKey
     *     The share key which is no longer valid.
     */
    void publishShareRevocation(String shareKey);

    /**
     * Records one authentication failure for the given client address,
     * re-arming the window in which those failures count.
     *
     * @param address
     *     The address the failed request originated from.
     *
     * @param banDurationSeconds
     *     The number of seconds a failure remains counted.
     *
     * @return
     *     The number of failures recorded for that address including this one,
     *     or -1 if the cluster could not be reached. -1 means unknown and never
     *     zero: reporting zero would unban every address for the duration of an
     *     outage.
     */
    int recordAuthenticationFailure(String address, int banDurationSeconds);

    /**
     * Returns the number of authentication failures currently counted against
     * the given client address.
     *
     * @param address
     *     The address being checked.
     *
     * @return
     *     The number of failures counted against that address, zero if none,
     *     or -1 if the cluster could not be reached.
     */
    int getAuthenticationFailures(String address);

    /**
     * Publishes the identity behind a session token, so that the session can be
     * rebuilt on any replica.
     *
     * @param tokenHash
     *     The hash of the token. The token itself is never stored.
     *
     * @param identity
     *     The identity the token resolves to.
     *
     * @param timeoutSeconds
     *     The idle lifetime of the token, in seconds.
     */
    void putToken(String tokenHash, TokenIdentity identity, int timeoutSeconds);

    /**
     * Returns the identity behind the given session token.
     *
     * @param tokenHash
     *     The hash of the token being looked up.
     *
     * @return
     *     The identity behind the given token, or null if the token is unknown
     *     or cannot be read.
     */
    TokenIdentity getToken(String tokenHash);

    /**
     * Removes the given session token, so that it can no longer be rebuilt
     * anywhere.
     *
     * @param tokenHash
     *     The hash of the token being removed.
     */
    void removeToken(String tokenHash);

    /**
     * Refreshes the idle lifetime of the given session token.
     *
     * @param tokenHash
     *     The hash of the token being refreshed.
     *
     * @param timeoutSeconds
     *     The idle lifetime of the token, in seconds.
     */
    void touchToken(String tokenHash, int timeoutSeconds);

    /**
     * Registers the handler to be invoked when a session is logged out anywhere
     * in the cluster, so that a session rebuilt from that token on this replica
     * can be dropped.
     *
     * @param handler
     *     The handler to invoke on logout.
     */
    void onLogout(ClusterLogoutHandler handler);

    /**
     * Announces to every replica that the session behind the given token hash
     * has been logged out.
     *
     * @param tokenHash
     *     The hash of the token which is no longer valid.
     */
    void publishLogout(String tokenHash);

    /**
     * Releases all resources held by this store.
     */
    void shutdown();

    /**
     * Exercises every Redis operation this store depends on, returning one
     * description per operation that failed.
     *
     * Intended to be run once at startup. A restrictive ACL otherwise surfaces
     * as a feature that quietly stopped working, because the failure of any
     * single operation is caught and degraded by design.
     *
     * @return
     *     A description of each failed operation, empty when every operation
     *     succeeded.
     */
    List<String> selfCheck();

    /**
     * Publishes this store's current state as gauges, immediately before a
     * scrape reads them.
     */
    void publishMetrics();

}
