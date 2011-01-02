
package net.sourceforge.guacamole;

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
    public abstract char[] read() throws GuacamoleException;
    public abstract void disconnect() throws GuacamoleException;

    public void connect(Configuration config) throws GuacamoleException {

        // TODO: Send "select" and "connect" messages in client connect function (based on config) ... to be implemented.
        char[] initMessages = "select:vnc;connect:localhost,5901,potato;".toCharArray();
        write(initMessages, 0, initMessages.length);

    }

}
