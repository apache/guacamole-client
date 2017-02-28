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

package org.apache.guacamole.protocol;

import org.apache.guacamole.GuacamoleException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit test for GuacamoleParser. Verifies that parsing of the Guacamole
 * protocol works as required.
 */
public class GuacamoleParserTest {

    /**
     * The GuacamoleParser instance being tested.
     */
    private final GuacamoleParser parser = new GuacamoleParser();

    /**
     * Test of append method, of class GuacamoleParser.
     * 
     * @throws GuacamoleException If a parse error occurs while parsing the
     *                            known-good test string.
     */
    @Test
    public void testParser() throws GuacamoleException {

        // Test string
        char buffer[] = "1.a,2.bc,3.def,10.helloworld;4.test,5.test2;0.;3.foo;".toCharArray();
        int offset = 0;
        int length = buffer.length;

        GuacamoleInstruction instruction;
        int parsed;

        // Parse more data
        while (length > 0 && (parsed = parser.append(buffer, offset, length)) != 0) {
            offset += parsed;
            length -= parsed;
        }

        // Validate first test instruction
        assertTrue(parser.hasNext());
        instruction = parser.next();
        assertNotNull(instruction);
        assertEquals(3, instruction.getArgs().size());
        assertEquals("a", instruction.getOpcode());
        assertEquals("bc", instruction.getArgs().get(0));
        assertEquals("def", instruction.getArgs().get(1));
        assertEquals("helloworld", instruction.getArgs().get(2));

        // Parse more data
        while (length > 0 && (parsed = parser.append(buffer, offset, length)) != 0) {
            offset += parsed;
            length -= parsed;
        }

        // Validate second test instruction
        assertTrue(parser.hasNext());
        instruction = parser.next();
        assertNotNull(instruction);
        assertEquals(1, instruction.getArgs().size());
        assertEquals("test", instruction.getOpcode());
        assertEquals("test2", instruction.getArgs().get(0));

        // Parse more data
        while (length > 0 && (parsed = parser.append(buffer, offset, length)) != 0) {
            offset += parsed;
            length -= parsed;
        }

        // Validate third test instruction
        assertTrue(parser.hasNext());
        instruction = parser.next();
        assertNotNull(instruction);
        assertEquals(0, instruction.getArgs().size());
        assertEquals("", instruction.getOpcode());

        // Parse more data
        while (length > 0 && (parsed = parser.append(buffer, offset, length)) != 0) {
            offset += parsed;
            length -= parsed;
        }

        // Validate fourth test instruction
        assertTrue(parser.hasNext());
        instruction = parser.next();
        assertNotNull(instruction);
        assertEquals(0, instruction.getArgs().size());
        assertEquals("foo", instruction.getOpcode());

        // Parse more data
        while (length > 0 && (parsed = parser.append(buffer, offset, length)) != 0) {
            offset += parsed;
            length -= parsed;
        }

        // There should be no more instructions
        assertFalse(parser.hasNext());

    }

}
