package org.glyptodon.guacamole.net.basic.crud.connections;

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

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.AuthenticatingHttpServlet;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * Simple HttpServlet which handles connection creation.
 *
 * @author Michael Jumper
 */
public class Create extends AuthenticatingHttpServlet {

    /**
     * Prefix given to a parameter name when that parameter is a protocol-
     * specific parameter meant for the configuration.
     */
    public static final String PARAMETER_PREFIX = "_";

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws GuacamoleException {

        // Get name and protocol
        String name     = request.getParameter("name");
        String protocol = request.getParameter("protocol");
        
        // Get the ID of the parent connection group
        String parentID = request.getParameter("parentID");

        // Find the correct connection directory
        Directory<String, Connection> directory = 
                ConnectionUtility.findConnectionDirectory(context, parentID);
        
        if(directory == null)
            throw new GuacamoleException("Connection directory not found.");

        // Create config
        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(protocol);

        // Load parameters into config
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {

            // If parameter starts with prefix, load corresponding parameter
            // value into config
            String param = params.nextElement();
            if (param.startsWith(PARAMETER_PREFIX))
                config.setParameter(
                    param.substring(PARAMETER_PREFIX.length()),
                    request.getParameter(param));

        }

        // Create connection skeleton
        Connection connection = new DummyConnection();
        connection.setName(name);
        connection.setConfiguration(config);

        // Add connection
        directory.add(connection);

    }

}

