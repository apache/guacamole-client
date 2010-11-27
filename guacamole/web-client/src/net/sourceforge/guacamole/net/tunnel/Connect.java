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

import net.sourceforge.guacamole.GuacamoleException;

import java.io.Reader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.net.GuacamoleServlet;

import net.sourceforge.guacamole.net.GuacamoleSession;

public class Connect extends GuacamoleServlet {

    protected boolean shouldCreateSession() {
        return true;
    }

    @Override
    protected void handleRequest(GuacamoleSession session, HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {

        // Disconnect if already connected
        if (session.isConnected())
            session.disconnect();

        // Obtain new connection
        session.connect();

        // Send data
        try {
            char[] connect = "connect:vnc,localhost,5901,potato;".toCharArray();
            session.getClient().write(connect, 0, connect.length);
        }
        catch (GuacamoleException e) {
            throw new GuacamoleException("Error sending data to server: " + e.getMessage(), e);
        }

    }

}

