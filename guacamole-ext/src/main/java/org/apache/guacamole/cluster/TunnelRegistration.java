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

import org.apache.guacamole.cluster.guacd.GuacdEndpoint;

/**
 * Immutable description of one live tunnel, as published to the cluster.
 */
public class TunnelRegistration {

    /**
     * The identifier of this tunnel's cluster state. This is the seat token
     * minted when the tunnel's seats were acquired, and is the member stored
     * in every cluster index.
     */
    private final String tunnelUuid;

    /**
     * The identity of the replica which owns this tunnel, used to decide
     * whether the tunnel can be closed locally or must be killed through
     * another replica.
     */
    private final String nodeId;

    /**
     * The connection ID issued by guacd for this tunnel, used to route users
     * joining the connection to the same guacd instance, or null if not
     * known.
     */
    private final String guacdConnectionId;

    /**
     * The guacd instance hosting this tunnel, or null if not known.
     */
    private final GuacdEndpoint endpoint;

    /**
     * The identifier of the connection this tunnel is connected to.
     */
    private final String connectionIdentifier;

    /**
     * The identifier of the balancing connection group through which the
     * connection was reached, or null if it was not reached through one.
     */
    private final String groupIdentifier;

    /**
     * The identifier of the sharing profile in use, or null if this tunnel
     * was not opened through a share.
     */
    private final String sharingProfileIdentifier;

    /**
     * The identifier of the user who opened this tunnel.
     */
    private final String username;

    /**
     * The remote host from which the user opened this tunnel, or null if not
     * known.
     */
    private final String remoteHost;

    /**
     * The time this tunnel was opened, in milliseconds since the epoch.
     */
    private final long startTime;

    /**
     * The UUID of this tunnel's connection history record, or null if no
     * history record was written.
     */
    private final String recordUuid;

    /**
     * Creates a new TunnelRegistration describing a single live tunnel.
     *
     * @param tunnelUuid
     *     The identifier of the tunnel's cluster state, which is the seat
     *     token minted when the tunnel's seats were acquired.
     *
     * @param nodeId
     *     The identity of the replica which owns the tunnel.
     *
     * @param guacdConnectionId
     *     The connection ID issued by guacd for the tunnel, or null if not
     *     known.
     *
     * @param endpoint
     *     The guacd instance hosting the tunnel, or null if not known.
     *
     * @param connectionIdentifier
     *     The identifier of the connection the tunnel is connected to.
     *
     * @param groupIdentifier
     *     The identifier of the balancing connection group through which the
     *     connection was reached, or null if it was not reached through one.
     *
     * @param sharingProfileIdentifier
     *     The identifier of the sharing profile in use, or null if the tunnel
     *     was not opened through a share.
     *
     * @param username
     *     The identifier of the user who opened the tunnel.
     *
     * @param remoteHost
     *     The remote host from which the user opened the tunnel, or null if
     *     not known.
     *
     * @param startTime
     *     The time the tunnel was opened, in milliseconds since the epoch.
     *
     * @param recordUuid
     *     The UUID of the tunnel's connection history record, or null if no
     *     history record was written.
     */
    public TunnelRegistration(String tunnelUuid, String nodeId,
            String guacdConnectionId, GuacdEndpoint endpoint,
            String connectionIdentifier, String groupIdentifier,
            String sharingProfileIdentifier, String username,
            String remoteHost, long startTime, String recordUuid) {
        this.tunnelUuid = tunnelUuid;
        this.nodeId = nodeId;
        this.guacdConnectionId = guacdConnectionId;
        this.endpoint = endpoint;
        this.connectionIdentifier = connectionIdentifier;
        this.groupIdentifier = groupIdentifier;
        this.sharingProfileIdentifier = sharingProfileIdentifier;
        this.username = username;
        this.remoteHost = remoteHost;
        this.startTime = startTime;
        this.recordUuid = recordUuid;
    }

    /**
     * Returns the identifier of this tunnel's cluster state.
     *
     * @return
     *     The identifier of this tunnel's cluster state, which is the seat
     *     token minted when its seats were acquired.
     */
    public String getTunnelUuid() {
        return tunnelUuid;
    }

    /**
     * Returns the identity of the replica which owns this tunnel.
     *
     * @return
     *     The identity of the replica which owns this tunnel.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Returns the connection ID issued by guacd for this tunnel.
     *
     * @return
     *     The connection ID issued by guacd for this tunnel, or null if not
     *     known.
     */
    public String getGuacdConnectionId() {
        return guacdConnectionId;
    }

    /**
     * Returns the guacd instance hosting this tunnel.
     *
     * @return
     *     The guacd instance hosting this tunnel, or null if not known.
     */
    public GuacdEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Returns the identifier of the connection this tunnel is connected to.
     *
     * @return
     *     The identifier of the connection this tunnel is connected to.
     */
    public String getConnectionIdentifier() {
        return connectionIdentifier;
    }

    /**
     * Returns the identifier of the balancing connection group through which
     * the connection was reached.
     *
     * @return
     *     The identifier of the balancing connection group, or null if the
     *     connection was not reached through one.
     */
    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    /**
     * Returns the identifier of the sharing profile in use.
     *
     * @return
     *     The identifier of the sharing profile in use, or null if this tunnel
     *     was not opened through a share.
     */
    public String getSharingProfileIdentifier() {
        return sharingProfileIdentifier;
    }

    /**
     * Returns the identifier of the user who opened this tunnel.
     *
     * @return
     *     The identifier of the user who opened this tunnel.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the remote host from which the user opened this tunnel.
     *
     * @return
     *     The remote host from which the user opened this tunnel, or null if
     *     not known.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Returns the time this tunnel was opened.
     *
     * @return
     *     The time this tunnel was opened, in milliseconds since the epoch.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the UUID of this tunnel's connection history record.
     *
     * @return
     *     The UUID of this connection's history record, which is what the
     *     administrative API uses to identify it, or null if no history record
     *     was written.
     */
    public String getRecordUuid() {
        return recordUuid;
    }

}
