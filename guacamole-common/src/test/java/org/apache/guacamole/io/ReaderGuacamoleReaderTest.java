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

package org.apache.guacamole.io;

import java.io.StringReader;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the ReaderGuacamoleReader implementation of GuacamoleReader, validating
 * that instructions are parsed correctly.
 */
public class ReaderGuacamoleReaderTest {

    /**
     * Test of ReaderGuacamoleReader parsing.
     * 
     * @throws GuacamoleException If a parse error occurs while parsing the
     *                            known-good test string.
     */
    @Test
    public void testReader() throws GuacamoleException {

        // Test string
        final String test = "1.a,2.bc,3.def,10.helloworld;4.test,5.test2;0.;3.foo;1.\uD83E\uDD79;";

        GuacamoleReader reader = new ReaderGuacamoleReader(new StringReader(test));

        GuacamoleInstruction instruction;

        // Validate first test instruction
        instruction = reader.readInstruction();
        assertNotNull(instruction);
        assertEquals(3, instruction.getArgs().size());
        assertEquals("a", instruction.getOpcode());
        assertEquals("bc", instruction.getArgs().get(0));
        assertEquals("def", instruction.getArgs().get(1));
        assertEquals("helloworld", instruction.getArgs().get(2));

        // Validate second test instruction
        instruction = reader.readInstruction();
        assertNotNull(instruction);
        assertEquals(1, instruction.getArgs().size());
        assertEquals("test", instruction.getOpcode());
        assertEquals("test2", instruction.getArgs().get(0));

        // Validate third test instruction
        instruction = reader.readInstruction();
        assertNotNull(instruction);
        assertEquals(0, instruction.getArgs().size());
        assertEquals("", instruction.getOpcode());

        // Validate fourth test instruction
        instruction = reader.readInstruction();
        assertNotNull(instruction);
        assertEquals(0, instruction.getArgs().size());
        assertEquals("foo", instruction.getOpcode());

        // Validate fifth test instruction
        instruction = reader.readInstruction();
        assertNotNull(instruction);
        assertEquals(0, instruction.getArgs().size());
        assertEquals("\uD83E\uDD79", instruction.getOpcode());    

        // There should be no more instructions
        instruction = reader.readInstruction();
        assertNull(instruction);

    }

    /**
     * Test of ReaderGuacamoleReader's read method.
     *
     * @throws GuacamoleException If an error occurs while reading the instructions.
     */
    @Test
    public void testRead() throws GuacamoleException {
        // Test string containing multiple instructions
        final String test = "3.foo,3.bar;2.az,4.bazz;";

        ReaderGuacamoleReader reader = new ReaderGuacamoleReader(new StringReader(test));

        // Expected character arrays for the instructions
        char[] expectedFirstInstruction = "3.foo,3.bar;".toCharArray();
        char[] expectedSecondInstruction = "2.az,4.bazz;".toCharArray();

        // Read first instruction and verify
        char[] firstInstructionChars = reader.read();
        assertNotNull(firstInstructionChars);
        assertArrayEquals(expectedFirstInstruction, firstInstructionChars);

        // Read second instruction and verify
        char[] secondInstructionChars = reader.read();
        assertNotNull(secondInstructionChars);
        assertArrayEquals(expectedSecondInstruction, secondInstructionChars);

        // Verify that there are no more instructions
        assertNull(reader.read());
    }

}
