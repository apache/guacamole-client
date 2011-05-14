
package net.sourceforge.guacamole.io;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction;

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
