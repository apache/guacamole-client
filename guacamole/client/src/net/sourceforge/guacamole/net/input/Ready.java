
package net.sourceforge.guacamole.net.input;

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

import javax.servlet.ServletRequest;
import net.sourceforge.guacamole.GuacamoleException;
import org.w3c.dom.Element;

import net.sourceforge.guacamole.net.GuacamoleSession;
import net.sourceforge.guacamole.net.XMLGuacamoleServlet;

public class Ready extends XMLGuacamoleServlet {

    @Override
    protected void handleRequest(GuacamoleSession session, ServletRequest request, Element root) throws GuacamoleException {

        try {
            session.getClient().ready();
        }
        catch (GuacamoleException e) {
            throw new GuacamoleException("Error updating ready status: " + e.getMessage(), e);
        }

    }
}

