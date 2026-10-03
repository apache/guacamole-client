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
     * @return
     *     The identity of this replica, as recorded on every tunnel this
     *     replica owns. Used to decide whether a tunnel can be closed locally
     *     or must be killed through another replica.
     */
    String getNodeId();

    /**
     * @return
     *     true if the backing store was reachable as of the most recent
     *     operation, false if callers should degrade to replica-local behavior.
     */
    boolean isAvailable();

    /**
     * Releases all resources held by this store.
     */
    void shutdown();

}
