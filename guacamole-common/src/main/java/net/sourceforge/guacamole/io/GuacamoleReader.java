
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
 * Provides abstract and raw character read access to a stream of Guacamole
 * instructions.
 *
 * @author Michael Jumper
 */
public interface GuacamoleReader {

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
