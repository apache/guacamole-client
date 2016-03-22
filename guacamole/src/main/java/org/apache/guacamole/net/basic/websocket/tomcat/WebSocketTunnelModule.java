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

package org.apache.guacamole.net.basic.websocket.tomcat;

import com.google.inject.servlet.ServletModule;
import org.apache.guacamole.net.basic.TunnelLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the Jetty 9 WebSocket tunnel implementation.
 * 
 * @author Michael Jumper
 */
public class WebSocketTunnelModule extends ServletModule implements TunnelLoader {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(WebSocketTunnelModule.class);

    @Override
    public boolean isSupported() {

        try {

            // Attempt to find WebSocket servlet
            Class.forName("org.apache.guacamole.net.basic.websocket.tomcat.BasicGuacamoleWebSocketTunnelServlet");

            // Support found
            return true;

        }

        // If no such servlet class, this particular WebSocket support
        // is not present
        catch (ClassNotFoundException e) {}
        catch (NoClassDefFoundError e) {}

        // Support not found
        return false;
        
    }
    
    @Override
    public void configureServlets() {

        logger.info("Loading Tomcat 7 WebSocket support...");
        serve("/websocket-tunnel").with(BasicGuacamoleWebSocketTunnelServlet.class);

    }

}
