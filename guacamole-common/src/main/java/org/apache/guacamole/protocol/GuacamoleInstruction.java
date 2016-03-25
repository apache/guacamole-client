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
 *
 * @author Michael Jumper
 */
public class GuacamoleInstruction {

    /**
     * The opcode of this instruction.
     */
    private String opcode;

    /**
     * All arguments of this instruction, in order.
     */
    private List<String> args;

    /**
     * Creates a new GuacamoleInstruction having the given Operation and
     * list of arguments values.
     *
     * @param opcode The opcode of the instruction to create.
     * @param args The list of argument values to provide in the new
     *             instruction if any.
     */
    public GuacamoleInstruction(String opcode, String... args) {
        this.opcode = opcode;
        this.args = Collections.unmodifiableList(Arrays.asList(args));
    }

    /**
     * Creates a new GuacamoleInstruction having the given Operation and
     * list of arguments values. The list given will be used to back the
     * internal list of arguments and the list returned by getArgs().
     *
     * @param opcode The opcode of the instruction to create.
     * @param args The list of argument values to provide in the new
     *             instruction if any.
     */
    public GuacamoleInstruction(String opcode, List<String> args) {
        this.opcode = opcode;
        this.args = Collections.unmodifiableList(args);
    }

    /**
     * Returns the opcode associated with this GuacamoleInstruction.
     * @return The opcode associated with this GuacamoleInstruction.
     */
    public String getOpcode() {
        return opcode;
    }

    /**
     * Returns a List of all argument values specified for this
     * GuacamoleInstruction. Note that the List returned is immutable.
     * Attempts to modify the list will result in exceptions.
     *
     * @return A List of all argument values specified for this
     *         GuacamoleInstruction.
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Returns this GuacamoleInstruction in the form it would be sent over the
     * Guacamole protocol.
     *
     * @return This GuacamoleInstruction in the form it would be sent over the
     *         Guacamole protocol.
     */
    @Override
    public String toString() {

        StringBuilder buff = new StringBuilder();

        // Write opcode
        buff.append(opcode.length());
        buff.append('.');
        buff.append(opcode);

        // Write argument values
        for (String value : args) {
            buff.append(',');
            buff.append(value.length());
            buff.append('.');
            buff.append(value);
        }

        // Write terminator
        buff.append(';');

        return buff.toString();

    }

}
