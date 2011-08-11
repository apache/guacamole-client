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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicLogin extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(BasicLogin.class);
    
    private AuthenticationProvider authProvider;

    @Override
    public void init() throws ServletException {

        // Get auth provider instance
        try {
            authProvider = GuacamoleProperties.getProperty(BasicGuacamoleProperties.AUTH_PROVIDER);
        }
        catch (GuacamoleException e) {
            logger.error("Error getting authentication provider from properties.", e);
            throw new ServletException(e);
        }

    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException {

        HttpSession httpSession = request.getSession(true);

        // Retrieve username and password from parms
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Get authorized config
        GuacamoleConfiguration config;
        try {
            config = authProvider.getAuthorizedConfiguration(username, password);
        }
        catch (GuacamoleException e) {
            logger.error("Error retrieving authorized configuration for user {}.", username);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        if (config == null) {
            logger.warn("Failed login from {} for user \"{}\".", request.getRemoteAddr(), username);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        logger.info("Successful login from {} for user \"{}\".", request.getRemoteAddr(), username);

        httpSession.setAttribute("GUAC_AUTH_CONFIGS", new Integer(0));

    }

}

