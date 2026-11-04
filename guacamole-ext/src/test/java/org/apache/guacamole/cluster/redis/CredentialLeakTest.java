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


package org.apache.guacamole.cluster.redis;

import java.util.List;
import org.apache.guacamole.cluster.SeatRequestBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the paths on which a Redis credential could escape into a log line or
 * an exception message, where the audience is wider than the set who can read
 * the credential itself. The store is pointed at an endpoint on which
 * nothing listens, so every operation fails without needing Docker, and each
 * failure message is checked for the password.
 */
public class CredentialLeakTest {

    /**
     * A distinctive password which must never appear in any exception or
     * self-check message.
     */
    private static final String PASSWORD = "hunter2-not-in-any-message";

    /**
     * Port 1 is reserved and nothing listens there, so every connection
     * attempt fails without needing Docker.
     */
    private static final String URI =
            "redis://guacamole:" + PASSWORD + "@127.0.0.1:1";

    /**
     * Verifies that when seat acquisition fails because Redis is unreachable,
     * the exception reaching the caller (and every cause in its chain) names
     * the endpoint but not the password.
     */
    @Test
    public void aConnectionFailureDoesNotNameThePasswordInAnException() {

        RedisClusterStore store = new RedisClusterStore(URI, 30000L, "node-1");

        try {

            // acquireSeats is the path that propagates rather than degrades,
            // so it is where a client-generated message reaches a caller
            Throwable thrown = assertThrows(Throwable.class,
                    () -> store.acquireSeats(SeatRequestBuilder.forConnection(
                            "t1", "alice", "conn-1", 1, 1)));

            // The message is derived from the URI -- it names the endpoint --
            // so this asserts redaction rather than an empty message
            assertTrue(describe(thrown).contains("127.0.0.1"),
                    "the guard is vacuous if nothing from the URI appears: "
                            + describe(thrown));

            assertFalse(describe(thrown).contains(PASSWORD),
                    "the password reached an exception message: " + describe(thrown));

        }

        finally {
            store.shutdown();
        }

    }

    /**
     * Verifies that none of the failure descriptions returned by the startup
     * self-check contain the password, since each is logged verbatim.
     */
    @Test
    public void theSelfCheckDoesNotNameThePassword() {

        RedisClusterStore store = new RedisClusterStore(URI, 30000L, "node-1");

        try {

            // Every one of these descriptions is logged at ERROR on startup,
            // and each carries a client-generated message verbatim
            List<String> failures = store.selfCheck();

            assertFalse(failures.isEmpty(), "the guard needs failures to inspect");

            for (String failure : failures)
                assertFalse(failure.contains(PASSWORD),
                        "the password reached a self-check message: " + failure);

        }

        finally {
            store.shutdown();
        }

    }

    /**
     * Returns the full text of a throwable and its causes.
     *
     * @param t
     *     The throwable to describe.
     *
     * @return
     *     Every message in the causal chain, concatenated.
     */
    private static String describe(Throwable t) {

        StringBuilder text = new StringBuilder();
        for (Throwable cause = t; cause != null; cause = cause.getCause())
            text.append(cause.toString()).append(' ');

        return text.toString();

    }

}
