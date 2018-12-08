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

package org.apache.guacamole.auth.cas;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test which verifies automatic generation of LDAP-specific connection
 * parameter token names.
 */
public class CASTokenNameTest {

    /**
     * Verifies that TokenName.fromAttribute() generates token names as
     * specified, regardless of the naming convention of the attribute.
     */
    @Test
    public void testFromAttribute() {
        assertEquals("CAS_A", CASTokenName.fromAttribute("a"));
        assertEquals("CAS_B", CASTokenName.fromAttribute("b"));
        assertEquals("CAS_1", CASTokenName.fromAttribute("1"));
        assertEquals("CAS_SOME_URL", CASTokenName.fromAttribute("someURL"));
        assertEquals("CAS_LOWERCASE_WITH_DASHES", CASTokenName.fromAttribute("lowercase-with-dashes"));
        assertEquals("CAS_HEADLESS_CAMEL_CASE", CASTokenName.fromAttribute("headlessCamelCase"));
        assertEquals("CAS_CAMEL_CASE", CASTokenName.fromAttribute("CamelCase"));
        assertEquals("CAS_CAMEL_CASE", CASTokenName.fromAttribute("CamelCase"));
        assertEquals("CAS_LOWERCASE_WITH_UNDERSCORES", CASTokenName.fromAttribute("lowercase_with_underscores"));
        assertEquals("CAS_UPPERCASE_WITH_UNDERSCORES", CASTokenName.fromAttribute("UPPERCASE_WITH_UNDERSCORES"));
        assertEquals("CAS_A_VERY_INCONSISTENT_MIX_OF_ALL_STYLES", CASTokenName.fromAttribute("aVery-INCONSISTENTMix_ofAllStyles"));
        assertEquals("CAS_ABC_123_DEF_456", CASTokenName.fromAttribute("abc123def456"));
        assertEquals("CAS_ABC_123_DEF_456", CASTokenName.fromAttribute("ABC123DEF456"));
        assertEquals("CAS_WORD_A_WORD_AB_WORD_ABC_WORD", CASTokenName.fromAttribute("WordAWordABWordABCWord"));
    }

}
