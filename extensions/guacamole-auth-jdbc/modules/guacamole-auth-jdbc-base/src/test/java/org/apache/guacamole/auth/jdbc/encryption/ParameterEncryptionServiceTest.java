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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterModel;
import org.junit.jupiter.api.Test;

public class ParameterEncryptionServiceTest {

    static final byte[] K1 = new byte[32];
    static final byte[] K2 = new byte[32];
    static { K2[0] = 1; }

    static ParameterKeyProvider provider(String current, String... kids) throws Exception {
        Map<String, byte[]> keys = new HashMap<>();
        for (String kid : kids)
            keys.put(kid, kid.equals("k1") ? K1 : K2);
        return new PropertyParameterKeyProvider(keys, current);
    }

    static SecretParameterPolicy policy() {
        return new SecretParameterPolicy(Collections.emptyList(), "password");
    }

    static ConnectionParameterModel param(String id, String name, String value) {
        ConnectionParameterModel m = new ConnectionParameterModel();
        m.setConnectionIdentifier(id);
        m.setName(name);
        m.setValue(value);
        return m;
    }

    @Test
    public void encryptsSecretsOnly() throws Exception {
        ParameterEncryptionService svc = new ParameterEncryptionService(provider("k1", "k1"), policy(), false);
        ConnectionParameterModel host = param("7", "hostname", "10.0.0.1");
        ConnectionParameterModel pw = param("7", "password", "s3cret");
        svc.protectConnectionParameters(Arrays.asList(host, pw));
        assertNull(host.getEncryption());
        assertEquals("10.0.0.1", host.getValue());
        assertEquals("k1", pw.getEncryption());
        assertNotEquals("s3cret", pw.getValue());
        assertEquals("s3cret", svc.reveal(ParameterOwner.CONNECTION, "7", "password", pw.getValue(), "k1"));
    }

    @Test
    public void disabledLeavesPlainText() throws Exception {
        ParameterEncryptionService svc = new ParameterEncryptionService(PropertyParameterKeyProvider.DISABLED, policy(), false);
        ConnectionParameterModel pw = param("7", "password", "s3cret");
        svc.protectConnectionParameters(Collections.singletonList(pw));
        assertNull(pw.getEncryption());
        assertEquals("s3cret", pw.getValue());
    }

    @Test
    public void decryptModeWritesPlainText() throws Exception {
        ParameterEncryptionService svc = new ParameterEncryptionService(provider("k1", "k1"), policy(), true);
        ConnectionParameterModel pw = param("7", "password", "s3cret");
        svc.protectConnectionParameters(Collections.singletonList(pw));
        assertNull(pw.getEncryption());
        assertEquals("s3cret", pw.getValue());
    }

    @Test
    public void nullKeyIdIsPlainTextEvenIfBase64() throws Exception {
        ParameterEncryptionService svc = new ParameterEncryptionService(provider("k1", "k1"), policy(), false);
        assertEquals("QUJDREVGR0g=", svc.reveal(ParameterOwner.CONNECTION, "7", "password", "QUJDREVGR0g=", null));
    }

    @Test
    public void emptyValueIsNotEncrypted() throws Exception {
        ParameterEncryptionService svc = new ParameterEncryptionService(provider("k1", "k1"), policy(), false);
        ConnectionParameterModel pw = param("7", "password", "");
        svc.protectConnectionParameters(Collections.singletonList(pw));
        assertNull(pw.getEncryption());
        assertEquals("", pw.getValue());
    }

    @Test
    public void rejectsOversizedSecret() throws Exception {
        ParameterEncryptionService svc = new ParameterEncryptionService(provider("k1", "k1"), policy(), false);
        char[] big = new char[6001];
        Arrays.fill(big, 'a');
        GuacamoleClientException e = assertThrows(GuacamoleClientException.class,
            () -> svc.protectConnectionParameters(Collections.singletonList(param("7", "password", new String(big)))));
        assertTrue(e.getMessage().contains("password"));
    }

    @Test
    public void unknownKeyFailsWithoutLeaking() throws Exception {
        ParameterEncryptionService old = new ParameterEncryptionService(provider("k2", "k2"), policy(), false);
        ConnectionParameterModel pw = param("7", "password", "s3cret");
        old.protectConnectionParameters(Collections.singletonList(pw));
        ParameterEncryptionService svc = new ParameterEncryptionService(provider("k1", "k1"), policy(), false);
        GuacamoleServerException e = assertThrows(GuacamoleServerException.class,
            () -> svc.reveal(ParameterOwner.CONNECTION, "7", "password", pw.getValue(), "k2"));
        assertTrue(e.getMessage().contains("k2") && e.getMessage().contains("7") && e.getMessage().contains("password"));
        assertFalse(e.getMessage().contains(pw.getValue()));
        assertNull(svc.revealOrOmit(ParameterOwner.CONNECTION, "7", "password", pw.getValue(), "k2"));
    }

    @Test
    public void rotationReadsOldWritesNew() throws Exception {
        ParameterEncryptionService before = new ParameterEncryptionService(provider("k1", "k1"), policy(), false);
        ConnectionParameterModel pw = param("7", "password", "s3cret");
        before.protectConnectionParameters(Collections.singletonList(pw));
        ParameterEncryptionService after = new ParameterEncryptionService(provider("k2", "k1", "k2"), policy(), false);
        assertEquals("s3cret", after.reveal(ParameterOwner.CONNECTION, "7", "password", pw.getValue(), "k1"));
        ConnectionParameterModel again = param("7", "password", "s3cret");
        after.protectConnectionParameters(Collections.singletonList(again));
        assertEquals("k2", again.getEncryption());
    }

}
