
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

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.event.KeyEvent;
import net.sourceforge.guacamole.event.PointerEvent;

public abstract class Client {

    public abstract void write(char[] chunk, int off, int len) throws GuacamoleException;
    public abstract char[] read() throws GuacamoleException;
    public abstract void disconnect() throws GuacamoleException;

}
