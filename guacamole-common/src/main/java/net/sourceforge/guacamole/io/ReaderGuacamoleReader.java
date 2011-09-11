
package net.sourceforge.guacamole.io;

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

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction.Operation;
import org.apache.commons.lang3.ArrayUtils;

/**
 * A GuacamoleReader which wraps a standard Java Reader, using that Reader as
 * the Guacamole instruction stream.
 *
 * @author Michael Jumper
 */
public class ReaderGuacamoleReader implements GuacamoleReader {

    private Reader input;

    /**
     * Creates a new ReaderGuacamoleReader which will use the given Reader as
     * the Guacamole instruction stream.
     *
     * @param input The Reader to use as the Guacamole instruction stream.
     */
    public ReaderGuacamoleReader(Reader input) {
        this.input = input;
    }

    private int usedLength = 0;
    private char[] buffer = new char[20000];

    private int instructionStart;
    private char[] instructionBuffer;

    @Override
    public char[] read() throws GuacamoleException {

        // If data was previously read via readInstruction(), return remaining
        // data instead of reading more.
        if (instructionBuffer != null) {
            
            char[] chunk = new char[instructionBuffer.length - instructionStart];
            System.arraycopy(instructionBuffer, instructionStart, chunk, 0, chunk.length); 
            instructionBuffer = null;
            
            return chunk;
        }
        
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

        // If EOF, return EOF
        if (instructionBuffer == null)
            return null;
        
        // Build list of elements
        LinkedList<String> elements = new LinkedList<String>();
        while (instructionStart < instructionBuffer.length) {

            // Find end of length 
            int lengthEnd = ArrayUtils.indexOf(instructionBuffer, '.', instructionStart);

            // Parse length
            int length = Integer.parseInt(new String(
                    instructionBuffer,
                    instructionStart,
                    lengthEnd - instructionStart
            ));

            // Parse element from just after period
            instructionStart = lengthEnd + 1;
            String element = new String(
                    instructionBuffer,
                    instructionStart,
                    length
            );

            // Append element to list of elements
            elements.addLast(element);
            
            // Read terminator after element
            instructionStart += length;
            char terminator = instructionBuffer[instructionStart];

            // Continue reading instructions after terminator
            instructionStart++;
           
            // If we've reached the end of the instruction
            if (terminator == ';')
                break;

        }

        // Pull opcode off elements list
        String opcode = elements.removeFirst();
        
        // Create instruction
        GuacamoleInstruction instruction = new GuacamoleInstruction(
                Operation.fromOpcode(opcode),
                elements.toArray(new String[elements.size()])
        );

        // Detect end of buffer
        if (instructionStart >= instructionBuffer.length)
            instructionBuffer = null;

        // Return parsed instruction
        return instruction;

    }

}
