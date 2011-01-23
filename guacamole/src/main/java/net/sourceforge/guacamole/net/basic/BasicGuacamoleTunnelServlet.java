package net.sourceforge.guacamole.net.basic;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleTCPClient;
import net.sourceforge.guacamole.net.Configuration;
import net.sourceforge.guacamole.net.GuacamoleProperties;
import net.sourceforge.guacamole.net.GuacamoleSession;
import net.sourceforge.guacamole.net.tunnel.GuacamoleTunnelServlet;

public class BasicGuacamoleTunnelServlet extends GuacamoleTunnelServlet {

    @Override
    protected void doConnect(HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {

        // Session must already exist from login
        HttpSession httpSession = request.getSession(false);

        // Retrieve authorized config data from session
        Configuration config = (Configuration) httpSession.getAttribute("BASIC-LOGIN-AUTH");

        // If no data, not authorized
        if (config == null)
            throw new GuacamoleException("Unauthorized");

        String hostname = GuacamoleProperties.getProperty("guacd-hostname");
        int port = GuacamoleProperties.getIntProperty("guacd-port", null);

        GuacamoleTCPClient client = new GuacamoleTCPClient(hostname, port);
        client.connect(config);

        // Set client for session
        GuacamoleSession session = new GuacamoleSession(httpSession);
        session.attachClient(client);

    }

}

