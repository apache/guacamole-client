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
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.cluster.metrics.ClusterMetrics;

/**
 * Cluster store used when clustering is disabled, and as the base for test
 * doubles. Every operation succeeds and does nothing, and isAvailable() reports
 * false so that callers fall back to replica-local behavior.
 */
public class NoOpClusterStore implements ClusterStore {

    @Override
    public SeatResult acquireSeats(SeatRequest request) throws GuacamoleException {
        return SeatResult.SUCCESS;
    }

    @Override
    public void releaseSeats(SeatRequest request) throws GuacamoleException {
        // Nothing is tracked, so nothing is released
    }

    @Override
    public void registerTunnel(TunnelRegistration registration) throws GuacamoleException {
        // Nothing is published when clustering is disabled
    }

    @Override
    public void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException {
        // Nothing is published when clustering is disabled
    }

    @Override
    public void heartbeat(Collection<TunnelRegistration> registrations) throws GuacamoleException {
        // Nothing is published when clustering is disabled
    }

    @Override
    public GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException {
        return null;
    }

    @Override
    public long countTunnels(GuacdEndpoint endpoint) {
        return 0L;
    }

    @Override
    public Collection<TunnelRegistration> listTunnels() {
        return Collections.<TunnelRegistration>emptyList();
    }

    @Override
    public String lookupSeatToken(String recordUuid) {
        return null;
    }

    @Override
    public String getNodeId() {
        return "local";
    }

    @Override
    public boolean isClustered() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void onKillRequest(ClusterKillHandler handler) {
        // Nothing is broadcast when clustering is disabled
    }

    @Override
    public void requestKill(String recordUuid) throws GuacamoleException {
        // Every tunnel is local when clustering is disabled
    }

    @Override
    public boolean isTunnelLive(String seatToken) {
        return false;
    }

    @Override
    public void putShareKey(String shareKey, SharedConnectionEntry entry) {
        // Share keys never leave this replica when clustering is disabled
    }

    @Override
    public SharedConnectionEntry getShareKey(String shareKey) {
        return null;
    }

    @Override
    public void removeShareKey(String shareKey) {
        // Nothing was ever stored
    }

    @Override
    public void onShareRevoked(ClusterShareRevocationHandler handler) {
        // Nothing to subscribe to when clustering is disabled
    }

    @Override
    public void publishShareRevocation(String shareKey) {
        // No other replica exists to announce to
    }

    @Override
    public int recordAuthenticationFailure(String address, int banDurationSeconds) {
        // No cluster exists to count against; the caller falls back to its own
        // replica-local tracking, exactly as without clustering
        return -1;
    }

    @Override
    public int getAuthenticationFailures(String address) {
        return -1;
    }

    @Override
    public void putToken(String tokenHash, TokenIdentity identity, int timeoutSeconds) {
        // Sessions never leave this replica when clustering is disabled
    }

    @Override
    public TokenIdentity getToken(String tokenHash) {
        return null;
    }

    @Override
    public void removeToken(String tokenHash) {
        // Nothing was ever stored
    }

    @Override
    public void touchToken(String tokenHash, int timeoutSeconds) {
        // Nothing was ever stored
    }

    @Override
    public void onLogout(ClusterLogoutHandler handler) {
        // Nothing to subscribe to when clustering is disabled
    }

    @Override
    public void publishLogout(String tokenHash) {
        // No other replica exists to announce to
    }

    @Override
    public void shutdown() {
        // No resources are held
    }

    @Override
    public List<String> selfCheck() {
        // Nothing to check: no cluster, no permissions, no failures
        return Collections.emptyList();
    }

    @Override
    public void publishMetrics() {
        // A deployment with clustering off still publishes the fact, so that
        // "is this replica clustered" is answerable from Prometheus alone
        ClusterMetrics.gauge("guacamole_cluster_enabled", 0);
    }

}
