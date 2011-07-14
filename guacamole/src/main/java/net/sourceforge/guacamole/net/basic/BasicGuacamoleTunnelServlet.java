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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.InetGuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.servlet.GuacamoleSession;
import net.sourceforge.guacamole.net.GuacamoleTunnel;
import net.sourceforge.guacamole.net.basic.properties.BasicGuacamoleProperties;
import net.sourceforge.guacamole.protocol.ConfiguredGuacamoleSocket;
import net.sourceforge.guacamole.servlet.GuacamoleTunnelServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicGuacamoleTunnelServlet extends GuacamoleTunnelServlet {

    private Logger logger = LoggerFactory.getLogger(BasicGuacamoleTunnelServlet.class);
    
    private AuthenticationProvider authProvider;

    @Override
    public void init() throws ServletException {

        // Get auth provider instance
        try {
            authProvider = GuacamoleProperties.getProperty(BasicGuacamoleProperties.AUTH_PROVIDER);
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }

    }

    @Override
    protected GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException {

        HttpSession httpSession = request.getSession(true);

        // Retrieve username and password from parms
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Get authorized config
        GuacamoleConfiguration config = authProvider.getAuthorizedConfiguration(username, password);
        if (config == null) {
            logger.warn("Failed login from {} for user \"{}\".", request.getRemoteAddr(), username);
            throw new GuacamoleException("Invalid login");
        }

        logger.debug("Successful login from {} for user \"{}\".", request.getRemoteAddr(), username);

        // Configure and connect socket
        String hostname = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_HOSTNAME);
        int port = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_PORT);

        GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                new InetGuacamoleSocket(hostname, port),
                config
        );

        // Associate socket with tunnel
        GuacamoleTunnel tunnel = new GuacamoleTunnel(socket);

        // Attach tunnel to session
        GuacamoleSession session = new GuacamoleSession(httpSession);
        session.attachTunnel(tunnel);

        return tunnel;

    }

}

