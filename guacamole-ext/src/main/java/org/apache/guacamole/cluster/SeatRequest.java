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

import java.util.Collections;
import java.util.List;

/**
 * A request to hold cluster-wide seats for one tunnel across an ordered set of
 * limit-bearing indexes. The order of the keys is significant: the seat script
 * reports failure by position.
 */
public class SeatRequest {

    /**
     * The identifier of the tunnel holding the seats. This is the member
     * added to, and later removed from, every sorted set in the request.
     */
    private final String tunnelUuid;

    /**
     * The unmodifiable, ordered list of limit-bearing sorted sets in which a
     * seat must be held.
     */
    private final List<SeatKey> keys;

    /**
     * Creates a new SeatRequest for the given tunnel and ordered list of
     * limit-bearing sorted sets.
     *
     * @param tunnelUuid
     *     The identifier of the tunnel holding the seats, such as the seat
     *     token minted at acquire time.
     *
     * @param keys
     *     The limit-bearing sorted sets in which a seat must be held, in the
     *     order in which their limits are checked. The list is wrapped, not
     *     copied, and must not be modified afterwards.
     */
    public SeatRequest(String tunnelUuid, List<SeatKey> keys) {
        this.tunnelUuid = tunnelUuid;
        this.keys = Collections.unmodifiableList(keys);
    }

    /**
     * Returns the identifier of the tunnel holding the seats.
     *
     * @return
     *     The identifier of the tunnel holding the seats.
     */
    public String getTunnelUuid() {
        return tunnelUuid;
    }

    /**
     * Returns the ordered list of limit-bearing sorted sets in which a seat
     * must be held.
     *
     * @return
     *     An unmodifiable, ordered list of the sorted sets in which a seat
     *     must be held.
     */
    public List<SeatKey> getKeys() {
        return keys;
    }

}
