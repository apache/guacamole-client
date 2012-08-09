
package net.sourceforge.guacamole.io;

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

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction;

/**
 * Provides abstract and raw character read access to a stream of Guacamole
 * instructions.
 *
 * @author Michael Jumper
 */
public interface GuacamoleReader {

    /**
     * Returns whether instruction data is available for reading. Note that
     * this does not guarantee an entire instruction is available. If a full
     * instruction is not available, this function can return true, and a call
     * to read() will still block.
     *
     * @return true if instruction data is available for reading, false
     *         otherwise.
     * @throws GuacamoleException If an error occurs while checking for
     *                            available data.
     */
    public boolean available() throws GuacamoleException;

    /**
     * Reads at least one complete Guacamole instruction, returning a buffer
     * containing one or more complete Guacamole instructions and no
     * incomplete Guacamole instructions. This function will block until at
     * least one complete instruction is available.
     *
     * @return A buffer containing at least one complete Guacamole instruction,
     *         or null if no more instructions are available for reading.
     * @throws GuacamoleException If an error occurs while reading from the
     *                            stream.
     */
    public char[] read() throws GuacamoleException;

    /**
     * Reads exactly one complete Guacamole instruction and returns the fully
     * parsed instruction.
     *
     * @return The next complete instruction from the stream, fully parsed, or
     *         null if no more instructions are available for reading.
     * @throws GuacamoleException If an error occurs while reading from the
     *                            stream, or if the instruction cannot be
     *                            parsed.
     */
    public GuacamoleInstruction readInstruction() throws GuacamoleException;

}
