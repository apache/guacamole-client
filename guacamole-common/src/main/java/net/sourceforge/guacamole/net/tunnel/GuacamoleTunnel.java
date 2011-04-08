
package net.sourceforge.guacamole.net.tunnel;

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
import net.sourceforge.guacamole.GuacamoleClient;
import net.sourceforge.guacamole.GuacamoleException;

public class GuacamoleTunnel {

    private UUID uuid;
    private GuacamoleClient client;
    private ReentrantLock instructionStreamLock;

    public GuacamoleTunnel(GuacamoleClient client) throws GuacamoleException {

        this.client = client;
        instructionStreamLock = new ReentrantLock();
        uuid = UUID.randomUUID();

    }

    public GuacamoleClient getClient() throws GuacamoleException {
        return client;
    }

    public ReentrantLock getInstructionStreamLock() {
        return instructionStreamLock;
    }

    public UUID getUUID() {
        return uuid;
    }

}
