
package net.sourceforge.guacamole.net.authentication.basic;

import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;

public class BasicLogin extends HttpServlet {

    private AuthenticationProvider authProvider;

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

            AuthorizedConfiguration info = authProvider.getAuthorizedConfiguration(username, password);
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
