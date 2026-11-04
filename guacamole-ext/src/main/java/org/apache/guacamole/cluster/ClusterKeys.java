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
 * Construction of every Redis key used by the cluster store. Key strings are
 * never built anywhere else.
 *
 * Identifier segments may contain arbitrary user-supplied text, including the
 * ":" separator. Segments are therefore escaped so that two different pairs of
 * segments can never produce the same key.
 */
public final class ClusterKeys {

    /**
     * Sorted set containing every active tunnel in the cluster.
     */
    public static final String ALL_INDEX = "guac:idx:all";

    /**
     * Channel on which kill requests are published.
     */
    public static final String KILL_CHANNEL = "guac:kill";

    /**
     * Channel on which share key revocations are published.
     */
    public static final String SHARE_REVOKE_CHANNEL = "guac:share:revoke";

    /**
     * Channel on which logouts are published.
     */
    public static final String LOGOUT_CHANNEL = "guac:logout";

    /**
     * Escapes a single key segment. "%" is escaped first so that the escaping
     * is reversible and unambiguous, then ":" is replaced.
     *
     * @param segment
     *     The raw, possibly user-supplied segment.
     *
     * @return
     *     The segment, safe for use between ":" separators.
     */
    private static String escape(String segment) {
        if (segment == null)
            return "";
        return segment.replace("%", "%25").replace(":", "%3A");
    }

    /**
     * Returns the key of the sorted set indexing every active tunnel to the
     * connection having the given identifier.
     *
     * @param connectionIdentifier
     *     The identifier of the connection.
     *
     * @return
     *     The key of the per-connection tunnel index.
     */
    public static String connectionIndex(String connectionIdentifier) {
        return "guac:idx:conn:" + escape(connectionIdentifier);
    }

    /**
     * Returns the key of the sorted set indexing every active tunnel to the
     * connection group having the given identifier.
     *
     * @param groupIdentifier
     *     The identifier of the connection group.
     *
     * @return
     *     The key of the per-connection-group tunnel index.
     */
    public static String groupIndex(String groupIdentifier) {
        return "guac:idx:group:" + escape(groupIdentifier);
    }

    /**
     * Returns the key of the sorted set indexing every active tunnel opened
     * by the user having the given username.
     *
     * @param username
     *     The username of the user.
     *
     * @return
     *     The key of the per-user tunnel index.
     */
    public static String userIndex(String username) {
        return "guac:idx:user:" + escape(username);
    }

    /**
     * Returns the key of the sorted set indexing every active tunnel routed
     * through the guacd endpoint having the given key.
     *
     * @param endpointKey
     *     The key identifying the guacd endpoint, such as its hostname and
     *     port.
     *
     * @return
     *     The key of the per-guacd tunnel index.
     */
    public static String guacdIndex(String endpointKey) {
        return "guac:idx:guacd:" + escape(endpointKey);
    }

    /**
     * Returns the key of the seat tracking the tunnels the given user has
     * open to the given connection, used to enforce per-user connection
     * limits across the cluster.
     *
     * @param username
     *     The username of the user occupying the seat.
     *
     * @param connectionIdentifier
     *     The identifier of the connection.
     *
     * @return
     *     The key of the user/connection seat.
     */
    public static String userConnectionSeat(String username, String connectionIdentifier) {
        return "guac:seat:user:" + escape(username) + ":" + escape(connectionIdentifier);
    }

    /**
     * Returns the key of the seat tracking the tunnels the given user has
     * open to the given connection group, used to enforce per-user
     * connection group limits across the cluster. The "g" segment keeps
     * these keys distinct from user/connection seats.
     *
     * @param username
     *     The username of the user occupying the seat.
     *
     * @param groupIdentifier
     *     The identifier of the connection group.
     *
     * @return
     *     The key of the user/connection group seat.
     */
    public static String userGroupSeat(String username, String groupIdentifier) {
        return "guac:seat:user:" + escape(username) + ":g:" + escape(groupIdentifier);
    }

    /**
     * Returns the key of the hash holding the cluster-wide description of the
     * tunnel having the given UUID.
     *
     * @param tunnelUuid
     *     The UUID of the tunnel.
     *
     * @return
     *     The key of the tunnel's description.
     */
    public static String tunnel(String tunnelUuid) {
        return "guac:tunnel:" + escape(tunnelUuid);
    }

    /**
     * Returns the key mapping the history record having the given UUID to the
     * tunnel which produced it.
     *
     * @param recordUuid
     *     The UUID of the history record.
     *
     * @return
     *     The key of the history record mapping.
     */
    public static String record(String recordUuid) {
        return "guac:record:" + escape(recordUuid);
    }

    /**
     * Returns the key recording which guacd endpoint hosts the guacd
     * connection having the given ID, allowing joining users to be routed
     * to the same guacd instance.
     *
     * @param guacdConnectionId
     *     The ID assigned to the connection by guacd.
     *
     * @return
     *     The key of the route for that guacd connection.
     */
    public static String route(String guacdConnectionId) {
        return "guac:route:" + escape(guacdConnectionId);
    }

    /**
     * Returns the key holding the cluster-wide entry for the given share key.
     *
     * @param shareKey
     *     The share key.
     *
     * @return
     *     The key of the share key's entry.
     */
    public static String shareKey(String shareKey) {
        return "guac:share:" + escape(shareKey);
    }

    /**
     * Returns the key counting recent authentication failures from the given
     * address, used to apply authentication throttling consistently across
     * every replica.
     *
     * @param address
     *     The address from which the failed authentication attempts
     *     originated.
     *
     * @return
     *     The key of the address's authentication failure counter.
     */
    public static String authFailure(String address) {
        return "guac:authfail:" + escape(address);
    }

    /**
     * Key written and deleted by the startup self-check. It sits under
     * "guac:" so that an ACL scoped to "~guac:*" covers it without a special
     * case.
     *
     * @return
     *     The probe key used by the startup self-check.
     */
    public static String selfCheckProbe() {
        return "guac:selfcheck";
    }

    /**
     * Returns the key holding the cluster-wide identity of the session token
     * having the given hash. Only a hash of the token is ever used, so the
     * token itself is never stored in Redis.
     *
     * @param tokenHash
     *     The hash of the session token.
     *
     * @return
     *     The key of the token's identity.
     */
    public static String token(String tokenHash) {
        return "guac:token:" + escape(tokenHash);
    }

}
