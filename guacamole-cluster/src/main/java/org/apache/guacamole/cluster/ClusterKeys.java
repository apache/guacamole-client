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

    private ClusterKeys() {}

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

    public static String connectionIndex(String connectionIdentifier) {
        return "guac:idx:conn:" + escape(connectionIdentifier);
    }

    public static String groupIndex(String groupIdentifier) {
        return "guac:idx:group:" + escape(groupIdentifier);
    }

    public static String userIndex(String username) {
        return "guac:idx:user:" + escape(username);
    }

    public static String guacdIndex(String endpointKey) {
        return "guac:idx:guacd:" + escape(endpointKey);
    }

    public static String userConnectionSeat(String username, String connectionIdentifier) {
        return "guac:seat:user:" + escape(username) + ":" + escape(connectionIdentifier);
    }

    public static String userGroupSeat(String username, String groupIdentifier) {
        return "guac:seat:user:" + escape(username) + ":g:" + escape(groupIdentifier);
    }

    public static String tunnel(String tunnelUuid) {
        return "guac:tunnel:" + escape(tunnelUuid);
    }

    public static String route(String guacdConnectionId) {
        return "guac:route:" + escape(guacdConnectionId);
    }

}
