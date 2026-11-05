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

package org.apache.guacamole.auth.jdbc.encryption;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class ParameterCipherTest {

    private static byte[] key() {
        byte[] k = new byte[ParameterCipher.KEY_BYTES];
        new SecureRandom().nextBytes(k);
        return k;
    }

    @Test
    public void roundTrip() throws Exception {
        byte[] k = key();
        String stored = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "password", "s3cret ü");
        assertEquals("s3cret ü", ParameterCipher.decrypt(k, ParameterOwner.CONNECTION, "7", "password", stored));
    }

    @Test
    public void freshIvEveryTime() {
        byte[] k = key();
        String a = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "password", "same");
        String b = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "password", "same");
        assertNotEquals(a, b);
    }

    @Test
    public void boundToOwnerName() {
        byte[] k = key();
        String stored = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "password", "x");
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(k, ParameterOwner.CONNECTION, "8", "password", stored));
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(k, ParameterOwner.CONNECTION, "7", "passphrase", stored));
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(k, ParameterOwner.SHARING_PROFILE, "7", "password", stored));
    }

    @Test
    public void ownerIdAndNameCannotBeShifted() {
        // "1"+"2x" must not equal "12"+"x": associated data is length-prefixed
        byte[] k = key();
        String stored = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "1", "2x", "v");
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(k, ParameterOwner.CONNECTION, "12", "x", stored));
    }

    @Test
    public void tamperedByteFails() {
        byte[] k = key();
        byte[] raw = Base64.getDecoder().decode(
            ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "password", "x"));
        for (int i = 0; i < raw.length; i++) {
            byte[] copy = Arrays.copyOf(raw, raw.length);
            copy[i] ^= 1;
            String bad = Base64.getEncoder().encodeToString(copy);
            assertThrows(GeneralSecurityException.class,
                () -> ParameterCipher.decrypt(k, ParameterOwner.CONNECTION, "7", "password", bad));
        }
    }

    @Test
    public void wrongKeyFails() {
        String stored = ParameterCipher.encrypt(key(), ParameterOwner.CONNECTION, "7", "password", "x");
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(key(), ParameterOwner.CONNECTION, "7", "password", stored));
    }

    @Test
    public void malformedStoredValueFails() {
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(key(), ParameterOwner.CONNECTION, "7", "password", "not base64!"));
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(key(), ParameterOwner.CONNECTION, "7", "password", "QUJD"));
    }

    @Test
    public void sizeLimit() {
        byte[] k = key();
        char[] max = new char[ParameterCipher.MAX_PLAINTEXT_BYTES];
        Arrays.fill(max, 'a');
        String stored = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "private-key", new String(max));
        assertTrue(stored.length() <= 8192, "encrypted form must fit varchar(8192)");
        char[] over = new char[ParameterCipher.MAX_PLAINTEXT_BYTES + 1];
        Arrays.fill(over, 'a');
        assertThrows(IllegalArgumentException.class,
            () -> ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "private-key", new String(over)));
        // limit is in bytes, not chars: 3001 two-byte chars = 6002 bytes
        char[] wide = new char[3001];
        Arrays.fill(wide, 'é');
        assertEquals(6002, new String(wide).getBytes(StandardCharsets.UTF_8).length);
        assertThrows(IllegalArgumentException.class,
            () -> ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "private-key", new String(wide)));
    }

    @Test
    public void decryptRejectsWrongLengthKey() throws Exception {
        byte[] k = key();
        String stored = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "password", "x");
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(new byte[0], ParameterOwner.CONNECTION, "7", "password", stored));
        assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(new byte[16], ParameterOwner.CONNECTION, "7", "password", stored));
    }

    @Test
    public void decryptRejectsOversizedAssociatedData() throws Exception {
        byte[] k = key();
        String stored = ParameterCipher.encrypt(k, ParameterOwner.CONNECTION, "7", "password", "x");
        char[] oversized = new char[70000];
        Arrays.fill(oversized, 'a');
        String longOwnerId = new String(oversized);
        GeneralSecurityException ex = assertThrows(GeneralSecurityException.class,
            () -> ParameterCipher.decrypt(k, ParameterOwner.CONNECTION, longOwnerId, "password", stored));
        assertFalse(ex.getMessage().contains("aaaa"), "Exception message should not contain plaintext");
    }

}
