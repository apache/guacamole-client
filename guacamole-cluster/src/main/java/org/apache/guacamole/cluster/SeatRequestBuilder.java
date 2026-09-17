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

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a connection's or group's configured limits into the ordered list
 * of seats that must be held for it.
 *
 * The order of the keys is the contract with the seat script, which reports
 * failure by position: the per-user limit is checked before the overall limit,
 * so that a caller can distinguish "this user is already using this connection"
 * from "this connection is busy". Only the latter should cause a balancing
 * group to try its next child.
 */
public final class SeatRequestBuilder {

    private SeatRequestBuilder() {}

    /**
     * @param seatToken
     *     Identifier for the seats, minted at acquire time.
     *
     * @param username
     *     The user connecting.
     *
     * @param connectionIdentifier
     *     The connection being acquired.
     *
     * @param maxConnections
     *     Overall concurrent-use limit; zero or negative means unlimited.
     *
     * @param maxConnectionsPerUser
     *     Per-user concurrent-use limit; zero or negative means unlimited.
     *
     * @return
     *     The seats which must be held for this connection.
     */
    public static SeatRequest forConnection(String seatToken, String username,
            String connectionIdentifier, int maxConnections,
            int maxConnectionsPerUser) {

        List<SeatKey> keys = new ArrayList<SeatKey>(2);

        keys.add(new SeatKey(
                ClusterKeys.userConnectionSeat(username, connectionIdentifier),
                maxConnectionsPerUser, SeatResult.USER_CONNECTION_LIMIT));

        keys.add(new SeatKey(
                ClusterKeys.connectionIndex(connectionIdentifier),
                maxConnections, SeatResult.CONNECTION_LIMIT));

        return new SeatRequest(seatToken, keys);

    }

    /**
     * @param seatToken
     *     Identifier for the seats, minted at acquire time.
     *
     * @param username
     *     The user connecting.
     *
     * @param groupIdentifier
     *     The balancing group being acquired.
     *
     * @param maxConnections
     *     Overall concurrent-use limit; zero or negative means unlimited.
     *
     * @param maxConnectionsPerUser
     *     Per-user concurrent-use limit; zero or negative means unlimited.
     *
     * @return
     *     The seats which must be held for this group.
     */
    public static SeatRequest forGroup(String seatToken, String username,
            String groupIdentifier, int maxConnections,
            int maxConnectionsPerUser) {

        List<SeatKey> keys = new ArrayList<SeatKey>(2);

        keys.add(new SeatKey(
                ClusterKeys.userGroupSeat(username, groupIdentifier),
                maxConnectionsPerUser, SeatResult.USER_GROUP_LIMIT));

        keys.add(new SeatKey(
                ClusterKeys.groupIndex(groupIdentifier),
                maxConnections, SeatResult.GROUP_LIMIT));

        return new SeatRequest(seatToken, keys);

    }

}
