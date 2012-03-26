
package net.sourceforge.guacamole.net;

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
import net.sourceforge.guacamole.GuacamoleServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides abstract socket-like access to a Guacamole connection over a given
 * hostname and port.
 *
 * @author Michael Jumper
 */
public class InetGuacamoleSocket implements GuacamoleSocket {

    private Logger logger = LoggerFactory.getLogger(InetGuacamoleSocket.class);
    
    private GuacamoleReader reader;
    private GuacamoleWriter writer;

    private static final int SOCKET_TIMEOUT = 15000;
    private Socket sock;

    /**
     * Creates a new InetGuacamoleSocket which reads and writes instructions
     * to the Guacamole instruction stream of the Guacamole proxy server
     * running at the given hostname and port.
     *
     * @param hostname The hostname of the Guacamole proxy server to connect to.
     * @param port The port of the Guacamole proxy server to connect to.
     * @throws GuacamoleException If an error occurs while connecting to the
     *                            Guacamole proxy server.
     */
    public InetGuacamoleSocket(String hostname, int port) throws GuacamoleException {

        try {

            logger.debug("Connecting to guacd at {}:{}.", hostname, port);
            
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
            throw new GuacamoleServerException(e);
        }

    }

    @Override
    public void close() throws GuacamoleException {
        try {
            logger.debug("Closing socket to guacd.");
            sock.close();
        }
        catch (IOException e) {
            throw new GuacamoleServerException(e);
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

    @Override
    public boolean isOpen() {
        return !sock.isClosed();
    }
    

}
