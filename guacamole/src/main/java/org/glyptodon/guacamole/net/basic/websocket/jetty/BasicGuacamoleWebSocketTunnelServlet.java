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

import javax.servlet.http.HttpServletRequest;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.eclipse.jetty.websocket.WebSocket;
import org.glyptodon.guacamole.net.basic.BasicTunnelRequestUtility;

/**
 * Authenticating tunnel servlet implementation which uses WebSocket as a
 * tunnel backend, rather than HTTP.
 */
public class BasicGuacamoleWebSocketTunnelServlet extends AuthenticatingWebSocketServlet {

    /**
     * Wrapped GuacamoleHTTPTunnelServlet which will handle all authenticated
     * requests.
     */
    private GuacamoleWebSocketTunnelServlet tunnelServlet =
            new GuacamoleWebSocketTunnelServlet() {

        @Override
        protected GuacamoleTunnel doConnect(HttpServletRequest request)
                throws GuacamoleException {
            return BasicTunnelRequestUtility.createTunnel(request);
        }

    };


    @Override
    protected WebSocket authenticatedConnect(UserContext context,
        HttpServletRequest request, String protocol) {
        return tunnelServlet.doWebSocketConnect(request, protocol);
    }

}

