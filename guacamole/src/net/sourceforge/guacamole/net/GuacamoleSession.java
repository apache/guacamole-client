
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import net.sourceforge.guacamole.Client;
import net.sourceforge.guacamole.vnc.VNCClient;
import net.sourceforge.guacamole.vnc.VNCConfiguration;
import net.sourceforge.guacamole.vnc.VNCException;

public class GuacamoleSession {

    private GuacamoleConfiguration config;
    private final HttpSession session;
    private Client client;

    private class SessionVNCClient extends VNCClient implements HttpSessionBindingListener {

        public SessionVNCClient(String host, int port, String password, int colorBits, int outputBPP) throws VNCException {
            super(host, port, password, colorBits, outputBPP);
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
        }
    }

    public void connect() throws GuacamoleException {
        synchronized (session) {

            if (client != null)
                client.disconnect();

            // Connect to VNC server
            try {

                // Read VNC-specific parameters
                ServletContext context = session.getServletContext();
                VNCConfiguration vncconfig = new VNCConfiguration(context);

                client = new SessionVNCClient(
                        config.getHostname(),
                        config.getPort(),
                        vncconfig.getPassword(),
                        vncconfig.getBPP(),
                        config.getOutputBPP()
                );
            }
            catch (VNCException e) {
                throw new GuacamoleException(e);
            }

            session.setAttribute("CLIENT", client);

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

}
