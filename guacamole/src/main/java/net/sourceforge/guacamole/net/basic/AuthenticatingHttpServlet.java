
package net.sourceforge.guacamole.net.basic;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.AuthenticationProvider;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.net.basic.properties.BasicGuacamoleProperties;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract servlet which provides an authenticatedService() function that
 * is only called if the HTTP request is authenticated, or the current
 * HTTP session has already been authenticated.
 * 
 * Authorized configurations are retrieved using the authentication provider
 * defined in guacamole.properties. The authentication provider has access
 * to the request and session, in addition to any submitted username and
 * password, in order to authenticate the user.
 * 
 * All authorized configurations will be stored in the current HttpSession.
 * 
 * Success and failure are logged.
 * 
 * @author Michael Jumper
 */
public abstract class AuthenticatingHttpServlet extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(AuthenticatingHttpServlet.class);
    
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
    throws IOException, ServletException {

        HttpSession httpSession = request.getSession(true);

        // Try to get configs from session
        Map<String, GuacamoleConfiguration> configs =
                (Map<String, GuacamoleConfiguration>) httpSession.getAttribute("GUAC_CONFIGS");

        // If no configs, try to authenticate the user to get the configs using
        // this request.
        if (configs == null) {

            // Retrieve username and password from parms
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            // Build credentials object
            Credentials credentials = new Credentials ();
            credentials.setSession(httpSession);
            credentials.setRequest(request);
            credentials.setUsername(username);
            credentials.setPassword(password);
            
            // Get authorized configs
            try {
                configs = authProvider.getAuthorizedConfigurations(credentials);
            }
            catch (GuacamoleException e) {
                logger.error("Error retrieving configuration(s) for user {}.", username);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            if (configs == null) {
                logger.warn("Authentication attempt from {} for user \"{}\" failed.",
                        request.getRemoteAddr(), username);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            logger.info("User \"{}\" successfully authenticated from {}.",
                    username, request.getRemoteAddr());

            // Associate configs with session
            httpSession.setAttribute("GUAC_CONFIGS", configs);

        }

        // Allow servlet to run now that authentication has been validated
        authenticatedService(configs, request, response);

    }

    protected abstract void authenticatedService(
            Map<String, GuacamoleConfiguration> configs,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;

}
