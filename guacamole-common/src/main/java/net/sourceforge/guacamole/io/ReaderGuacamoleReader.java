
package net.sourceforge.guacamole.io;

import java.io.IOException;
import java.io.Reader;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction.Operation;

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

public class ReaderGuacamoleReader implements GuacamoleReader {

    private Reader input;

    public ReaderGuacamoleReader(Reader input) {
        this.input = input;
    }

    private int usedLength = 0;
    private char[] buffer = new char[20000];

    private int instructionStart;
    private char[] instructionBuffer;

    @Override
    public char[] read() throws GuacamoleException {

        try {

            // While we're blocking, or input is available
            for (;;) {

                // If past threshold, resize buffer before reading
                if (usedLength > buffer.length/2) {
                    char[] biggerBuffer = new char[buffer.length*2];
                    System.arraycopy(buffer, 0, biggerBuffer, 0, usedLength);
                    buffer = biggerBuffer;
                }

                // Attempt to fill buffer
                int numRead = input.read(buffer, usedLength, buffer.length - usedLength);
                if (numRead == -1)
                    return null;

                int prevLength = usedLength;
                usedLength += numRead;

                for (int i=usedLength-1; i>=prevLength; i--) {

                    char readChar = buffer[i];

                    // If end of instruction, return it.
                    if (readChar == ';') {

                        // Get instruction
                        char[] chunk = new char[i+1];
                        System.arraycopy(buffer, 0, chunk, 0, i+1);

                        // Reset buffer
                        usedLength -= i+1;
                        System.arraycopy(buffer, i+1, buffer, 0, usedLength);

                        // Return instruction string
                        return chunk;
                    }

                }

            } // End read loop

        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }

    @Override
    public GuacamoleInstruction readInstruction() throws GuacamoleException {

        // Fill instructionBuffer if not already filled
        if (instructionBuffer == null) {
            instructionBuffer = read();
            instructionStart = 0;
        }

        // Locate end-of-opcode and end-of-instruction
        int opcodeEnd = -1;
        int instructionEnd = -1;

        for (int i=instructionStart; i<instructionBuffer.length; i++) {

            char c = instructionBuffer[i];

            if (c == ':')
                opcodeEnd = i;

            else if (c == ';') {
                instructionEnd = i;
                break;
            }

        }

        // If no end-of-instruction marker, malformed.
        if (instructionEnd == -1)
            throw new GuacamoleException("Malformed instruction.");

        // If no end-of-opcode marker, end is end-of-instruction
        if (opcodeEnd == -1)
            opcodeEnd = instructionEnd;

        // Parse opcode
        String opcode = new String(instructionBuffer, instructionStart, opcodeEnd - instructionStart);

        // Parse args
        String[] args;
        if (instructionEnd > opcodeEnd)
            args = new String(instructionBuffer, opcodeEnd+1, instructionEnd - opcodeEnd - 1).split(",");
        else
            args = new String[0];

        // Create instruction
        GuacamoleInstruction instruction = new GuacamoleInstruction(
                Operation.fromOpcode(opcode),
                args
        );

        // Advance instructionBuffer
        instructionStart = instructionEnd + 1;
        if (instructionStart >= instructionBuffer.length)
            instructionBuffer = null;

        return instruction;

    }

}
