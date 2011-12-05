
package net.sourceforge.guacamole.protocol;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

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

        buff.append(operation.getOpcode().length());
        buff.append('.');
        buff.append(operation.getOpcode());

        for (int i=0; i<args.length; i++) {
            buff.append(',');
            buff.append(args[i].length());
            buff.append('.');
            buff.append(args[i]);
        }

        buff.append(';');

        return buff.toString();

    }

}
