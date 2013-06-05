
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
 * Provides abstract and raw character write access to a stream of Guacamole
 * instructions.
 *
 * @author Michael Jumper
 */
public interface GuacamoleWriter {

    /**
     * Writes a portion of the given array of characters to the Guacamole
     * instruction stream. The portion must contain only complete Guacamole
     * instructions.
     *
     * @param chunk An array of characters containing Guacamole instructions.
     * @param off The start offset of the portion of the array to write.
     * @param len The length of the portion of the array to write.
     * @throws GuacamoleException If an error occurred while writing the
     *                            portion of the array specified.
     */
    public void write(char[] chunk, int off, int len) throws GuacamoleException;

    /**
     * Writes the entire given array of characters to the Guacamole instruction
     * stream. The array must consist only of complete Guacamole instructions.
     *
     * @param chunk An array of characters consisting only of complete
     *              Guacamole instructions.
     * @throws GuacamoleException If an error occurred while writing the
     *                            the specified array.
     */
    public void write(char[] chunk) throws GuacamoleException;

    /**
     * Writes the given fully parsed instruction to the Guacamole instruction
     * stream.
     *
     * @param instruction The Guacamole instruction to write.
     * @throws GuacamoleException If an error occurred while writing the
     *                            instruction.
     */
    public void writeInstruction(GuacamoleInstruction instruction) throws GuacamoleException;

}
