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

package org.apache.guacamole.token;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test which verifies automatic generation of LDAP-specific connection
 * parameter token names.
 */
public class TokenNameTest {

    /**
     * Verifies that TokenName.fromAttribute() generates token names as
     * specified, regardless of the naming convention of the attribute.
     */
    @Test
    public void testFromAttribute() {
        assertEquals("A", TokenName.fromAttribute("a"));
        assertEquals("B", TokenName.fromAttribute("b"));
        assertEquals("1", TokenName.fromAttribute("1"));
        assertEquals("SOME_URL", TokenName.fromAttribute("someURL"));
        assertEquals("LOWERCASE_WITH_DASHES", TokenName.fromAttribute("lowercase-with-dashes"));
        assertEquals("HEADLESS_CAMEL_CASE", TokenName.fromAttribute("headlessCamelCase"));
        assertEquals("CAMEL_CASE", TokenName.fromAttribute("CamelCase"));
        assertEquals("CAMEL_CASE", TokenName.fromAttribute("CamelCase"));
        assertEquals("LOWERCASE_WITH_UNDERSCORES", TokenName.fromAttribute("lowercase_with_underscores"));
        assertEquals("UPPERCASE_WITH_UNDERSCORES", TokenName.fromAttribute("UPPERCASE_WITH_UNDERSCORES"));
        assertEquals("A_VERY_INCONSISTENT_MIX_OF_ALL_STYLES", TokenName.fromAttribute("aVery-INCONSISTENTMix_ofAllStyles"));
        assertEquals("ABC_123_DEF_456", TokenName.fromAttribute("abc123def456"));
        assertEquals("ABC_123_DEF_456", TokenName.fromAttribute("ABC123DEF456"));
        assertEquals("WORD_A_WORD_AB_WORD_ABC_WORD", TokenName.fromAttribute("WordAWordABWordABCWord"));
        
        assertEquals("AUTH_ATTRIBUTE", TokenName.fromAttribute("Attribute", "AUTH_"));
        assertEquals("auth_SOMETHING", TokenName.fromAttribute("Something", "auth_"));
    }

}
