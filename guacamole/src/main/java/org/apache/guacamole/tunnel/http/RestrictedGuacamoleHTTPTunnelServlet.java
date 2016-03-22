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

package org.apache.guacamole.tunnel.http;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.tunnel.TunnelRequestService;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.servlet.GuacamoleHTTPTunnelServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects users to a tunnel associated with the authorized connection
 * having the given ID.
 *
 * @author Michael Jumper
 */
@Singleton
public class RestrictedGuacamoleHTTPTunnelServlet extends GuacamoleHTTPTunnelServlet {

    /**
     * Service for handling tunnel requests.
     */
    @Inject
    private TunnelRequestService tunnelRequestService;
    
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RestrictedGuacamoleHTTPTunnelServlet.class);

    @Override
    protected GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException {

        // Attempt to create HTTP tunnel
        GuacamoleTunnel tunnel = tunnelRequestService.createTunnel(new HTTPTunnelRequest(request));

        // If successful, warn of lack of WebSocket
        logger.info("Using HTTP tunnel (not WebSocket). Performance may be sub-optimal.");

        return tunnel;

    }

}
