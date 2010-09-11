
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
import java.io.Reader;
import java.io.InputStreamReader;

import java.io.OutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;

import net.sourceforge.guacamole.instruction.Instruction;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.event.EventQueue;
import net.sourceforge.guacamole.event.EventHandler;
import net.sourceforge.guacamole.event.KeyEvent;
import net.sourceforge.guacamole.event.PointerEvent;

public class GuacamoleClient extends Client {

    private Socket sock;
    private Reader input;
    private Writer output;

    public GuacamoleClient(String hostname, int port) throws GuacamoleException {

        try {
            sock = new Socket(InetAddress.getByName(hostname), port);
            input = new InputStreamReader(sock.getInputStream());
            output = new OutputStreamWriter(sock.getOutputStream());
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }


    private static final int EVENT_DEADLINE = 500;

    private EventQueue<KeyEvent> keyEvents = new EventQueue<KeyEvent>(new EventHandler<KeyEvent>() {

        public void handle(KeyEvent event) throws IOException {
            int pressed = 0;
            if (event.getPressed()) pressed = 1;

            output.write("key:" + event.getKeySym() + "," + pressed + ";");
            output.flush();
        }

    }, EVENT_DEADLINE);

    private EventQueue<PointerEvent> pointerEvents = new EventQueue<PointerEvent>(new EventHandler<PointerEvent>() {

        public void handle(PointerEvent event) throws IOException {
            int mask = 0;
            if (event.isLeftButtonPressed())   mask |= 1;
            if (event.isMiddleButtonPressed()) mask |= 2;
            if (event.isRightButtonPressed())  mask |= 4;
            if (event.isUpButtonPressed())     mask |= 8;
            if (event.isDownButtonPressed())   mask |= 16;


            output.write("mouse:" + event.getX() + "," + event.getY() + "," + mask + ";");
            output.flush();
        }

    }, EVENT_DEADLINE);


    public void send(KeyEvent event) throws GuacamoleException {

        try {
            keyEvents.add(event);
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }

    public void send(PointerEvent event) throws GuacamoleException {

        try {
            pointerEvents.add(event);
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }

    public void setClipboard(String clipboard) throws GuacamoleException {
        try {
            output.write("clipboard:" + Instruction.escape(clipboard) + ";");
            output.flush();
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }
    }

    public void disconnect() throws GuacamoleException {
        try {
            sock.close();
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }
    }

    private int usedLength = 0;
    private char[] buffer = new char[20000];

    public Instruction nextInstruction(boolean blocking) throws GuacamoleException {

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
                        final String instruction = new String(buffer, 0, i+1);

                        // Reset buffer
                        usedLength -= i+1;
                        System.arraycopy(buffer, i+1, buffer, 0, usedLength);

                        // Return instruction string wrapped in Instruction class
                        return new Instruction() {

                            public String toString() {
                                return instruction;
                            }

                        };
                    }

                }

            } // End read loop

        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }

}
