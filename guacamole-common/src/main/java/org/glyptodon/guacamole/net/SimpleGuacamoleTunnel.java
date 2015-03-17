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

package org.glyptodon.guacamole.net;


import java.util.UUID;

/**
 * GuacamoleTunnel implementation which uses a provided socket. The UUID of
 * the tunnel will be randomly generated.
 *
 * @author Michael Jumper
 */
public class SimpleGuacamoleTunnel extends AbstractGuacamoleTunnel {

    /**
     * The UUID associated with this tunnel. Every tunnel must have a
     * corresponding UUID such that tunnel read/write requests can be
     * directed to the proper tunnel.
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * The GuacamoleSocket that tunnel should use for communication on
     * behalf of the connecting user.
     */
    private final GuacamoleSocket socket;

    /**
     * Creates a new GuacamoleTunnel which synchronizes access to the
     * Guacamole instruction stream associated with the given GuacamoleSocket.
     *
     * @param socket The GuacamoleSocket to provide synchronized access for.
     */
    public SimpleGuacamoleTunnel(GuacamoleSocket socket) {
        this.socket = socket;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public GuacamoleSocket getSocket() {
        return socket;
    }

}
