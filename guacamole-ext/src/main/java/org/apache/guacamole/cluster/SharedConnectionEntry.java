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

/**
 * Everything a share key resolves to: enough to rebuild a shared connection
 * definition on a replica which does not own the session being shared.
 */
public class SharedConnectionEntry {

    /**
     * Identifies the shared session's cluster state.
     */
    private final String seatToken;

    /**
     * The connection ID issued by guacd for the shared session.
     */
    private final String guacdConnectionId;

    /**
     * The connection being shared, as known to the database.
     */
    private final String connectionIdentifier;

    /**
     * The sharing profile in use, or null if none.
     */
    private final String sharingProfileIdentifier;

    /**
     * Identifier of the user who shared the connection.
     */
    private final String sharedBy;

    /**
     * Creates a new SharedConnectionEntry describing the session behind a
     * share key.
     *
     * @param seatToken
     *     Identifies the shared session's cluster state, used to check that it
     *     is still live.
     *
     * @param guacdConnectionId
     *     The connection ID issued by guacd, which routes the join.
     *
     * @param connectionIdentifier
     *     The connection being shared, as known to the database.
     *
     * @param sharingProfileIdentifier
     *     The sharing profile in use, or null if none.
     *
     * @param sharedBy
     *     Identifier of the user who shared the connection.
     */
    public SharedConnectionEntry(String seatToken, String guacdConnectionId,
            String connectionIdentifier, String sharingProfileIdentifier,
            String sharedBy) {
        this.seatToken = seatToken;
        this.guacdConnectionId = guacdConnectionId;
        this.connectionIdentifier = connectionIdentifier;
        this.sharingProfileIdentifier = sharingProfileIdentifier;
        this.sharedBy = sharedBy;
    }

    /**
     * Returns the seat token identifying the shared session's cluster state.
     *
     * @return
     *     The seat token of the shared session.
     */
    public String getSeatToken() {
        return seatToken;
    }

    /**
     * Returns the connection ID issued by guacd for the shared session.
     *
     * @return
     *     The connection ID issued by guacd.
     */
    public String getGuacdConnectionId() {
        return guacdConnectionId;
    }

    /**
     * Returns the identifier of the connection being shared.
     *
     * @return
     *     The identifier of the connection being shared.
     */
    public String getConnectionIdentifier() {
        return connectionIdentifier;
    }

    /**
     * Returns the identifier of the sharing profile in use.
     *
     * @return
     *     The identifier of the sharing profile in use, or null if the
     *     connection was shared without one.
     */
    public String getSharingProfileIdentifier() {
        return sharingProfileIdentifier;
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

}
