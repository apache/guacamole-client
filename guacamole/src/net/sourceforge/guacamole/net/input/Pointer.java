package net.sourceforge.guacamole.net.input;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
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
import net.sourceforge.guacamole.net.GuacamoleException;
import org.w3c.dom.Element;
import net.sourceforge.guacamole.vnc.event.PointerEvent;

import net.sourceforge.guacamole.net.GuacamoleSession;
import net.sourceforge.guacamole.net.XMLGuacamoleServlet;
import net.sourceforge.guacamole.vnc.VNCException;

public class Pointer extends XMLGuacamoleServlet {


    @Override
    protected void handleRequest(GuacamoleSession session, ServletRequest request, Element root) throws GuacamoleException {
        // Event parameters
        String[] events = request.getParameterValues("event");

        for (String event : events) {

            String[] parameters = event.split(",");

            int index = Integer.parseInt(parameters[0]);

            int x = Integer.parseInt(parameters[1]);
            int y = Integer.parseInt(parameters[2]);

            boolean left = parameters[3].equals("1");
            boolean middle = parameters[4].equals("1");
            boolean right = parameters[5].equals("1");
            boolean up = parameters[6].equals("1");
            boolean down = parameters[7].equals("1");

            // Store event
            try {
                session.getClient().send(new PointerEvent(index, left, middle, right, up, down, x, y));
            }
            catch (GuacamoleException e) {
                throw new GuacamoleException("Error sending pointer event to server: " + e.getMessage(), e);
            }
        }
    }

}

