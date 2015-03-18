/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.jdbc.tunnel;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.InetGuacamoleSocket;

/**
 * Implementation of GuacamoleSocket which connects via TCP to a given hostname
 * and port. If the socket is closed for any reason, a given task is run.
 *
 * @author Michael Jumper
 */
public class ManagedInetGuacamoleSocket extends InetGuacamoleSocket {

    /**
     * The task to run when the socket is closed.
     */
    private final Runnable socketClosedTask;

    /**
     * Creates a new socket which connects via TCP to a given hostname and
     * port. If the socket is closed for any reason, the given task is run.
     * 
     * @param hostname
     *     The hostname of the Guacamole proxy server to connect to.
     *
     * @param port
     *     The port of the Guacamole proxy server to connect to.
     *
     * @param socketClosedTask
     *     The task to run when the socket is closed. This task will NOT be
     *     run if an exception occurs during connection, and this
     *     ManagedInetGuacamoleSocket instance is ultimately not created.
     *
     * @throws GuacamoleException
     *     If an error occurs while connecting to the Guacamole proxy server.
     */
    public ManagedInetGuacamoleSocket(String hostname, int port,
            Runnable socketClosedTask) throws GuacamoleException {
        super(hostname, port);
        this.socketClosedTask = socketClosedTask;
    }

    @Override
    public void close() throws GuacamoleException {
        super.close();
        socketClosedTask.run();
    }
    
}
