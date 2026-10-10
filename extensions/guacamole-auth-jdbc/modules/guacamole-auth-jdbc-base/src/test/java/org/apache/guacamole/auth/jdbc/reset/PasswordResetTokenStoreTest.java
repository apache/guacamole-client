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


package org.apache.guacamole.auth.jdbc.reset;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.guacamole.GuacamoleClientTooManyException;
import org.apache.guacamole.GuacamoleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PasswordResetTokenStore against an in-memory mapper.
 */
public class PasswordResetTokenStoreTest {

    private static final long MINUTE = 60L * 1000L;

    private static final Date T0 = new Date(1_700_000_000_000L);

    private final InMemoryPasswordResetTokenMapper mapper = new InMemoryPasswordResetTokenMapper();

    private final PasswordResetTokenStore store = new PasswordResetTokenStore(mapper);

    private static Date at(long minutes) {
        return new Date(T0.getTime() + minutes * MINUTE);
    }

    @Test
    public void tokenIs32RandomUrlSafeBytes() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = PasswordResetTokenStore.generateToken();
            assertTrue(token.matches("[A-Za-z0-9_-]{43}"), token);
            assertEquals(32, Base64.getUrlDecoder().decode(token).length);
            assertTrue(seen.add(token));
        }
    }

    @Test
    public void hashIsSha256() {
        // FIPS 180-2 test vector for "abc"
        byte[] expected = new byte[] {
            (byte) 0xba, (byte) 0x78, (byte) 0x16, (byte) 0xbf, (byte) 0x8f, (byte) 0x01, (byte) 0xcf, (byte) 0xea,
            (byte) 0x41, (byte) 0x41, (byte) 0x40, (byte) 0xde, (byte) 0x5d, (byte) 0xae, (byte) 0x22, (byte) 0x23,
            (byte) 0xb0, (byte) 0x03, (byte) 0x61, (byte) 0xa3, (byte) 0x96, (byte) 0x17, (byte) 0x7a, (byte) 0x9c,
            (byte) 0xb4, (byte) 0x10, (byte) 0xff, (byte) 0x61, (byte) 0xf2, (byte) 0x00, (byte) 0x15, (byte) 0xad
        };
        assertArrayEquals(expected, PasswordResetTokenStore.hash("abc"));
    }

    @Test
    public void onlyTheHashIsStored() throws GuacamoleException {
        String token = store.issue(1, 30, 3, T0);
        assertEquals(1, mapper.rows.size());
        byte[] stored = mapper.rows.get(0).getTokenHash();
        assertArrayEquals(PasswordResetTokenStore.hash(token), stored);
        assertFalse(Arrays.equals(Base64.getUrlDecoder().decode(token), stored));
    }

    @Test
    public void validTokenResolvesToItsUser() throws GuacamoleException {
        String token = store.issue(7, 30, 3, T0);
        assertEquals("user7", store.findUsername(token, at(1)));
        assertNull(store.findUsername("not-a-token", at(1)));
    }

    @Test
    public void tokenExpiresAfterTtl() throws GuacamoleException {
        String token = store.issue(1, 30, 3, T0);
        assertEquals("user1", store.findUsername(token, at(29)));
        assertNull(store.findUsername(token, at(30)));
        assertFalse(store.consume(token, at(31)));
    }

    @Test
    public void tokenIsSingleUse() throws GuacamoleException {
        String token = store.issue(1, 30, 3, T0);
        assertTrue(store.consume(token, at(1)));
        assertFalse(store.consume(token, at(2)));
        assertNull(store.findUsername(token, at(2)));
    }

    @Test
    public void newTokenInvalidatesOlderUnusedTokens() throws GuacamoleException {
        String first = store.issue(1, 30, 3, T0);
        String other = store.issue(2, 30, 3, T0);
        String second = store.issue(1, 30, 3, at(1));
        assertNull(store.findUsername(first, at(2)));
        assertFalse(store.consume(first, at(2)));
        assertEquals("user1", store.findUsername(second, at(2)));
        assertEquals("user2", store.findUsername(other, at(2)), "other users unaffected");
    }

    @Test
    public void rateLimitedPerUserPerHour() throws GuacamoleException {
        store.issue(1, 30, 3, T0);
        store.issue(1, 30, 3, at(10));
        store.issue(1, 30, 3, at(20));
        GuacamoleClientTooManyException e = assertThrows(GuacamoleClientTooManyException.class,
                () -> store.issue(1, 30, 3, at(30)));
        assertEquals(429, e.getStatus().getHttpStatusCode());

        // Another user is unaffected, and the window slides
        store.issue(2, 30, 3, at(30));
        store.issue(1, 30, 3, at(61));
    }

    @Test
    public void oldRowsArePurged() throws GuacamoleException {
        store.issue(1, 30, 3, T0);
        store.issue(2, 30, 3, at(25 * 60));
        assertEquals(1, mapper.rows.size());
    }

}
