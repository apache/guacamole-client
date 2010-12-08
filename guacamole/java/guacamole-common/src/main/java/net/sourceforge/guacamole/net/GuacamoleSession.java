
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

import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import net.sourceforge.guacamole.Client;
import net.sourceforge.guacamole.GuacamoleClient;
import net.sourceforge.guacamole.GuacamoleException;

public class GuacamoleSession {

    private GuacamoleConfiguration config;
    private final HttpSession session;
    private SessionClient client;
    private ReentrantLock instructionStreamLock;

    private String protocol;
    private String hostname;
    private int port;
    private String password;

    public class SessionClient extends Client implements HttpSessionBindingListener {

        private Client client;
        private ReentrantLock authorizedLock;

        public SessionClient(Client client) {
            this.client = client;

            authorizedLock = new ReentrantLock();
            authorizedLock.lock();
        }

        public void authorize() {
            authorizedLock.unlock();
        }

        public void waitForAuthorization() {
            if (authorizedLock.isLocked()) {
                try {
                    authorizedLock.lock();
                    authorizedLock.unlock();
                }
                catch (Throwable t) {
                    throw new Error("Internal error waiting for authorization", t);
                }
            }
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

        public void write(char[] data, int off, int len) throws GuacamoleException {
            client.write(data, off, len);
        }

        public char[] read() throws GuacamoleException {
            return client.read();
        }

        public void disconnect() throws GuacamoleException {
            client.disconnect();
        }

    }

    public GuacamoleSession(HttpSession session) throws GuacamoleException {

        if (session == null)
            throw new GuacamoleException("User has no session.");

        this.session = session;
        synchronized (session) {

            // Read configuration parameters
            config = new GuacamoleConfiguration();

            client = (SessionClient) session.getAttribute("CLIENT");
            instructionStreamLock = (ReentrantLock) session.getAttribute("INSTRUCTION_STREAM_LOCK");
        }
    }

    public void connect() throws GuacamoleException {
        synchronized (session) {

            if (client != null)
                client.disconnect();


            client = new SessionClient(
                    new GuacamoleClient (
                        config.getProxyHostname(),
                        config.getProxyPort()
                    )
            );

            session.setAttribute("CLIENT", client);

            instructionStreamLock = new ReentrantLock();
            session.setAttribute("INSTRUCTION_STREAM_LOCK", instructionStreamLock);

        }
    }

    public boolean isConnected() {
        synchronized (session) {
            return client != null;
        }
    }

    public GuacamoleConfiguration getConfiguration() {
        return config;
    }

    public SessionClient getClient() {
        synchronized (session) {
            return client;
        }
    }

    public void invalidate() {
        session.invalidate();
    }

    public void disconnect() throws GuacamoleException {
        if (client != null) {
            client.disconnect();

            session.removeAttribute("CLIENT");
            client = null;
        }
    }

    public ReentrantLock getInstructionStreamLock() {
        return instructionStreamLock;
    }

    public void setConnection(String protocol, String hostname, int port) {
        this.protocol = protocol;
        this.hostname = hostname;
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConnectMessage() throws GuacamoleException {

        if (getProtocol() == null)
            throw new GuacamoleException("Protocol not specified");

        if (getHostname() == null)
            throw new GuacamoleException("Hostname not specified");

        if (getPassword() == null)
            return "connect:" + getProtocol() + "," + getHostname() + "," + getPort() + ";";
        else
            return "connect:" + getProtocol() + "," + getHostname() + "," + getPort() + "," + getPassword() + ";";
    }

}
