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
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for RedisUris.redact(), which produces a form of a Redis URI that
 * is safe to log. These tests verify that any user and password are replaced,
 * that the scheme (and therefore whether TLS is in use) remains visible, and
 * that a URI which cannot be understood is never echoed.
 */
public class RedisUrisTest {

    /**
     * Verifies that a URI containing no credentials is returned unchanged.
     */
    @Test
    public void aUriWithoutCredentialsIsUnchanged() {
        assertEquals("redis://redis:6379", RedisUris.redact("redis://redis:6379"));
    }

    /**
     * Verifies that a password given without a username is replaced.
     */
    @Test
    public void aPasswordIsRemoved() {
        assertEquals("redis://***@redis:6379",
                RedisUris.redact("redis://:hunter2@redis:6379"));
    }

    /**
     * Verifies that both the username and the password are replaced, while the
     * host and port are kept.
     */
    @Test
    public void aUserAndPasswordAreRemoved() {
        assertEquals("rediss://***@redis.example.com:6379",
                RedisUris.redact("rediss://guacamole:hunter2@redis.example.com:6379"));
    }

    /**
     * Verifies that the "rediss://" scheme is preserved, so the log still shows
     * whether the connection is encrypted.
     */
    @Test
    public void theSchemeSurvivesSoEncryptionRemainsVisible() {
        assertEquals("rediss://***@redis:6379",
                RedisUris.redact("rediss://guacamole:hunter2@redis:6379"));
    }

    /**
     * Verifies that a password which itself contains "@" is removed entirely,
     * with no fragment of it left in the redacted URI.
     */
    @Test
    public void aPasswordContainingAnAtSignIsStillRemoved() {

        // The last "@" delimits the authority, not the first
        String redacted = RedisUris.redact("redis://user:p@ss@redis:6379");
        assertEquals("redis://***@redis:6379", redacted);
        assertFalse(redacted.contains("p@ss"));

    }

    /**
     * Verifies that an unparseable or null URI is replaced by a fixed
     * placeholder rather than being returned as-is.
     */
    @Test
    public void anUnparseableUriIsNotEchoed() {

        // A URI this class does not understand is the one most likely to carry
        // a credential in an unanticipated shape, so it is never echoed
        assertEquals("(redacted)", RedisUris.redact("not-a-uri"));
        assertEquals("(redacted)", RedisUris.redact(null));

    }

}
