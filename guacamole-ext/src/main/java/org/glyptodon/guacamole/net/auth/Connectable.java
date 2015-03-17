/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.net.auth;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;

/**
 * An object which Guacamole can connect to.
 *
 * @author Michael Jumper
 */
public interface Connectable {

    /**
     * Establishes a connection to guacd using the information associated with
     * this object. The connection will be provided the given client
     * information.
     *
     * @param info
     *     Information associated with the connecting client.
     *
     * @return
     *     A fully-established GuacamoleTunnel.
     *
     * @throws GuacamoleException
     *     If an error occurs while connecting to guacd, or if permission to
     *     connect is denied.
     */
    public GuacamoleTunnel connect(GuacamoleClientInformation info)
            throws GuacamoleException;

    /**
     * Returns the number of active connections associated with this object.
     * Implementations may simply return 0 if this value is not tracked.
     *
     * @return
     *     The number of active connections associated with this object.
     */
    public int getActiveConnections();
    
}
