
package org.glyptodon.guacamole.net.basic.websocket.jetty;

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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.AuthenticatingHttpServlet;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A WebSocket servlet wrapped around an AuthenticatingHttpServlet.
 *
 * @author Michael Jumper
 */
public abstract class AuthenticatingWebSocketServlet extends WebSocketServlet {

    /**
     * Logger for this class.
     */
    private Logger logger = LoggerFactory.getLogger(AuthenticatingWebSocketServlet.class);

    /**
     * Wrapped authenticating servlet.
     */
    private AuthenticatingHttpServlet auth_servlet =
            new AuthenticatingHttpServlet() {

        @Override
        protected void authenticatedService(UserContext context,
            HttpServletRequest request, HttpServletResponse response)
            throws GuacamoleException {

            try {
                // If authenticated, service request
                service_websocket_request(request, response);
            }
            catch (IOException e) {
                throw new GuacamoleServerException(
                        "Cannot service WebSocket request (I/O error).", e);
            }
            catch (ServletException e) {
                throw new GuacamoleServerException(
                        "Cannot service WebSocket request (internal error).", e);
            }

        }

    };

    @Override
    public void init() throws ServletException {
        auth_servlet.init();
    }

    @Override
    protected void service(HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        // Authenticate all inbound requests
        auth_servlet.service(request, response);

    }

    /**
     * Actually services the given request, bypassing the service() override
     * and the authentication scheme.
     *
     * @param request The HttpServletRequest to service.
     * @param response The associated HttpServletResponse.
     * @throws IOException If an I/O error occurs while handling the request.
     * @throws ServletException If an internal error occurs while handling the
     *                          request.
     */
    private void service_websocket_request(HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        // Bypass override and service WebSocket request
        super.service(request, response);

    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request,
        String protocol) {

        // Get session and user context
        HttpSession session = request.getSession(true);
        UserContext context = AuthenticatingHttpServlet.getUserContext(session);

        // Ensure user logged in
        if (context == null) {
            logger.warn("User no longer logged in upon WebSocket connect.");
            return null;
        }

        // Connect WebSocket
        return authenticatedConnect(context, request, protocol);

    }

    /**
     * Function called after the credentials given in the request (if any)
     * are authenticated. If the current session is not associated with
     * valid credentials, this function will not be called.
     *
     * @param context The current UserContext.
     * @param request The HttpServletRequest being serviced.
     * @param protocol The protocol being used over the WebSocket connection.
     */
    protected abstract WebSocket authenticatedConnect(
            UserContext context,
            HttpServletRequest request, String protocol);

}
