
package net.sourceforge.guacamole.protocol;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.HashMap;

/**
 * An abstract representation of a Guacamole instruction, as defined by the
 * Guacamole protocol.
 *
 * @author Michael Jumper
 */
public class GuacamoleInstruction {

    /**
     * The operation performed by a particular Guacamole instruction. Each
     * Operation is associated with a unique opcode.
     */
    public enum Operation {

        /**
         * Message sent from client to server specifying which protocol is
         * to be used.
         */
        CLIENT_SELECT("select"),

        /**
         * Message sent from client to server specifying which argument
         * values correspond to the arguments required by the selected
         * protocol.
         */
        CLIENT_CONNECT("connect"),

        /**
         * Message sent from server to client specifying which arguments
         * are required by the selected protocol.
         */
        SERVER_ARGS("args");

        private String opcode;
        private Operation(String opcode) {
            this.opcode = opcode;
        }

        /**
         * Returns the unique opcode associated with this Operation.
         * @return The unique opcode associated with this Operation.
         */
        public String getOpcode() {
            return opcode;
        }

        /**
         * Static hash of all opcodes and their corresponding Operations.
         */
        private static final HashMap<String, Operation> opcodeToOperation;
        static {

            opcodeToOperation = new HashMap<String, Operation>();

            for (Operation operation : Operation.values())
                opcodeToOperation.put(operation.getOpcode(), operation);

        }

        /**
         * Returns the corresponding Operation having the given opcode, if any.
         *
         * @param opcode The unique opcode associated with an Operation.
         * @return The Operation associated with the given opcode, or null if
         *         no such Operation is defined.
         */
        public static Operation fromOpcode(String opcode) {
            return opcodeToOperation.get(opcode);
        }

    }

    private Operation operation;
    private String[] args;

    /**
     * Creates a new GuacamoleInstruction having the given Operation and
     * list of arguments values.
     *
     * @param operation The Operation of the instruction to create.
     * @param args The list of argument values to provide in the new
     *             instruction if any.
     */
    public GuacamoleInstruction(Operation operation, String... args) {
        this.operation = operation;
        this.args = args;
    }

    /**
     * Returns the Operation associated with this GuacamoleInstruction.
     * @return The Operation associated with this GuacamoleInstruction.
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Returns an array of all argument values specified for this
     * GuacamoleInstruction.
     *
     * @return An array of all argument values specified for this
     *         GuacamoleInstruction.
     */
    public String[] getArgs() {
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

        buff.append(operation.getOpcode());

        if (args.length >= 1)
            buff.append(':');

        for (int i=0; i<args.length; i++) {
            if (i > 0)
                buff.append(',');
            buff.append(args[i]);
        }

        buff.append(';');

        return buff.toString();

    }

}
