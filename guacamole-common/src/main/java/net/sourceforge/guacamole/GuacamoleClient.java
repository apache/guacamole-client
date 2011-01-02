
package net.sourceforge.guacamole;

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

    public abstract void disconnect() throws GuacamoleException;

    public void connect(Configuration config) throws GuacamoleException {

        // Send protocol
        write(new GuacamoleInstruction(Operation.CLIENT_SELECT, config.getProtocol()));

        // TODO: Wait for and read args message

        // Send args
        write(new GuacamoleInstruction(Operation.CLIENT_CONNECT, "localhost", "5901", "potato"));

    }

}
