
package net.sourceforge.guacamole;

import java.util.LinkedList;
import net.sourceforge.guacamole.GuacamoleInstruction.Operation;
import net.sourceforge.guacamole.net.Configuration;

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

public abstract class GuacamoleClient {

    public abstract void write(char[] chunk, int off, int len) throws GuacamoleException;

    public void write(char[] chunk) throws GuacamoleException {
        write(chunk, 0, chunk.length);
    }

    public void write(GuacamoleInstruction instruction) throws GuacamoleException {
        write(instruction.toString().toCharArray());
    }

    public abstract char[] read() throws GuacamoleException;

    private int instructionStart;
    private char[] buffer;

    public GuacamoleInstruction readInstruction() throws GuacamoleException {

        // Fill buffer if not already filled
        if (buffer == null) {
            buffer = read();
            instructionStart = 0;
        }

        // Locate end-of-opcode and end-of-instruction
        int opcodeEnd = -1;
        int instructionEnd = -1;

        for (int i=instructionStart; i<buffer.length; i++) {

            char c = buffer[i];

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
        String opcode = new String(buffer, instructionStart, opcodeEnd - instructionStart);

        // Parse args
        String[] args;
        if (instructionEnd > opcodeEnd)
            args = new String(buffer, opcodeEnd+1, instructionEnd - opcodeEnd - 1).split(",");
        else
            args = new String[0];

        // Create instruction
        GuacamoleInstruction instruction = new GuacamoleInstruction(
                Operation.fromOpcode(opcode),
                args
        );

        // Advance buffer
        instructionStart = instructionEnd + 1;
        if (instructionStart >= buffer.length)
            buffer = null;

        return instruction;

    }

    public abstract void disconnect() throws GuacamoleException;

    public void connect(Configuration config) throws GuacamoleException {

        // Send protocol
        write(new GuacamoleInstruction(Operation.CLIENT_SELECT, config.getProtocol()));

        // Wait for server args
        GuacamoleInstruction instruction;
        do {
            instruction = readInstruction();
        } while (instruction.getOperation() != Operation.SERVER_ARGS);

        // Build args list off provided names and config
        String[] args = new String[instruction.getArgs().length];
        for (int i=0; i<instruction.getArgs().length; i++) {

            String requiredArg = instruction.getArgs()[i];

            String value = config.getParameter(requiredArg);
            if (value != null)
                args[i] = value;
            else
                args[i] = "";
            
        }

        // Send args
        write(new GuacamoleInstruction(Operation.CLIENT_CONNECT, args));

    }

}
