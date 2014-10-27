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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleUnauthorizedException;
import org.glyptodon.guacamole.GuacamoleUnsupportedException;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract servlet which provides an restrictedService() function that is only
 * called if the current HTTP session has already been authenticated by the
 * AuthenticatingFilter.
 *
 * @author Michael Jumper
 */
public abstract class RestrictedHttpServlet extends HttpServlet {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RestrictedHttpServlet.class);

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

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
        
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
            UserContext context = AuthenticatingFilter.getUserContext(httpSession);

            // If no context, no authorizaton present
            if (context == null)
                throw new GuacamoleUnauthorizedException("Not authenticated");

            // Allow servlet to run now that authentication has been validated
            restrictedService(context, request, response);

        }

        // Catch any thrown guacamole exception and attempt to pass within the
        // HTTP response, logging each error appropriately.
        catch (GuacamoleClientException e) {
            logger.warn("Client request rejected: {}", e.getMessage());
            sendError(response, e.getStatus(), e.getMessage());
        }
        catch (GuacamoleUnsupportedException e) {
            logger.debug("Unsupported operation.", e);
            sendError(response, e.getStatus(), e.getMessage());
        }
        catch (GuacamoleException e) {
            logger.error("Internal server error.", e);
            sendError(response, e.getStatus(), "Internal server error.");
        }

    }

    /**
     * Function called after the request and associated session are validated.
     * If the current session is not associated with valid credentials, this
     * function will not be called.
     *
     * @param context The current UserContext.
     * @param request The HttpServletRequest being serviced.
     * @param response An HttpServletResponse which controls the HTTP response
     *                 of this servlet.
     *
     * @throws GuacamoleException If an error occurs that interferes with the
     *                            normal operation of this servlet.
     */
    protected abstract void restrictedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
            throws GuacamoleException;

}
