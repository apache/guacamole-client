package net.sourceforge.guacamole.net.basic;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import net.sourceforge.guacamole.GuacamoleException;

/**
 * Simple HttpServlet which outputs XML containing a list of all authorized
 * configurations for the current user.
 * 
 * @author Michael Jumper
 */
public class WebSocketSupportLoader implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {

            // Attempt to find WebSocket servlet
            Class<Servlet> servlet = (Class<Servlet>) GuacamoleClassLoader.getInstance().findClass(
                "net.sourceforge.guacamole.net.basic.BasicGuacamoleTunnelServlet"
                //"net.sourceforge.guacamole.net.basic.BasicGuacamoleWebSocketTunnelServlet"
            ); 

            // Dynamically add servlet IF SERVLET 3.0 API AVAILABLE!
            try {

                // Get servlet registration class
                Class regClass = Class.forName("javax.servlet.ServletRegistration");

                // Get and invoke addServlet()
                Method addServlet = ServletContext.class.getMethod("addServlet", String.class, Class.class);
                Object reg = addServlet.invoke(sce.getServletContext(), "WebSocketTunnel", servlet);

                // Get and invoke addMapping()
                Method addMapping = regClass.getMethod("addMapping", String[].class);
                addMapping.invoke(reg, (Object) new String[]{"/websocket-tunnel"});

                // If we succesfully load and register the WebSocket tunnel servlet,
                // WebSocket is supported.
                System.err.println("WebSocket support found!");

            }
            catch (ClassNotFoundException e) {
                // Servlet API 3.0 unsupported
                System.err.println("Servlet API 3.0 not found.");
            }
            catch (NoSuchMethodException e) {
                // Servlet API 3.0 unsupported
                System.err.println("Servlet API 3.0 not found.");
            }
            catch (IllegalAccessException e) {
            }
            catch (InvocationTargetException e) {
            }

        }
        catch (ClassNotFoundException e) {
            
            // If no such servlet class, WebSocket support not present
            System.err.println("WebSocket support not found.");

        }
        catch (GuacamoleException e) {
            e.printStackTrace();
        }

    }

}

