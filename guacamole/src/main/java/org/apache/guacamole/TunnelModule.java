/*
 * Copyright (C) 2015 Glyptodon LLC
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

package org.apache.guacamole;

import com.google.inject.servlet.ServletModule;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module which loads tunnel implementations.
 *
 * @author Michael Jumper
 */
public class TunnelModule extends ServletModule {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(TunnelModule.class);

    /**
     * Classnames of all implementation-specific WebSocket tunnel modules.
     */
    private static final String[] WEBSOCKET_MODULES = {
        "org.apache.guacamole.websocket.WebSocketTunnelModule",
        "org.apache.guacamole.websocket.jetty8.WebSocketTunnelModule",
        "org.apache.guacamole.websocket.jetty9.WebSocketTunnelModule",
        "org.apache.guacamole.websocket.tomcat.WebSocketTunnelModule"
    };

    private boolean loadWebSocketModule(String classname) {

        try {

            // Attempt to find WebSocket module
            Class<?> module = Class.forName(classname);

            // Create loader
            TunnelLoader loader = (TunnelLoader) module.getConstructor().newInstance();

            // Install module, if supported
            if (loader.isSupported()) {
                install(loader);
                return true;
            }

        }

        // If no such class or constructor, etc., then this particular
        // WebSocket support is not present
        catch (ClassNotFoundException e) {}
        catch (NoClassDefFoundError e) {}
        catch (NoSuchMethodException e) {}

        // Log errors which indicate bugs
        catch (InstantiationException e) {
            logger.debug("Error instantiating WebSocket module.", e);
        }
        catch (IllegalAccessException e) {
            logger.debug("Error instantiating WebSocket module.", e);
        }
        catch (InvocationTargetException e) {
            logger.debug("Error instantiating WebSocket module.", e);
        }

        // Load attempt failed
        return false;

    }

    @Override
    protected void configureServlets() {

        bind(TunnelRequestService.class);

        // Set up HTTP tunnel
        serve("/tunnel").with(BasicGuacamoleTunnelServlet.class);

        // Try to load each WebSocket tunnel in sequence
        for (String classname : WEBSOCKET_MODULES) {
            if (loadWebSocketModule(classname)) {
                logger.debug("WebSocket module loaded: {}", classname);
                return;
            }
        }

        // Warn of lack of WebSocket
        logger.info("WebSocket support NOT present. Only HTTP will be used.");

    }

}
