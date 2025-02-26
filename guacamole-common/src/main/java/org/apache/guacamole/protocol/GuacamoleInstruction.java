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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An abstract representation of a Guacamole instruction, as defined by the
 * Guacamole protocol.
 */
public class GuacamoleInstruction {

    /**
     * The opcode of this instruction.
     */
    private final String opcode;

    /**
     * All arguments of this instruction, in order.
     */
    private final List<String> args;

    /**
     * The cached String result of converting this GuacamoleInstruction to the
     * format used by the Guacamole protocol.
     *
     * @see #toString()
     */
    private String rawString = null;

    /**
     * The cached char[] result of converting this GuacamoleInstruction to the
     * format used by the Guacamole protocol.
     *
     * @see #toCharArray()
     */
    private char[] rawChars = null;

    /**
     * Creates a new GuacamoleInstruction having the given opcode and list of
     * argument values.
     *
     * @param opcode
     *     The opcode of the instruction to create.
     *
     * @param args
     *     The list of argument values to provide in the new instruction, if
     *     any.
     */
    public GuacamoleInstruction(String opcode, String... args) {
        this.opcode = opcode;
        this.args = Collections.unmodifiableList(Arrays.asList(args));
    }

    /**
     * Creates a new GuacamoleInstruction having the given opcode and list of
     * argument values. The list given will be used to back the internal list of
     * arguments and the list returned by {@link #getArgs()}.
     * <p>
     * The provided argument list may not be modified in any way after being
     * provided to this constructor. Doing otherwise will result in undefined
     * behavior.
     *
     * @param opcode
     *     The opcode of the instruction to create.
     *
     * @param args
     *     The list of argument values to provide in the new instruction, if
     *     any.
     */
    public GuacamoleInstruction(String opcode, List<String> args) {
        this.opcode = opcode;
        this.args = Collections.unmodifiableList(args);
    }

    /**
     * Returns the opcode associated with this GuacamoleInstruction.
     *
     * @return
     *     The opcode associated with this GuacamoleInstruction.
     */
    public String getOpcode() {
        return opcode;
    }

    /**
     * Returns a List of all argument values specified for this
     * GuacamoleInstruction. Note that the List returned is immutable.
     * Attempts to modify the list will result in exceptions.
     *
     * @return
     *     An unmodifiable List of all argument values specified for this
     *     GuacamoleInstruction.
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Appends the given value to the provided StringBuilder as a Guacamole
     * instruction element, including length prefix.
     *
     * @param buff
     *     The StringBuilder to append the element to.
     *
     * @param element
     *     The string value of the element to append.
     */
    private static void appendElement(StringBuilder buff, String element) {
        buff.append(element.codePointCount(0, element.length()));
        buff.append('.');
        buff.append(element);
    }

    /**
     * Returns this GuacamoleInstruction in the form it would be sent over the
     * Guacamole protocol.
     *
     * @return
     *     This GuacamoleInstruction in the form it would be sent over the
     *     Guacamole protocol.
     */
    @Override
    public String toString() {

        // Avoid rebuilding Guacamole protocol form of instruction if already
        // known
        if (rawString == null) {

            // Prefer to construct String from existing char array, rather than
            // reconstruct protocol from scratch
            if (rawChars != null)
                rawString = new String(rawChars);

            // Reconstruct protocol details only if truly necessary
            else {

                StringBuilder buff = new StringBuilder();

                // Write opcode
                appendElement(buff, opcode);

                // Write argument values
                for (String value : args) {
                    buff.append(',');
                    appendElement(buff, value);
                }

                // Write terminator
                buff.append(';');

                // Cache result for future calls
                rawString = buff.toString();

            }

        }

        return rawString;

    }

    /**
     * Returns this GuacamoleInstruction in the form it would be sent over the
     * Guacamole protocol. The returned char[] MUST NOT be modified. If the
     * returned char[] is modified, the results of doing so are undefined.
     *
     * @return
     *     This GuacamoleInstruction in the form it would be sent over the
     *     Guacamole protocol. The returned char[] MUST NOT be modified.
     */
    public char[] toCharArray() {

        // Avoid rebuilding Guacamole protocol form of instruction if already
        // known
        if (rawChars == null)
            rawChars = toString().toCharArray();

        return rawChars;

    }

}
