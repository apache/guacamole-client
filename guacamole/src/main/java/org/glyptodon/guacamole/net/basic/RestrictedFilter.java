/*
 * Copyright (C) 2014 Glyptodon LLC
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
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which enforces authentication. If no user context is associated with
 * the current HTTP session, or no HTTP session exists, the request is denied.
 *
 * @author Michael Jumper
 */
public class RestrictedFilter implements Filter {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RestrictedFilter.class);

    @Override
    public void init(FilterConfig config) throws ServletException {
        // No configuration
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
    throws IOException, ServletException {
      
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        // Pull user context from session
        UserContext context = null;
        HttpSession session = request.getSession(false);
        if (session != null)
            context = AuthenticatingFilter.getUserContext(session);

        // If authenticated, proceed with rest of chain
        if (context != null)
            chain.doFilter(req, resp);

        // Otherwise, deny entire request
        else {
            final GuacamoleStatus status = GuacamoleStatus.CLIENT_UNAUTHORIZED;
            final String message = "Not authenticated";

            logger.info("HTTP request rejected: {}", message);
            response.addHeader("Guacamole-Status-Code", Integer.toString(status.getGuacamoleStatusCode()));
            response.addHeader("Guacamole-Error-Message", message);
            response.sendError(status.getHttpStatusCode());
        }

    }

    @Override
    public void destroy() {
        // No destruction needed
    }

}
