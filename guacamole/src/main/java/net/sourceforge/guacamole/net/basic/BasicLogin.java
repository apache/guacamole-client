
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
import java.lang.reflect.InvocationTargetException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.Configuration;
import net.sourceforge.guacamole.net.GuacamoleProperties;

public class BasicLogin extends HttpServlet {

    private AuthenticationProvider authProvider;

    @Override
    public void init() throws ServletException {

        // Get auth provider instance
        try {
            String authProviderClassName = GuacamoleProperties.getProperty("auth-provider");
            Object obj = Class.forName(authProviderClassName).getConstructor().newInstance();
            if (!(obj instanceof AuthenticationProvider))
                throw new ServletException("Specified authentication provider class is not a AuthenticationProvider.");

            authProvider = (AuthenticationProvider) obj;
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }
        catch (ClassNotFoundException e) {
            throw new ServletException("Authentication provider class not found", e);
        }
        catch (NoSuchMethodException e) {
            throw new ServletException("Default constructor for authentication provider not present", e);
        }
        catch (SecurityException e) {
            throw new ServletException("Creation of authentication provider disallowed; check your security settings", e);
        }
        catch (InstantiationException e) {
            throw new ServletException("Unable to instantiate authentication provider", e);
        }
        catch (IllegalAccessException e) {
            throw new ServletException("Unable to access default constructor of authentication provider", e);
        }
        catch (InvocationTargetException e) {
            throw new ServletException("Internal error in constructor of authentication provider", e.getTargetException());
        }

    }

    public static interface AuthenticationProvider {
        public Configuration getAuthorizedConfiguration(String username, String password) throws GuacamoleException;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Retrieve username and password from parms
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate username and password
        try {

            Configuration config = authProvider.getAuthorizedConfiguration(username, password);
            if (config != null) {

                // Store authorized configuration
                HttpSession session = req.getSession(true);
                session.setAttribute(
                    "BASIC-LOGIN-AUTH",
                    config
                );

                // Success
                return;

            }

            // Report "forbidden" on any failure
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Login invalid");
        }
        catch (GuacamoleException e) {
            throw new ServletException("Error validating credentials", e);
        }

    }


}
