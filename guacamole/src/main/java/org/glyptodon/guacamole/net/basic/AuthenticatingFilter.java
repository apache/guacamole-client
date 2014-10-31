/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.basic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.event.SessionListenerCollection;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.net.event.AuthenticationFailureEvent;
import org.glyptodon.guacamole.net.event.AuthenticationSuccessEvent;
import org.glyptodon.guacamole.net.event.listener.AuthenticationFailureListener;
import org.glyptodon.guacamole.net.event.listener.AuthenticationSuccessListener;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.glyptodon.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which provides watches requests for credentials, authenticating the
 * user against the configured AuthenticationProvider if credentials are
 * present. Note that if authentication fails, the request is still allowed. To
 * restrict access based on the result of authentication, use
 * RestrictedHttpServlet or RestrictedFilter.
 *
 * The user context is retrieved using the authentication provider defined in
 * guacamole.properties. The authentication provider has access to the request
 * and session, in addition to any submitted username and password, in order
 * to authenticate the user.
 *
 * The user context will be stored in the current HttpSession.
 *
 * Success and failure are logged.
 *
 * @author Michael Jumper
 */
public class AuthenticatingFilter implements Filter {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(AuthenticatingFilter.class);

    /**
     * The session attribute holding the current UserContext.
     */
    public static final String CONTEXT_ATTRIBUTE = "GUAC_CONTEXT";

    /**
     * The session attribute holding the credentials authorizing this session.
     */
    public static final String CREDENTIALS_ATTRIBUTE = "GUAC_CREDS";

    /**
     * The session attribute holding the session-scoped clipboard storage.
     */
    public static final String CLIPBOARD_ATTRIBUTE = "GUAC_CLIP";
    
    /**
     * The AuthenticationProvider to use to authenticate all requests.
     */
    private AuthenticationProvider authProvider;

    /**
     * Whether HTTP authentication should be used (the "Authorization" header).
     */
    private boolean useHttpAuthentication;

    @Override
    public void init(FilterConfig config) throws ServletException {

        // Parse Guacamole configuration
        try {

            // Get auth provider instance
            authProvider = GuacamoleProperties.getRequiredProperty(BasicGuacamoleProperties.AUTH_PROVIDER);

            // Enable HTTP auth, if requested
            useHttpAuthentication = GuacamoleProperties.getProperty(BasicGuacamoleProperties.ENABLE_HTTP_AUTH, false);

        }
        catch (GuacamoleException e) {
            logger.error("Unable to read guacamole.properties: {}", e.getMessage());
            logger.debug("Error reading guacamole.properties.", e);
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
                logger.debug("Error notifying AuthenticationFailureListener: {}", e);
            }
        }

    }

    /**
     * Notifies all listeners in the given collection that authentication was
     * successful.
     *
     * @param listeners A collection of all listeners that should be notified.
     * @param context The UserContext created as a result of authentication
     *                success.
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
    private boolean notifySuccess(Collection listeners, UserContext context,
            Credentials credentials) throws GuacamoleException {

        // Build event for auth success
        AuthenticationSuccessEvent event =
                new AuthenticationSuccessEvent(context, credentials);

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
     * Sends an error on the given HTTP response using the information within
     * the given GuacamoleStatus.
     *
     * @param response The HTTP response to use to send the error.
     * @param guac_status The status to send
     * @param message A human-readable message that can be presented to the
     *                user.
     * @throws ServletException If an error prevents sending of the error
     *                          code.
     */
    public static void sendError(HttpServletResponse response,
            GuacamoleStatus guac_status, String message)
            throws ServletException {

        try {

            // If response not committed, send error code and message
            if (!response.isCommitted()) {
                response.addHeader("Guacamole-Status-Code", Integer.toString(guac_status.getGuacamoleStatusCode()));
                response.addHeader("Guacamole-Error-Message", message);
                response.sendError(guac_status.getHttpStatusCode());
            }

        }
        catch (IOException ioe) {

            // If unable to send error at all due to I/O problems,
            // rethrow as servlet exception
            throw new ServletException(ioe);

        }

    }

    /**
     * Returns the credentials associated with the given session.
     *
     * @param session The session to retrieve credentials from.
     * @return The credentials associated with the given session.
     */
    public static Credentials getCredentials(HttpSession session) {
        return (Credentials) session.getAttribute(CREDENTIALS_ATTRIBUTE);
    }

    /**
     * Returns the UserContext associated with the given session.
     *
     * @param session The session to retrieve UserContext from.
     * @return The UserContext associated with the given session.
     */
    public static UserContext getUserContext(HttpSession session) {
        return (UserContext) session.getAttribute(CONTEXT_ATTRIBUTE);
    }

    /**
     * Returns the ClipboardState associated with the given session. If none
     * exists yet, one is created.
     *
     * @param session The session to retrieve the ClipboardState from.
     * @return The ClipboardState associated with the given session.
     */
    public static ClipboardState getClipboardState(HttpSession session) {

        ClipboardState clipboard = (ClipboardState) session.getAttribute(CLIPBOARD_ATTRIBUTE);
        if (clipboard == null) {
            clipboard = new ClipboardState();
            session.setAttribute(CLIPBOARD_ATTRIBUTE, clipboard);
        }

        return clipboard;

    }

    /**
     * Returns whether the request given has updated credentials. If this
     * function returns false, the UserContext will not be updated.
     * 
     * @param request The request to check for credentials.
     * @return true if the request contains credentials, false otherwise.
     */
    protected boolean hasNewCredentials(HttpServletRequest request) {
        return true;
    }

    /**
     * Regular expression which matches any IPv4 address.
     */
    private static final String IPV4_ADDRESS_REGEX = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})";

    /**
     * Regular expression which matches any IPv6 address.
     */
    private static final String IPV6_ADDRESS_REGEX = "([0-9a-fA-F]*(:[0-9a-fA-F]*){0,7})";

    /**
     * Regular expression which matches any IP address, regardless of version.
     */
    private static final String IP_ADDRESS_REGEX = "(" + IPV4_ADDRESS_REGEX + "|" + IPV6_ADDRESS_REGEX + ")";
    
    /**
     * Pattern which matches valid values of the de-facto standard
     * "X-Forwarded-For" header.
     */
    private static final Pattern X_FORWARDED_FOR = Pattern.compile("^" + IP_ADDRESS_REGEX + "(, " + IP_ADDRESS_REGEX + ")*$");

    /**
     * Returns a formatted string containing an IP address, or list of IP
     * addresses, which represent the HTTP client and any involved proxies. As
     * the headers used to determine proxies can easily be forged, this data is
     * superficially validated to ensure that it at least looks like a list of
     * IPs.
     *
     * @param request The HTTP request to format.
     * @return A formatted string containing one or more IP addresses.
     */
    private String getLoggableAddress(HttpServletRequest request) {

        // Log X-Forwarded-For, if present and valid
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && X_FORWARDED_FOR.matcher(header).matches())
            return "[" + header + ", " + request.getRemoteAddr() + "]";

        // If header absent or invalid, just use source IP
        return request.getRemoteAddr();
        
    }
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
    throws IOException, ServletException {
       
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        
        // Set character encoding to UTF-8 if it's not already set
        if(request.getCharacterEncoding() == null) {
            try {
                request.setCharacterEncoding("UTF-8");
            } catch (UnsupportedEncodingException exception) {
               throw new ServletException(exception);
            }
        }

        try {

            // Obtain context from session
            HttpSession httpSession = request.getSession(true);
            UserContext context = getUserContext(httpSession);

            // If new credentials present, update/create context
            if (hasNewCredentials(request)) {

                // Retrieve username and password from parms
                String username = request.getParameter("username");
                String password = request.getParameter("password");

                // If no username/password given, try Authorization header
                if (useHttpAuthentication && username == null && password == null) {

                    String authorization = request.getHeader("Authorization");
                    if (authorization != null && authorization.startsWith("Basic ")) {

                        // Decode base64 authorization
                        String basicBase64 = authorization.substring(6);
                        String basicCredentials = new String(DatatypeConverter.parseBase64Binary(basicBase64), "UTF-8");

                        // Pull username/password from auth data
                        int colon = basicCredentials.indexOf(':');
                        if (colon != -1) {
                            username = basicCredentials.substring(0, colon);
                            password = basicCredentials.substring(colon+1);
                        }

                        else
                            logger.info("Invalid HTTP Basic \"Authorization\" header received.");

                    }

                } // end Authorization header fallback
                
                // Build credentials object
                Credentials credentials = new Credentials();
                credentials.setSession(httpSession);
                credentials.setRequest(request);
                credentials.setUsername(username);
                credentials.setPassword(password);

                SessionListenerCollection listeners = new SessionListenerCollection(httpSession);

                // If no cached context, attempt to get new context
                if (context == null) {

                    context = authProvider.getUserContext(credentials);

                    // Log successful authentication
                    if (context != null && logger.isInfoEnabled())
                        logger.info("User \"{}\" successfully authenticated from {}.",
                                context.self().getUsername(), getLoggableAddress(request));
                    
                }

                // Otherwise, update existing context
                else
                    context = authProvider.updateUserContext(context, credentials);

                // If auth failed, notify listeners
                if (context == null) {

                    if (logger.isWarnEnabled()) {

                        // Only bother logging failures involving usernames
                        if (credentials.getUsername() != null)
                            logger.info("Authentication attempt from {} for user \"{}\" failed.",
                                    getLoggableAddress(request), credentials.getUsername());
                        else
                            logger.debug("Authentication attempt from {} without username failed.",
                                    getLoggableAddress(request));
                    }

                    notifyFailed(listeners, credentials);
                }

                // If auth succeeded, notify and check with listeners
                else if (!notifySuccess(listeners, context, credentials))
                    logger.info("Successful authentication canceled by hook.");

                // If auth still OK, associate context with session
                else {
                    httpSession.setAttribute(CONTEXT_ATTRIBUTE,     context);
                    httpSession.setAttribute(CREDENTIALS_ATTRIBUTE, credentials);
                }

            } // end if credentials present

            // Allow servlet to run now that authentication has been validated
            chain.doFilter(request, response);

        }

        // Catch any thrown guacamole exception and attempt to pass within the
        // HTTP response, logging each error appropriately.
        catch (GuacamoleClientException e) {
            logger.info("HTTP request rejected: {}", e.getMessage());
            logger.debug("HTTP request rejected by AuthenticatingFilter.", e);
            sendError(response, e.getStatus(), e.getMessage());
        }
        catch (GuacamoleException e) {
            logger.error("Authentication failed internally: {}", e.getMessage());
            logger.debug("Internal server error during authentication.", e);
            sendError(response, e.getStatus(), "Internal server error.");
        }

    }

    @Override
    public void destroy() {
        // No destruction needed
    }

}
