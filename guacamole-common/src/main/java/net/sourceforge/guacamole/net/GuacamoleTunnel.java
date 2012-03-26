
package net.sourceforge.guacamole.net;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.io.GuacamoleWriter;

/**
 * Provides a unique identifier and synchronized access to the GuacamoleReader
 * and GuacamoleWriter associated with a GuacamoleSocket.
 *
 * @author Michael Jumper
 */
public class GuacamoleTunnel {

    private UUID uuid;
    private GuacamoleSocket socket;

    private ReentrantLock readerLock;
    private ReentrantLock writerLock;

    /**
     * Creates a new GuacamoleTunnel which synchronizes access to the
     * Guacamole instruction stream associated with the given GuacamoleSocket.
     *
     * @param socket The GuacamoleSocket to provide synchronized access for.
     */
    public GuacamoleTunnel(GuacamoleSocket socket) {

        this.socket = socket;
        uuid = UUID.randomUUID();

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
    public GuacamoleReader acquireReader() {
        readerLock.lock();
        return socket.getReader();
    }

    /**
     * Relinquishes exclusive read access to the Guacamole instruction
     * stream. This function should be called whenever a thread finishes using
     * a GuacamoleTunnel's GuacamoleReader.
     */
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
    public GuacamoleWriter acquireWriter() {
        writerLock.lock();
        return socket.getWriter();
    }

    /**
     * Relinquishes exclusive write access to the Guacamole instruction
     * stream. This function should be called whenever a thread finishes using
     * a GuacamoleTunnel's GuacamoleWriter.
     */
    public void releaseWriter() {
        writerLock.unlock();
    }

    /**
     * Returns whether there are threads waiting for write access to the
     * Guacamole instruction stream.
     *
     * @return true if threads are waiting for write access the Guacamole
     *         instruction stream, false otherwise.
     */
    public boolean hasQueuedWriterThreads() {
        return writerLock.hasQueuedThreads();
    }

    /**
     * Returns the unique identifier associated with this GuacamoleTunnel.
     *
     * @return The unique identifier associated with this GuacamoleTunnel.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Returns the GuacamoleSocket used by this GuacamoleTunnel for reading
     * and writing.
     * 
     * @return The GuacamoleSocket used by this GuacamoleTunnel.
     */
    public GuacamoleSocket getSocket() {
        return socket;
    }

    /**
     * Release all resources allocated to this GuacamoleTunnel.
     * 
     * @throws GuacamoleException if an error occurs while releasing
     *                            resources.
     */
    public void close() throws GuacamoleException {
        socket.close();
    }

    /**
     * Returns whether this GuacamoleTunnel is open, or has been closed.
     * 
     * @return true if this GuacamoleTunnel is open, false if it is closed.
     */
    public boolean isOpen() {
        return socket.isOpen();
    }
    
}
