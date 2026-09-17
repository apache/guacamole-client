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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeatRequestBuilderTest {

    @Test
    public void ordersPerUserLimitBeforeOverallLimit() {

        // Order is the contract: the script reports failure by position, and
        // the caller must be able to tell "this user is at their limit" from
        // "this connection is busy". Upstream throws different exceptions for
        // the two, and only the latter tries the next child of a balancing
        // group.
        SeatRequest request = SeatRequestBuilder.forConnection(
                "t1", "alice", "conn-1", 5, 1);

        List<SeatKey> keys = request.getKeys();
        assertEquals(2, keys.size());
        assertEquals(ClusterKeys.userConnectionSeat("alice", "conn-1"), keys.get(0).getRedisKey());
        assertEquals(SeatResult.USER_CONNECTION_LIMIT, keys.get(0).getFailureResult());
        assertEquals(ClusterKeys.connectionIndex("conn-1"), keys.get(1).getRedisKey());
        assertEquals(SeatResult.CONNECTION_LIMIT, keys.get(1).getFailureResult());

    }

    @Test
    public void carriesTheSeatToken() {
        assertEquals("t1", SeatRequestBuilder.forConnection("t1", "alice", "c", 0, 0).getTunnelUuid());
    }

    @Test
    public void keepsUnlimitedKeysSoTheyAreStillTracked() {

        // A zero limit means unlimited, not "skip". The member must still be
        // added, because guac:idx:conn:{id} is also the cluster's live-session
        // view and the admin listing reads it.
        SeatRequest request = SeatRequestBuilder.forConnection("t1", "alice", "conn-1", 0, 0);
        assertEquals(2, request.getKeys().size());
        assertEquals(0, request.getKeys().get(0).getLimit());

    }

    @Test
    public void buildsGroupKeys() {

        SeatRequest request = SeatRequestBuilder.forGroup("t1", "alice", "grp-1", 10, 2);

        assertEquals(ClusterKeys.userGroupSeat("alice", "grp-1"),
                request.getKeys().get(0).getRedisKey());
        assertEquals(SeatResult.USER_GROUP_LIMIT, request.getKeys().get(0).getFailureResult());
        assertEquals(ClusterKeys.groupIndex("grp-1"),
                request.getKeys().get(1).getRedisKey());
        assertEquals(SeatResult.GROUP_LIMIT, request.getKeys().get(1).getFailureResult());

    }

}
