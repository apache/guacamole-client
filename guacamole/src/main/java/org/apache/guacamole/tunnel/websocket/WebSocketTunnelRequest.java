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

package org.apache.guacamole.tunnel.websocket;

import java.util.List;
import java.util.Map;
import javax.websocket.server.HandshakeRequest;
import org.apache.guacamole.tunnel.TunnelRequest;

/**
 * WebSocket-specific implementation of TunnelRequest.
 *
 * @author Michael Jumper
 */
public class WebSocketTunnelRequest extends TunnelRequest {

    /**
     * All parameters passed via HTTP to the WebSocket handshake.
     */
    private final Map<String, List<String>> handshakeParameters;
    
    /**
     * Creates a TunnelRequest implementation which delegates parameter and
     * session retrieval to the given HandshakeRequest.
     *
     * @param request The HandshakeRequest to wrap.
     */
    public WebSocketTunnelRequest(HandshakeRequest request) {
        this.handshakeParameters = request.getParameterMap();
    }

    @Override
    public String getParameter(String name) {

        // Pull list of values, if present
        List<String> values = getParameterValues(name);
        if (values == null || values.isEmpty())
            return null;

        // Return first parameter value arbitrarily
        return values.get(0);

    }

    @Override
    public List<String> getParameterValues(String name) {
        return handshakeParameters.get(name);
    }
    
}
