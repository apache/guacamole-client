
package net.sourceforge.guacamole;

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
import java.net.InetAddress;
import java.net.Socket;

import java.io.InputStream;
import java.io.BufferedInputStream;

import net.sourceforge.guacamole.instruction.Instruction;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.event.KeyEvent;
import net.sourceforge.guacamole.event.PointerEvent;

public class GuacamoleClient extends Client {

    private Socket sock;
    private InputStream input;

    public GuacamoleClient(String hostname, int port) throws GuacamoleException {

        try {
            sock = new Socket(InetAddress.getByName(hostname), port);
            input = new BufferedInputStream(sock.getInputStream());
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }

    public void send(KeyEvent event) throws GuacamoleException {
        // STUB
    }

    public void send(PointerEvent event) throws GuacamoleException {
        // STUB
    }

    public void setClipboard(String clipboard) throws GuacamoleException {
        // STUB
    }

    public void disconnect() throws GuacamoleException {
        try {
            sock.close();
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }
    }

    private StringBuilder instructionBuffer = new StringBuilder();

    public Instruction nextInstruction(boolean blocking) throws GuacamoleException {

        try {

            // While we're blocking, or input is available
            while (blocking || input.available() > 0) {

                int readChar = input.read();
                if (readChar == -1)
                    return null;

                // Add to buffer
                instructionBuffer.append((char) readChar);

                // If end of instruction, return it.
                if (readChar == ';') {

                    // Get instruction, reset buffer
                    final String instruction = instructionBuffer.toString();
                    instructionBuffer.setLength(0);

                    // Return instruction string wrapped in Instruction class
                    return new Instruction() {

                        public String toString() {
                            return instruction;
                        }

                    };
                }

            } // End read loop

        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

        return null;
    }

}
