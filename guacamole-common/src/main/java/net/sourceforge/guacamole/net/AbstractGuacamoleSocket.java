
package net.sourceforge.guacamole.net;

import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.io.GuacamoleWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction.Operation;
import net.sourceforge.guacamole.protocol.Configuration;

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

public abstract class AbstractGuacamoleSocket implements GuacamoleSocket {

    @Override
    public void connect(Configuration config) throws GuacamoleException {

        // Get reader and writer
        GuacamoleReader reader = getReader();
        GuacamoleWriter writer = getWriter();

        // Send protocol
        writer.writeInstruction(new GuacamoleInstruction(Operation.CLIENT_SELECT, config.getProtocol()));

        // Wait for server args
        GuacamoleInstruction instruction;
        do {
            instruction = reader.readInstruction();
        } while (instruction.getOperation() != Operation.SERVER_ARGS);

        // Build args list off provided names and config
        String[] args = new String[instruction.getArgs().length];
        for (int i=0; i<instruction.getArgs().length; i++) {

            String requiredArg = instruction.getArgs()[i];

            String value = config.getParameter(requiredArg);
            if (value != null)
                args[i] = value;
            else
                args[i] = "";
            
        }

        // Send args
        writer.writeInstruction(new GuacamoleInstruction(Operation.CLIENT_CONNECT, args));

    }

}
