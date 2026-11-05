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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.protocols.ProtocolInfo;
import org.junit.jupiter.api.Test;

public class SecretParameterPolicyTest {

    private static final String[] PROTOCOLS = { "kubernetes", "rdp", "ssh", "telnet", "vnc" };

    static Collection<ProtocolInfo> bundled() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<ProtocolInfo> list = new ArrayList<>();
        for (String name : PROTOCOLS) {
            try (InputStream in = ProtocolInfo.class.getResourceAsStream(
                    "/org/apache/guacamole/protocols/" + name + ".json")) {
                assertNotNull(in, name);
                list.add(mapper.readValue(in, ProtocolInfo.class));
            }
        }
        return list;
    }

    @Test
    public void everyPasswordFieldIsSecret() throws Exception {
        Collection<ProtocolInfo> protocols = bundled();
        SecretParameterPolicy policy = new SecretParameterPolicy(protocols, null);
        int checked = 0;
        for (ProtocolInfo p : protocols)
            for (Form f : p.getConnectionForms())
                for (Field field : f.getFields())
                    if (Field.Type.PASSWORD.equals(field.getType())) {
                        assertTrue(policy.isSecret(field.getName()), field.getName());
                        checked++;
                    }
        assertTrue(checked >= 5, "expected the bundled PASSWORD fields");
    }

    @Test
    public void builtInsAndExtras() throws Exception {
        SecretParameterPolicy policy = new SecretParameterPolicy(bundled(), " totp-seed , api-token ");
        for (String n : SecretParameterPolicy.BUILT_IN)
            assertTrue(policy.isSecret(n), n);
        assertTrue(policy.isSecret("totp-seed"));
        assertTrue(policy.isSecret("api-token"));
    }

    @Test
    public void ordinaryFieldsAreNot() throws Exception {
        SecretParameterPolicy policy = new SecretParameterPolicy(bundled(), null);
        for (String n : new String[] { "hostname", "port", "username", "domain", "host-key", "public-key", "" })
            assertFalse(policy.isSecret(n), n);
    }

}
