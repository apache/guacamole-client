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

    private final String tunnelUuid;
    private final String nodeId;
    private final String guacdConnectionId;
    private final GuacdEndpoint endpoint;
    private final String connectionIdentifier;
    private final String groupIdentifier;
    private final String sharingProfileIdentifier;
    private final String username;
    private final String remoteHost;
    private final long startTime;

    public TunnelRegistration(String tunnelUuid, String nodeId,
            String guacdConnectionId, GuacdEndpoint endpoint,
            String connectionIdentifier, String groupIdentifier,
            String sharingProfileIdentifier, String username,
            String remoteHost, long startTime) {
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
    }

    public String getTunnelUuid() { return tunnelUuid; }
    public String getNodeId() { return nodeId; }
    public String getGuacdConnectionId() { return guacdConnectionId; }
    public GuacdEndpoint getEndpoint() { return endpoint; }
    public String getConnectionIdentifier() { return connectionIdentifier; }
    public String getGroupIdentifier() { return groupIdentifier; }
    public String getSharingProfileIdentifier() { return sharingProfileIdentifier; }
    public String getUsername() { return username; }
    public String getRemoteHost() { return remoteHost; }
    public long getStartTime() { return startTime; }

}
