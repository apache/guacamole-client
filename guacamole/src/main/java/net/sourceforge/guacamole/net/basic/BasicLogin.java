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

import net.sourceforge.guacamole.net.auth.AuthenticationProvider;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.UsernamePassword;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.net.basic.properties.BasicGuacamoleProperties;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves the authorized configurations associated with a given
 * username/password pair using the authentication provider defined in
 * guacamole.properties.
 * 
 * All authorized configurations will be stored in the current HttpSession.
 * 
 * Success and failure are logged.
 * 
 * @author Michael Jumper
 */
public class BasicLogin extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(BasicLogin.class);
    
    private AuthenticationProvider authProvider;

    @Override
    public void init() throws ServletException {

        // Get auth provider instance
        try {
            authProvider = GuacamoleProperties.getRequiredProperty(BasicGuacamoleProperties.AUTH_PROVIDER);
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

        UsernamePassword credentials = new UsernamePassword();
        credentials.setUsername(username);
        credentials.setPassword(password);
        
        // Get authorized configs
        Map<String, GuacamoleConfiguration> configs;
        try {
            configs = authProvider.getAuthorizedConfigurations(credentials);
        }
        catch (GuacamoleException e) {
            logger.error("Error retrieving configuration(s) for user {}.", username);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        if (configs == null) {
            logger.warn("Failed login from {} for user \"{}\".", request.getRemoteAddr(), username);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        logger.info("Successful login from {} for user \"{}\".", request.getRemoteAddr(), username);

        // Associate configs with session
        httpSession.setAttribute("GUAC_CONFIGS", configs);

    }

}

