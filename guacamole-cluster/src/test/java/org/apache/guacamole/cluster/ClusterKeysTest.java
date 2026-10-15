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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ClusterKeysTest {

    @Test
    public void buildsExpectedKeyShapes() {
        assertEquals("guac:idx:conn:7", ClusterKeys.connectionIndex("7"));
        assertEquals("guac:idx:group:3", ClusterKeys.groupIndex("3"));
        assertEquals("guac:idx:user:alice", ClusterKeys.userIndex("alice"));
        assertEquals("guac:idx:all", ClusterKeys.ALL_INDEX);
        assertEquals("guac:seat:user:alice:7", ClusterKeys.userConnectionSeat("alice", "7"));
        assertEquals("guac:seat:user:alice:g:3", ClusterKeys.userGroupSeat("alice", "3"));
        assertEquals("guac:tunnel:abc", ClusterKeys.tunnel("abc"));
        assertEquals("guac:route:$xyz", ClusterKeys.route("$xyz"));
    }

    @Test
    public void escapesSeparatorsSoSegmentsCannotCollide() {

        // Username "alice:7" with connection "9" must not produce the same key
        // as username "alice" with connection "7:9"
        assertNotEquals(
                ClusterKeys.userConnectionSeat("alice:7", "9"),
                ClusterKeys.userConnectionSeat("alice", "7:9"));

    }

    @Test
    public void escapesPercentSoEscapingIsUnambiguous() {

        // "a%3Ab" must not collide with the escaped form of "a:b"
        assertNotEquals(
                ClusterKeys.userIndex("a%3Ab"),
                ClusterKeys.userIndex("a:b"));

    }

}
