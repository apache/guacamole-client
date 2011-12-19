
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

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
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

    /**
     * Wrapped Reader to be used for all input.
     */
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

    private int parseStart;

    private char[] buffer = new char[20480];
    private int usedLength = 0;

    @Override
    public boolean available() throws GuacamoleException {
        try {
            return input.ready() || usedLength != 0;
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }
    }

    @Override
    public char[] read() throws GuacamoleException {

        try {

            // While we're blocking, or input is available
            for (;;) {

                // Length of element
                int elementLength = 0;
                
                // Resume where we left off
                int i = parseStart;

                // Parse instruction in buffer
                while (i < usedLength) {

                    // Read character
                    char readChar = buffer[i++];

                    // If digit, update length
                    if (readChar >= '0' && readChar <= '9')
                        elementLength = elementLength * 10 + readChar - '0';

                    // If not digit, check for end-of-length character
                    else if (readChar == '.') {

                        // Check if element present in buffer
                        if (i + elementLength < usedLength) {

                            // Get terminator
                            char terminator = buffer[i + elementLength];
                            
                            // Move to character after terminator
                            i += elementLength + 1;

                            // Reset length
                            elementLength = 0;

                            // Continue here if necessary
                            parseStart = i;

                            // If terminator is semicolon, we have a full
                            // instruction.
                            if (terminator == ';') {

                                // Copy instruction data
                                char[] instruction = new char[i];
                                System.arraycopy(buffer, 0, instruction, 0, i);

                                // Update buffer
                                usedLength -= i;
                                parseStart = 0;
                                System.arraycopy(buffer, i, buffer, 0, usedLength);

                                return instruction;
                                
                            }

                            // Handle invalid terminator characters
                            else if (terminator != ',')
                                throw new GuacamoleException("Element terminator of instruction was not ';' nor ','");
                            
                        }

                        // Otherwise, read more data
                        else
                            break;
                        
                    }

                    // Otherwise, parse error
                    else
                        throw new GuacamoleException("Non-numeric character in element length.");

                }

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

                // Update used length
                usedLength += numRead;

            } // End read loop

        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }

    @Override
    public GuacamoleInstruction readInstruction() throws GuacamoleException {

        // Get instruction
        char[] instructionBuffer = read();
        
        // If EOF, return EOF
        if (instructionBuffer == null)
            return null;
        
        // Start of element
        int elementStart = 0;
        
        // Build list of elements
        LinkedList<String> elements = new LinkedList<String>();
        while (elementStart < instructionBuffer.length) {

            // Find end of length 
            int lengthEnd = ArrayUtils.indexOf(instructionBuffer, '.', elementStart);

            // Parse length
            int length = Integer.parseInt(new String(
                    instructionBuffer,
                    elementStart,
                    lengthEnd - elementStart
            ));

            // Parse element from just after period
            elementStart = lengthEnd + 1;
            String element = new String(
                    instructionBuffer,
                    elementStart,
                    length
            );

            // Append element to list of elements
            elements.addLast(element);
            
            // Read terminator after element
            elementStart += length;
            char terminator = instructionBuffer[elementStart];

            // Continue reading instructions after terminator
            elementStart++;
           
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

        // Return parsed instruction
        return instruction;

    }

}
