
package net.sourceforge.guacamole.protocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
     * @param operation The opcode of the instruction to create.
     * @param args The list of argument values to provide in the new
     *             instruction if any.
     */
    public GuacamoleInstruction(String opcode, String... args) {
        this.opcode = opcode;
        this.args = Collections.unmodifiableList(Arrays.asList(args));
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
