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

import java.util.Collections;
import java.util.Map;
import org.apache.guacamole.GuacamoleServerException;
import org.junit.jupiter.api.Test;

public class PropertyParameterKeyProviderTest {

    @Test
    public void disabledWhenNoKeys() {
        assertFalse(PropertyParameterKeyProvider.DISABLED.isEnabled());
        assertNull(PropertyParameterKeyProvider.DISABLED.keyFor("k1"));
    }

    @Test
    public void currentKeyMustExist() {
        Map<String, byte[]> keys = Collections.singletonMap("k1", new byte[32]);
        assertThrows(GuacamoleServerException.class,
            () -> new PropertyParameterKeyProvider(keys, "k2"));
        assertThrows(GuacamoleServerException.class,
            () -> new PropertyParameterKeyProvider(keys, null));
    }

    @Test
    public void servesKeys() throws Exception {
        Map<String, byte[]> keys = Collections.singletonMap("k1", new byte[32]);
        ParameterKeyProvider p = new PropertyParameterKeyProvider(keys, "k1");
        assertTrue(p.isEnabled());
        assertEquals("k1", p.currentKeyId());
        assertNotNull(p.keyFor("k1"));
        assertNull(p.keyFor("other"));
    }

}
