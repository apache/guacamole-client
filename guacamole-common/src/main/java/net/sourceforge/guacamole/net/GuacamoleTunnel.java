
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

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.io.GuacamoleWriter;

public class GuacamoleTunnel {

    private UUID uuid;
    private GuacamoleSocket socket;

    private ReentrantLock readerLock;
    private ReentrantLock writerLock;

    public GuacamoleTunnel(GuacamoleSocket socket) throws GuacamoleException {

        this.socket = socket;
        uuid = UUID.randomUUID();

        readerLock = new ReentrantLock();
        writerLock = new ReentrantLock();

    }

    public GuacamoleReader acquireReader() {
        readerLock.lock();
        return socket.getReader();
    }

    public void releaseReader() {
        readerLock.unlock();
    }

    public boolean hasQueuedReaderThreads() {
        return readerLock.hasQueuedThreads();
    }

    public GuacamoleWriter acquireWriter() {
        writerLock.lock();
        return socket.getWriter();
    }

    public void releaseWriter() {
        writerLock.unlock();
    }

    public boolean hasQueuedWriterThreads() {
        return writerLock.hasQueuedThreads();
    }

    public UUID getUUID() {
        return uuid;
    }

}
