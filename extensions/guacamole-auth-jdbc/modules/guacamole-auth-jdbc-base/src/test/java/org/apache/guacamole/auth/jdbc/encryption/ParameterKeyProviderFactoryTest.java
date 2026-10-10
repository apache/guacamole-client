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
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicy;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

public class ParameterKeyProviderFactoryTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private static class Env extends JDBCEnvironment {

        private final String keys, current, provider;
        private String mode = "encrypt", extraNames;

        Env(String keys, String current, String provider) {
            this.keys = keys;
            this.current = current;
            this.provider = provider;
        }

        @Override public String getParameterEncryptionKeys() { return keys; }
        @Override public String getParameterEncryptionCurrentKey() { return current; }
        @Override public String getParameterEncryptionProvider() { return provider; }
        @Override public String getParameterEncryptionMode() { return mode; }
        @Override public String getParameterEncryptionExtraNames() { return extraNames; }
        @Override public boolean isUserRequired() { return false; }
        @Override public int getAbsoluteMaxConnections() { return 0; }
        @Override public int getBatchSize() { return 100; }
        @Override public int getDefaultMaxConnections() { return 0; }
        @Override public int getDefaultMaxGroupConnections() { return 0; }
        @Override public int getDefaultMaxConnectionsPerUser() { return 0; }
        @Override public int getDefaultMaxGroupConnectionsPerUser() { return 0; }
        @Override public boolean isRecursiveQuerySupported(SqlSession session) { return false; }
        @Override public boolean autoCreateAbsentAccounts() { return false; }
        @Override public String getUsername() { return null; }
        @Override public String getPassword() { return null; }
        @Override public boolean trackExternalConnectionHistory() { return false; }
        @Override public boolean enforceAccessWindowsForActiveSessions() { return false; }
        @Override public PasswordPolicy getPasswordPolicy() { return null; }

    }

    @Test
    public void disabledWhenKeysUnset() throws Exception {
        assertSame(PropertyParameterKeyProvider.DISABLED,
                ParameterKeyProviderFactory.loadConfigured(new Env(null, null, "property")));
    }

    @Test
    public void loadsPropertyProviderViaServiceLoader() throws Exception {
        ParameterKeyProvider p = ParameterKeyProviderFactory.loadConfigured(
                new Env("k1:" + KEY, "k1", "property"));
        assertTrue(p.isEnabled());
        assertEquals("k1", p.currentKeyId());
        assertEquals(32, p.keyFor("k1").length);
    }

    @Test
    public void unknownProviderListsAvailable() {
        GuacamoleServerException e = assertThrows(GuacamoleServerException.class,
                () -> ParameterKeyProviderFactory.loadConfigured(new Env("k1:" + KEY, "k1", "nope")));
        assertTrue(e.getMessage().contains("property"));
    }

    @Test
    public void blankKeysFailLoudly() {
        assertThrows(GuacamoleServerException.class,
                () -> ParameterKeyProviderFactory.loadConfigured(new Env("", "k1", "property")));
        assertThrows(GuacamoleServerException.class,
                () -> ParameterKeyProviderFactory.loadConfigured(new Env("  ", "k1", "property")));
    }

    @Test
    public void refusesSettingsWithoutKeys() {
        GuacamoleServerException e = assertThrows(GuacamoleServerException.class,
                () -> ParameterKeyProviderFactory.loadConfigured(new Env(null, "k1", "property")));
        assertTrue(e.getMessage().contains("current-key"), e.getMessage());
        assertTrue(e.getMessage().contains("parameter-encryption-keys"), e.getMessage());

        assertThrows(GuacamoleServerException.class,
                () -> ParameterKeyProviderFactory.loadConfigured(new Env(null, null, "custom")));

        Env decrypt = new Env(null, null, "property");
        decrypt.mode = "decrypt";
        assertThrows(GuacamoleServerException.class,
                () -> ParameterKeyProviderFactory.loadConfigured(decrypt));

        Env extra = new Env(null, null, "property");
        extra.extraNames = "token";
        assertThrows(GuacamoleServerException.class,
                () -> ParameterKeyProviderFactory.loadConfigured(extra));
    }

}
