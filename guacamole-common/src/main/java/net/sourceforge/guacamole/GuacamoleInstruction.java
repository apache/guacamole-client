
package net.sourceforge.guacamole;

import java.util.HashMap;

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

public class GuacamoleInstruction {

    public enum Operation {

        CLIENT_SELECT("select"),
        CLIENT_CONNECT("connect"),

        SERVER_ARGS("args");

        private String opcode;
        private Operation(String opcode) { this.opcode = opcode; }

        public String getOpcode() {
            return opcode;
        }

        // Maintain static hash of all opcodes
        private static final HashMap<String, Operation> opcodeToOperation;
        static {

            opcodeToOperation = new HashMap<String, Operation>();

            for (Operation operation : Operation.values())
                opcodeToOperation.put(operation.getOpcode(), operation);

        }

        public Operation fromOpcode(String opcode) {
            return opcodeToOperation.get(opcode);
        }

    }

    private Operation operation;
    private String[] args;

    public GuacamoleInstruction(Operation operation, String... args) {
        this.operation = operation;
        this.args = args;
    }

    public Operation getOperation() {
        return operation;
    }

    public String[] getArgs() {
        return args;
    }

    @Override
    public String toString() {

        StringBuilder buff = new StringBuilder();

        buff.append(operation);

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
