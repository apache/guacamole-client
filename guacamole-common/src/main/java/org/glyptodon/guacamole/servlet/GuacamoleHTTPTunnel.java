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

package org.glyptodon.guacamole.servlet;

import org.glyptodon.guacamole.net.DelegatingGuacamoleTunnel;
import org.glyptodon.guacamole.net.GuacamoleTunnel;

/**
 * Tracks the last time a particular GuacamoleTunnel was accessed. This
 * information is not necessary for tunnels associated with WebSocket
 * connections, as each WebSocket connection has its own read thread which
 * continuously checks the state of the tunnel and which will automatically
 * timeout when the underlying socket times out, but the HTTP tunnel has no
 * such thread. Because the HTTP tunnel requires the stream to be split across
 * multiple requests, tracking of activity on the tunnel must be performed
 * independently of the HTTP requests.
 *
 * @author Michael Jumper
 */
class GuacamoleHTTPTunnel extends DelegatingGuacamoleTunnel {

    /**
     * The last time this tunnel was accessed.
     */
    private long lastAccessedTime;

    /**
     * Creates a new GuacamoleHTTPTunnel which wraps the given tunnel.
     * Absolutely all function calls on this new GuacamoleHTTPTunnel will be
     * delegated to the underlying GuacamoleTunnel.
     *
     * @param wrappedTunnel
     *     The GuacamoleTunnel to wrap within this GuacamoleHTTPTunnel.
     */
    public GuacamoleHTTPTunnel(GuacamoleTunnel wrappedTunnel) {
        super(wrappedTunnel);
    }

    /**
     * Updates this tunnel, marking it as recently accessed.
     */
    public void access() {
        lastAccessedTime = System.currentTimeMillis();
    }

    /**
     * Returns the time this tunnel was last accessed, as the number of
     * milliseconds since midnight January 1, 1970 GMT. Tunnel access must
     * be explicitly marked through calls to the access() function.
     *
     * @return
     *     The time this tunnel was last accessed.
     */
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

}
