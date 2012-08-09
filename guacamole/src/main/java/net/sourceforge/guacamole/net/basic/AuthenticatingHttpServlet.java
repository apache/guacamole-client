
package net.sourceforge.guacamole.net.basic;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.AuthenticationProvider;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.net.basic.event.SessionListenerCollection;
import net.sourceforge.guacamole.net.basic.properties.BasicGuacamoleProperties;
import net.sourceforge.guacamole.net.event.AuthenticationFailureEvent;
import net.sourceforge.guacamole.net.event.AuthenticationSuccessEvent;
import net.sourceforge.guacamole.net.event.listener.AuthenticationFailureListener;
import net.sourceforge.guacamole.net.event.listener.AuthenticationSuccessListener;
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

    /**
     * The session attribute holding the map of configurations.
     */
    private static final String CONFIGURATIONS_ATTRIBUTE = "GUAC_CONFIGS";

    /**
     * The session attribute holding the credentials authorizing this session.
     */
    private static final String CREDENTIALS_ATTRIBUTE = "GUAC_CREDS";

    /**
     * The AuthenticationProvider to use to authenticate all requests.
     */
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

    /**
     * Notifies all listeners in the given collection that authentication has
     * failed.
     *
     * @param listeners A collection of all listeners that should be notified.
     * @param credentials The credentials associated with the authentication
     *                    request that failed.
     */
    private void notifyFailed(Collection listeners, Credentials credentials) {

        // Build event for auth failure
        AuthenticationFailureEvent event = new AuthenticationFailureEvent(credentials);

        // Notify all listeners
        for (Object listener : listeners) {
            try {
                if (listener instanceof AuthenticationFailureListener)
                    ((AuthenticationFailureListener) listener).authenticationFailed(event);
            }
            catch (GuacamoleException e) {
                logger.error("Error notifying AuthenticationFailureListener.", e);
            }
        }

    }

    /**
     * Notifies all listeners in the given collection that authentication was
     * successful.
     *
     * @param listeners A collection of all listeners that should be notified.
     * @param credentials The credentials associated with the authentication
     *                    request that succeeded.
     * @return true if all listeners are allowing the authentication success,
     *         or if there are no listeners, and false if any listener is
     *         canceling the authentication success. Note that once one
     *         listener cancels, no other listeners will run.
     * @throws GuacamoleException If any listener throws an error while being
     *                            notified. Note that if any listener throws an
     *                            error, the success is canceled, and no other
     *                            listeners will run.
     */
    private boolean notifySuccess(Collection listeners, Credentials credentials)
            throws GuacamoleException {

        // Build event for auth success
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(credentials);

        // Notify all listeners
        for (Object listener : listeners) {
            if (listener instanceof AuthenticationSuccessListener) {

                // Cancel immediately if hook returns false
                if (!((AuthenticationSuccessListener) listener).authenticationSucceeded(event))
                    return false;

            }
        }

        return true;

    }

    /**
     * Sends a predefined, generic error message to the user, along with a
     * "403 - Forbidden" HTTP status code in the response.
     *
     * @param response The response to send the error within.
     * @throws IOException If an error occurs while sending the error.
     */
    private void failAuthentication(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns the credentials associated with the given session.
     *
     * @param session The session to retrieve credentials from.
     * @return The credentials associated with the given session.
     */
    protected Credentials getCredentials(HttpSession session) {
        return (Credentials) session.getAttribute(CREDENTIALS_ATTRIBUTE);
    }

    /**
     * Returns the configurations associated with the given session.
     *
     * @param session The session to retrieve configurations from.
     * @return The configurations associated with the given session.
     */
    protected Map<String, GuacamoleConfiguration> getConfigurations(HttpSession session) {
        return (Map<String, GuacamoleConfiguration>) session.getAttribute(CONFIGURATIONS_ATTRIBUTE);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

        HttpSession httpSession = request.getSession(true);

        // Try to get configs from session
        Map<String, GuacamoleConfiguration> configs = getConfigurations(httpSession);

        // If no configs, try to authenticate the user to get the configs using
        // this request.
        if (configs == null) {

            SessionListenerCollection listeners;
            try {
                listeners = new SessionListenerCollection(httpSession);
            }
            catch (GuacamoleException e) {
                logger.error("Failed to retrieve listeners. Authentication canceled.", e);
                failAuthentication(response);
                return;
            }

            // Retrieve username and password from parms
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            // Build credentials object
            Credentials credentials = new Credentials();
            credentials.setSession(httpSession);
            credentials.setRequest(request);
            credentials.setUsername(username);
            credentials.setPassword(password);

            // Get authorized configs
            try {
                configs = authProvider.getAuthorizedConfigurations(credentials);
            }


            /******** HANDLE FAILED AUTHENTICATION ********/

            // If error retrieving configs, fail authentication, notify listeners
            catch (GuacamoleException e) {
                logger.error("Error retrieving configuration(s) for user \"{}\".",
                        credentials.getUsername(), e);

                notifyFailed(listeners, credentials);
                failAuthentication(response);
                return;
            }

            // If no configs, fail authentication, notify listeners
            if (configs == null) {
                logger.warn("Authentication attempt from {} for user \"{}\" failed.",
                        request.getRemoteAddr(), credentials.getUsername());

                notifyFailed(listeners, credentials);
                failAuthentication(response);
                return;
            }


            /******** HANDLE SUCCESSFUL AUTHENTICATION ********/

            try {

                // Otherwise, authentication has been succesful
                logger.info("User \"{}\" successfully authenticated from {}.",
                        credentials.getUsername(), request.getRemoteAddr());

                // Notify of success, cancel if requested
                if (!notifySuccess(listeners, credentials)) {
                    logger.info("Successful authentication canceled by hook.");
                    failAuthentication(response);
                    return;
                }

            }
            catch (GuacamoleException e) {

                // Cancel authentication success if hook throws exception
                logger.error("Successful authentication canceled by error in hook.", e);
                failAuthentication(response);
                return;

            }

            // Associate configs and credentials with session
            httpSession.setAttribute(CONFIGURATIONS_ATTRIBUTE, configs);
            httpSession.setAttribute(CREDENTIALS_ATTRIBUTE,    credentials);


        }

        // Allow servlet to run now that authentication has been validated
        authenticatedService(configs, request, response);

    }

    protected abstract void authenticatedService(
            Map<String, GuacamoleConfiguration> configs,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;

}
