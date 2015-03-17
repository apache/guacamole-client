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
import java.util.concurrent.locks.ReentrantLock;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.io.GuacamoleReader;
import org.glyptodon.guacamole.io.GuacamoleWriter;

/**
 * GuacamoleTunnel implementation which synchronizes access to the underlying
 * reader and write with reentrant locks.
 *
 * @author Michael Jumper
 */
public class SynchronizedGuacamoleTunnel implements GuacamoleTunnel {

    /**
     * The UUID associated with this tunnel. Every tunnel must have a
     * corresponding UUID such that tunnel read/write requests can be
     * directed to the proper tunnel.
     */
    private final UUID uuid;

    /**
     * The GuacamoleSocket that tunnel should use for communication on
     * behalf of the connecting user.
     */
    private final GuacamoleSocket socket;

    /**
     * Lock acquired when a read operation is in progress.
     */
    private final ReentrantLock readerLock;

    /**
     * Lock acquired when a write operation is in progress.
     */
    private final ReentrantLock writerLock;

    /**
     * Creates a new GuacamoleTunnel which synchronizes access to the
     * Guacamole instruction stream associated with the given GuacamoleSocket.
     *
     * @param socket The GuacamoleSocket to provide synchronized access for.
     */
    public SynchronizedGuacamoleTunnel(GuacamoleSocket socket) {

        this.socket = socket;
        uuid = UUID.randomUUID();

        readerLock = new ReentrantLock();
        writerLock = new ReentrantLock();

    }

    @Override
    public GuacamoleReader acquireReader() {
        readerLock.lock();
        return socket.getReader();
    }

    @Override
    public void releaseReader() {
        readerLock.unlock();
    }

    @Override
    public boolean hasQueuedReaderThreads() {
        return readerLock.hasQueuedThreads();
    }

    @Override
    public GuacamoleWriter acquireWriter() {
        writerLock.lock();
        return socket.getWriter();
    }

    @Override
    public void releaseWriter() {
        writerLock.unlock();
    }

    @Override
    public boolean hasQueuedWriterThreads() {
        return writerLock.hasQueuedThreads();
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public GuacamoleSocket getSocket() {
        return socket;
    }

    @Override
    public void close() throws GuacamoleException {
        socket.close();
    }

    @Override
    public boolean isOpen() {
        return socket.isOpen();
    }

}
