
package net.sourceforge.guacamole.io;

import java.io.IOException;
import java.io.Writer;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleInstruction;

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

public class WriterGuacamoleWriter implements GuacamoleWriter {

    private Writer output;

    public WriterGuacamoleWriter(Writer output) {
        this.output = output;
    }

    @Override
    public void write(char[] chunk, int off, int len) throws GuacamoleException {
        try {
            output.write(chunk, off, len);
            output.flush();
        }
        catch (IOException e) {
            throw new GuacamoleException(e);
        }
    }

    @Override
    public void write(char[] chunk) throws GuacamoleException {
        write(chunk, 0, chunk.length);
    }

    @Override
    public void writeInstruction(GuacamoleInstruction instruction) throws GuacamoleException {
        write(instruction.toString().toCharArray());
    }

}
