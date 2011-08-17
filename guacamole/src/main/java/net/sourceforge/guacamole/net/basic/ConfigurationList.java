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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationList extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(ConfigurationList.class);
   
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException {

        HttpSession httpSession = request.getSession(true);

        // Get user configuration
        // Get authorized configs
        Map<String, GuacamoleConfiguration> configs = (Map<String, GuacamoleConfiguration>) 
                httpSession.getAttribute("GUAC_CONFIGS");

        // If no configs in session, not authorized
        if (configs == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Write XML
        response.setHeader("Content-Type", "text/xml");
        PrintWriter out = response.getWriter();
        out.println("<configs>");
        
        for (Entry<String, GuacamoleConfiguration> entry : configs.entrySet()) {

            GuacamoleConfiguration config = entry.getValue();

            // Write config
            out.print("<config id=\"");
            out.print(entry.getKey());
            out.print("\" protocol=\"");
            out.print(config.getProtocol());
            out.println("\"/>");


        }

        out.println("</configs>");
    }

}

