
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
import net.sourceforge.guacamole.vnc.event.KeyEvent;

import net.sourceforge.guacamole.net.GuacamoleSession;
import net.sourceforge.guacamole.net.XMLGuacamoleServlet;
import net.sourceforge.guacamole.vnc.VNCException;

/**
 * Servlet which accepts keyboard input events, forwards these events to the
 * VNC client associated with the session, and returns the result (if any)
 * to the HTTP client via XML.
 *
 * This servlet takes three parameters:
 *      index:    The event index. As HTTP requests may arrive out of order,
 *                this index provides the event queue with a means of sorting
 *                events, and determining if events are missing. The first
 *                event has index 0.
 *      pressed:  Whether the key was pressed (1) or released (0).
 *      keysym:   The integer representing the corresponding X11 keysym.
 *
 * @author Michael Jumper
 */

public class Key extends XMLGuacamoleServlet {

    @Override
    protected void handleRequest(GuacamoleSession session, ServletRequest request, Element root) throws GuacamoleException {

        // Event parameters
        int index = Integer.parseInt(request.getParameter("index"));
        boolean pressed = request.getParameter("pressed").equals("1");
        int keysym = Integer.parseInt(request.getParameter("keysym"));

        // Send/queue event
        try {
            session.getClient().send(new KeyEvent(index, keysym, pressed));
        }
        catch (GuacamoleException e) {
            throw new GuacamoleException("Error sending key event to server: " + e.getMessage(), e);
        }

    }
}

