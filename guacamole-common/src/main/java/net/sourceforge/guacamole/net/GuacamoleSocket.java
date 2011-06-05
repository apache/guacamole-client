
package net.sourceforge.guacamole.net;

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
import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.io.GuacamoleWriter;

/**
 * Provides abstract socket-like access to a Guacamole connection.
 *
 * @author Michael Jumper
 */
public interface GuacamoleSocket {

    /**
     * Returns a GuacamoleReader which can be used to read from the
     * Guacamole instruction stream associated with the connection
     * represented by this GuacamoleSocket.
     *
     * @return A GuacamoleReader which can be used to read from the
     *         Guacamole instruction stream.
     */
    public GuacamoleReader getReader();

    /**
     * Returns a GuacamoleWriter which can be used to write to the
     * Guacamole instruction stream associated with the connection
     * represented by this GuacamoleSocket.
     *
     * @return A GuacamoleWriter which can be used to write to the
     *         Guacamole instruction stream.
     */
    public GuacamoleWriter getWriter();

    /**
     * Releases all resources in use by the connection represented by this
     * GuacamoleSocket.
     *
     * @throws GuacamoleException If an error occurs while releasing resources.
     */
    public void close() throws GuacamoleException;

}
