
package net.sourceforge.guacamole.net.authentication.basic;

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

public class BasicLogin extends HttpServlet {

    private Config config;

    @Override
    public void init() throws ServletException {
        try {
            config = new Config();
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }
    }


    private class Config extends Configuration {

        private AuthenticationProvider authProvider;

        public Config() throws GuacamoleException {

            // Get auth provider instance
            try {
                String authProviderClassName = readParameter("auth-provider");
                Object obj = Class.forName(authProviderClassName).getConstructor().newInstance();
                if (!(obj instanceof AuthenticationProvider))
                    throw new GuacamoleException("Specified session provider class is not a GuacamoleSessionProvider");

                authProvider = (AuthenticationProvider) obj;
            }
            catch (ClassNotFoundException e) {
                throw new GuacamoleException("Session provider class not found", e);
            }
            catch (NoSuchMethodException e) {
                throw new GuacamoleException("Default constructor for session provider not present", e);
            }
            catch (SecurityException e) {
                throw new GuacamoleException("Creation of session provider disallowed; check your security settings", e);
            }
            catch (InstantiationException e) {
                throw new GuacamoleException("Unable to instantiate session provider", e);
            }
            catch (IllegalAccessException e) {
                throw new GuacamoleException("Unable to access default constructor of session provider", e);
            }
            catch (InvocationTargetException e) {
                throw new GuacamoleException("Internal error in constructor of session provider", e.getTargetException());
            }

        }

        public AuthenticationProvider getAuthenticationProvider() {
            return authProvider;
        }

    }

    public static interface AuthenticationProvider {
        public AuthorizedConfiguration getAuthorizedConfiguration(String username, String password) throws GuacamoleException;
    }

    // Added to session when session validated
    public static class AuthorizedConfiguration {

        private String protocol;
        private String hostname;
        private int port;
        private String password;

        public AuthorizedConfiguration(String protocol, String hostname, int port, String password) {
            this.protocol = protocol;
            this.hostname = hostname;
            this.port = port;
            this.password = password;
        }

        public String getHostname() {
            return hostname;
        }

        public String getPassword() {
            return password;
        }

        public int getPort() {
            return port;
        }

        public String getProtocol() {
            return protocol;
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Retrieve username and password from parms
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate username and password
        try {

            AuthorizedConfiguration info = config.getAuthenticationProvider().getAuthorizedConfiguration(username, password);
            if (info != null) {

                // Store authorized configuration
                HttpSession session = req.getSession(true);
                session.setAttribute(
                    "BASIC-LOGIN-AUTH",
                    info
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
