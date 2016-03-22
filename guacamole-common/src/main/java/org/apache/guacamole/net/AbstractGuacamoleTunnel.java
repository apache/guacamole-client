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


import java.util.concurrent.locks.ReentrantLock;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;

/**
 * Base GuacamoleTunnel implementation which synchronizes access to the
 * underlying reader and writer with reentrant locks. Implementations need only
 * provide the tunnel's UUID and socket.
 *
 * @author Michael Jumper
 */
public abstract class AbstractGuacamoleTunnel implements GuacamoleTunnel {

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
     * Guacamole instruction stream associated with the underlying
     * GuacamoleSocket.
     */
    public AbstractGuacamoleTunnel() {
        readerLock = new ReentrantLock();
        writerLock = new ReentrantLock();
    }

    /**
     * Acquires exclusive read access to the Guacamole instruction stream
     * and returns a GuacamoleReader for reading from that stream.
     *
     * @return A GuacamoleReader for reading from the Guacamole instruction
     *         stream.
     */
    @Override
    public GuacamoleReader acquireReader() {
        readerLock.lock();
        return getSocket().getReader();
    }

    /**
     * Relinquishes exclusive read access to the Guacamole instruction
     * stream. This function should be called whenever a thread finishes using
     * a GuacamoleTunnel's GuacamoleReader.
     */
    @Override
    public void releaseReader() {
        readerLock.unlock();
    }

    /**
     * Returns whether there are threads waiting for read access to the
     * Guacamole instruction stream.
     *
     * @return true if threads are waiting for read access the Guacamole
     *         instruction stream, false otherwise.
     */
    @Override
    public boolean hasQueuedReaderThreads() {
        return readerLock.hasQueuedThreads();
    }

    /**
     * Acquires exclusive write access to the Guacamole instruction stream
     * and returns a GuacamoleWriter for writing to that stream.
     *
     * @return A GuacamoleWriter for writing to the Guacamole instruction
     *         stream.
     */
    @Override
    public GuacamoleWriter acquireWriter() {
        writerLock.lock();
        return getSocket().getWriter();
    }

    /**
     * Relinquishes exclusive write access to the Guacamole instruction
     * stream. This function should be called whenever a thread finishes using
     * a GuacamoleTunnel's GuacamoleWriter.
     */
    @Override
    public void releaseWriter() {
        writerLock.unlock();
    }

    @Override
    public boolean hasQueuedWriterThreads() {
        return writerLock.hasQueuedThreads();
    }

    @Override
    public void close() throws GuacamoleException {
        getSocket().close();
    }

    @Override
    public boolean isOpen() {
        return getSocket().isOpen();
    }

}
