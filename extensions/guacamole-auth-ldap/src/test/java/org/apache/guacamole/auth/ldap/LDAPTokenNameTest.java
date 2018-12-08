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

package org.apache.guacamole.auth.ldap;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test which verifies automatic generation of LDAP-specific connection
 * parameter token names.
 */
public class LDAPTokenNameTest {

    /**
     * Verifies that TokenName.fromAttribute() generates token names as
     * specified, regardless of the naming convention of the attribute.
     */
    @Test
    public void testFromAttribute() {
        assertEquals("LDAP_A", LDAPTokenName.fromAttribute("a"));
        assertEquals("LDAP_B", LDAPTokenName.fromAttribute("b"));
        assertEquals("LDAP_1", LDAPTokenName.fromAttribute("1"));
        assertEquals("LDAP_SOME_URL", LDAPTokenName.fromAttribute("someURL"));
        assertEquals("LDAP_LOWERCASE_WITH_DASHES", LDAPTokenName.fromAttribute("lowercase-with-dashes"));
        assertEquals("LDAP_HEADLESS_CAMEL_CASE", LDAPTokenName.fromAttribute("headlessCamelCase"));
        assertEquals("LDAP_CAMEL_CASE", LDAPTokenName.fromAttribute("CamelCase"));
        assertEquals("LDAP_CAMEL_CASE", LDAPTokenName.fromAttribute("CamelCase"));
        assertEquals("LDAP_LOWERCASE_WITH_UNDERSCORES", LDAPTokenName.fromAttribute("lowercase_with_underscores"));
        assertEquals("LDAP_UPPERCASE_WITH_UNDERSCORES", LDAPTokenName.fromAttribute("UPPERCASE_WITH_UNDERSCORES"));
        assertEquals("LDAP_A_VERY_INCONSISTENT_MIX_OF_ALL_STYLES", LDAPTokenName.fromAttribute("aVery-INCONSISTENTMix_ofAllStyles"));
        assertEquals("LDAP_ABC_123_DEF_456", LDAPTokenName.fromAttribute("abc123def456"));
        assertEquals("LDAP_ABC_123_DEF_456", LDAPTokenName.fromAttribute("ABC123DEF456"));
        assertEquals("LDAP_WORD_A_WORD_AB_WORD_ABC_WORD", LDAPTokenName.fromAttribute("WordAWordABWordABCWord"));
    }

}
