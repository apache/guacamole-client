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

package org.glyptodon.guacamole.net.basic.websocket;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.basic.GuacamoleClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple ServletContextListener which loads a WebSocket tunnel implementation
 * if available, using the Servlet 3.0 API to dynamically load and install
 * the tunnel servlet.
 *
 * Note that because Guacamole depends on the Servlet 2.5 API, and 3.0 may
 * not be available or needed if WebSocket is not desired, the 3.0 API is
 * detected and invoked dynamically via reflection.
 * 
 * @author Michael Jumper
 */
public class WebSocketSupportLoader implements ServletContextListener {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(WebSocketSupportLoader.class);

    /**
     * Classnames of all legacy (non-JSR) WebSocket tunnel implementations.
     */
    private static final String[] WEBSOCKET_CLASSES = {
        "org.glyptodon.guacamole.net.basic.websocket.jetty8.BasicGuacamoleWebSocketTunnelServlet",
        "org.glyptodon.guacamole.net.basic.websocket.jetty9.BasicGuacamoleWebSocketTunnelServlet",
        "org.glyptodon.guacamole.net.basic.websocket.tomcat.BasicGuacamoleWebSocketTunnelServlet"
    };

    private boolean loadWebSocketTunnel(ServletContext context, String classname) {

        try {

            // Attempt to find WebSocket servlet
            Class<Servlet> servlet = (Class<Servlet>)
                    GuacamoleClassLoader.getInstance().findClass(classname);

            // Dynamically add servlet IF SERVLET 3.0 API AVAILABLE!
            try {

                // Get servlet registration class
                Class regClass = Class.forName("javax.servlet.ServletRegistration");

                // Get and invoke addServlet()
                Method addServlet = ServletContext.class.getMethod("addServlet",
                        String.class, Class.class);
                Object reg = addServlet.invoke(context, "WebSocketTunnel", servlet);

                // Get and invoke addMapping()
                Method addMapping = regClass.getMethod("addMapping", String[].class);
                addMapping.invoke(reg, (Object) new String[]{"/websocket-tunnel"});

                // If we succesfully load and register the WebSocket tunnel servlet,
                // WebSocket is supported.
                return true;

            }

            // Servlet API 3.0 unsupported
            catch (ClassNotFoundException e) {
                logger.info("Servlet API 3.0 not found.", e);
            }
            catch (NoSuchMethodException e) {
                logger.warn("Servlet API 3.0 found, but incomplete.", e);
            }

            // Servlet API 3.0 found, but errors during use
            catch (IllegalAccessException e) {
                logger.error("Unable to load WebSocket tunnel servlet.", e);
            }
            catch (InvocationTargetException e) {
                logger.error("Internal error loading WebSocket tunnel servlet.", e);
            }

        }

        // If no such servlet class, this particular WebSocket support
        // is not present
        catch (ClassNotFoundException e) {}
        catch (NoClassDefFoundError e) {}

        // Log all GuacamoleExceptions
        catch (GuacamoleException e) {
            logger.error("Unable to load/detect WebSocket support.", e);
        }

        // Load attempt failed
        return false;

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        // Try to load each WebSocket tunnel in sequence
        for (String classname : WEBSOCKET_CLASSES) {
            if (loadWebSocketTunnel(sce.getServletContext(), classname)) {
                logger.info("Legacy (non-JSR) WebSocket support loaded: {}", classname);
                return;
            }
        }

        // No legacy WebSocket support found (usually good)
        logger.debug("Legacy WebSocket support NOT loaded.");

    }

}

