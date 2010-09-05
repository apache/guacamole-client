
package net.sourceforge.guacamole.net;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
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

import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import net.sourceforge.guacamole.Client;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.instruction.Instruction;
import net.sourceforge.guacamole.event.KeyEvent;
import net.sourceforge.guacamole.event.PointerEvent;
import net.sourceforge.guacamole.vnc.VNCClient;
import net.sourceforge.guacamole.vnc.VNCConfiguration;
import net.sourceforge.guacamole.vnc.VNCException;

public class GuacamoleSession {

    private GuacamoleConfiguration config;
    private final HttpSession session;
    private Client client;
    private ReentrantLock instructionStreamLock;

    private class SessionClient extends Client implements HttpSessionBindingListener {

        private Client client;

        public SessionClient(Client client) {
            this.client = client;
        }

        public void valueBound(HttpSessionBindingEvent event) {
            // Do nothing
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            try {
                disconnect();
            }
            catch (GuacamoleException e) {
                // Ignore
            }
        }

        public void send(KeyEvent event) throws GuacamoleException {
            client.send(event);
        }

        public void send(PointerEvent event) throws GuacamoleException {
            client.send(event);
        }

        public void setClipboard(String clipboard) throws GuacamoleException {
            client.setClipboard(clipboard);
        }

        public void disconnect() throws GuacamoleException {
            client.disconnect();
        }

        public Instruction nextInstruction(boolean blocking) throws GuacamoleException {
            return client.nextInstruction(blocking);
        }

    }

    public GuacamoleSession(HttpSession session) throws GuacamoleException {

        if (session == null)
            throw new GuacamoleException("User has no session.");

        this.session = session;
        synchronized (session) {

            // Read configuration parameters
            ServletContext context = session.getServletContext();
            config = new GuacamoleConfiguration(context);

            client = (Client) session.getAttribute("CLIENT");
            instructionStreamLock = (ReentrantLock) session.getAttribute("INSTRUCTION_STREAM_LOCK");
        }
    }

    public void connect() throws GuacamoleException {
        synchronized (session) {

            if (client != null)
                client.disconnect();


            String protocol = config.getProtocol();
            if (protocol.equals("vnc")) {

                // Connect to VNC server
                try {

                    // Read VNC-specific parameters
                    ServletContext context = session.getServletContext();
                    VNCConfiguration vncconfig = new VNCConfiguration(context);

                    client = new SessionClient(
                            new VNCClient(
                                vncconfig.getHostname(),
                                vncconfig.getPort(),
                                vncconfig.getPassword(),
                                vncconfig.getBPP(),
                                config.getOutputBPP(),
                                config.getSwapRedAndBlue()
                            )
                    );

                }
                catch (VNCException e) {
                    throw new GuacamoleException(e);
                }

            }
            else
                throw new GuacamoleException("Unsupported protocol: " + protocol);

            session.setAttribute("CLIENT", client);

            instructionStreamLock = new ReentrantLock();
            session.setAttribute("INSTRUCTION_STREAM_LOCK", instructionStreamLock);

        }
    }

    public GuacamoleConfiguration getConfiguration() {
        return config;
    }

    public Client getClient() {
        synchronized (session) {
            return client;
        }
    }

    public void invalidate() {
        session.invalidate();
    }

    public void disconnect() throws GuacamoleException {
        if (client != null)
            client.disconnect();
    }

    public ReentrantLock getInstructionStreamLock() {
        return instructionStreamLock;
    }

}
