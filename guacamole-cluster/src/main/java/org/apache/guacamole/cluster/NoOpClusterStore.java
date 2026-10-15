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
    public String getNodeId() {
        return "local";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void shutdown() {
        // No resources are held
    }

}
