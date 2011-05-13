
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

import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.io.ReaderGuacamoleReader;
import net.sourceforge.guacamole.io.WriterGuacamoleWriter;
import net.sourceforge.guacamole.io.GuacamoleWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import java.io.InputStreamReader;

import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.sourceforge.guacamole.GuacamoleException;


public class InetGuacamoleSocket implements GuacamoleSocket {

    private GuacamoleReader reader;
    private GuacamoleWriter writer;

    private static final int SOCKET_TIMEOUT = 15000;
    private Socket sock;

    public InetGuacamoleSocket(String hostname, int port) throws GuacamoleException {

        try {

            // Get address
            SocketAddress address = new InetSocketAddress(
                    InetAddress.getByName(hostname),
                    port
            );

            // Connect with timeout
            sock = new Socket();
            sock.connect(address, SOCKET_TIMEOUT);

            // Set read timeout
            sock.setSoTimeout(SOCKET_TIMEOUT);

            // On successful connect, retrieve I/O streams
            reader = new ReaderGuacamoleReader(new InputStreamReader(sock.getInputStream()));
            writer = new WriterGuacamoleWriter(new OutputStreamWriter(sock.getOutputStream()));

        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }

    }

    @Override
    public void close() throws GuacamoleException {
        try {
            sock.close();
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }
    }

    @Override
    public GuacamoleReader getReader() {
        return reader;
    }

    @Override
    public GuacamoleWriter getWriter() {
        return writer;
    }


}
