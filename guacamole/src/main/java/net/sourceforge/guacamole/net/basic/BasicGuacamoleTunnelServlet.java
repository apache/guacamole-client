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

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.InetGuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.GuacamoleTunnel;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.protocol.ConfiguredGuacamoleSocket;
import net.sourceforge.guacamole.servlet.GuacamoleHTTPTunnelServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects users to a tunnel associated with the authorized configuration
 * having the given ID.
 * 
 * @author Michael Jumper
 */
public class BasicGuacamoleTunnelServlet extends AuthenticatingHttpServlet {

    private Logger logger = LoggerFactory.getLogger(BasicGuacamoleTunnelServlet.class);

    @Override
    protected void authenticatedService(
            Map<String, GuacamoleConfiguration> configs,
            HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
        
        // If authenticated, respond as tunnel
        tunnelServlet.service(request, response);
        
    }

    /**
     * Wrapped GuacamoleHTTPTunnelServlet which will handle all authenticated
     * requests.
     */
    private GuacamoleHTTPTunnelServlet tunnelServlet = new GuacamoleHTTPTunnelServlet() {

        @Override
        protected GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException {

            HttpSession httpSession = request.getSession(true);

            // Get ID of connection
            String id = request.getParameter("id");
            
            // Get credentials
            Credentials credentials = getCredentials(httpSession);
            
            // Get authorized configs
            Map<String, GuacamoleConfiguration> configs = getConfigurations(httpSession);

            // If no configs/credentials in session, not authorized
            if (credentials == null || configs == null)
                throw new GuacamoleException("Cannot connect - user not logged in.");

            // Get authorized config
            GuacamoleConfiguration config = configs.get(id);
            if (config == null) {
                logger.error("Error retrieving authorized configuration id={}.", id);
                throw new GuacamoleException("Unknown configuration ID.");
            }
            
            logger.info("Successful connection from {} to \"{}\".", request.getRemoteAddr(), id);

            // Configure and connect socket
            String hostname = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_HOSTNAME);
            int port = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_PORT);

            GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                    new InetGuacamoleSocket(hostname, port),
                    config
            );

            // Associate socket with tunnel
            GuacamoleTunnel tunnel = new GuacamoleTunnel(socket);

            return tunnel;

        }

    };

}

