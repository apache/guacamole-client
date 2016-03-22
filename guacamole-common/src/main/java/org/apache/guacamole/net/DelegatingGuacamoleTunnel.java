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

package org.apache.guacamole.net;

import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;

/**
 * GuacamoleTunnel implementation which simply delegates all function calls to
 * an underlying GuacamoleTunnel.
 *
 * @author Michael Jumper
 */
public class DelegatingGuacamoleTunnel implements GuacamoleTunnel {

    /**
     * The wrapped GuacamoleTunnel.
     */
    private final GuacamoleTunnel tunnel;

    /**
     * Wraps the given tunnel such that all function calls against this tunnel
     * will be delegated to it.
     *
     * @param tunnel
     *     The GuacamoleTunnel to wrap.
     */
    public DelegatingGuacamoleTunnel(GuacamoleTunnel tunnel) {
        this.tunnel = tunnel;
    }

    @Override
    public GuacamoleReader acquireReader() {
        return tunnel.acquireReader();
    }

    @Override
    public void releaseReader() {
        tunnel.releaseReader();
    }

    @Override
    public boolean hasQueuedReaderThreads() {
        return tunnel.hasQueuedReaderThreads();
    }

    @Override
    public GuacamoleWriter acquireWriter() {
        return tunnel.acquireWriter();
    }

    @Override
    public void releaseWriter() {
        tunnel.releaseWriter();
    }

    @Override
    public boolean hasQueuedWriterThreads() {
        return tunnel.hasQueuedWriterThreads();
    }

    @Override
    public UUID getUUID() {
        return tunnel.getUUID();
    }

    @Override
    public GuacamoleSocket getSocket() {
        return tunnel.getSocket();
    }

    @Override
    public void close() throws GuacamoleException {
        tunnel.close();
    }

    @Override
    public boolean isOpen() {
        return tunnel.isOpen();
    }

}
