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

import java.util.Base64;
import java.util.Map;
import org.apache.guacamole.GuacamoleServerException;
import org.junit.jupiter.api.Test;

public class KeyringTest {

    private static String b64(int len) {
        return Base64.getEncoder().encodeToString(new byte[len]);
    }

    @Test
    public void parsesTwoKeys() throws Exception {
        Map<String, byte[]> keys = Keyring.parseBase64("k1:" + b64(32) + ", k2:" + b64(32));
        assertEquals(2, keys.size());
        assertEquals(32, keys.get("k2").length);
    }

    @Test
    public void rejectsWrongLength() {
        GuacamoleServerException e = assertThrows(GuacamoleServerException.class,
            () -> Keyring.parseBase64("k1:" + b64(16)));
        assertTrue(e.getMessage().contains("k1"));
        assertFalse(e.getMessage().contains(b64(16)), "key material must not appear in errors");
    }

    @Test
    public void rejectsBadBase64() {
        assertThrows(GuacamoleServerException.class, () -> Keyring.parseBase64("k1:!!!"));
    }

    @Test
    public void rejectsMissingSeparatorDuplicateAndBadId() {
        assertThrows(GuacamoleServerException.class, () -> Keyring.parseBase64(b64(32)));
        assertThrows(GuacamoleServerException.class,
            () -> Keyring.parseBase64("k1:" + b64(32) + ",k1:" + b64(32)));
        assertThrows(GuacamoleServerException.class,
            () -> Keyring.parseBase64("bad id:" + b64(32)));
        String longId = new String(new char[65]).replace('\0', 'a');
        assertThrows(GuacamoleServerException.class,
            () -> Keyring.parseBase64(longId + ":" + b64(32)));
    }

}
