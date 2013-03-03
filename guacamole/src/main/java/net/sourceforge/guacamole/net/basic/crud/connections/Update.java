package net.sourceforge.guacamole.net.basic.crud.connections;

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
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.basic.AuthenticatingHttpServlet;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * Simple HttpServlet which handles connection update.
 *
 * @author Michael Jumper
 */
public class Update extends AuthenticatingHttpServlet {

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

        // Get ID and protocol
        String identifier = request.getParameter("id");
        String protocol = request.getParameter("protocol");

        // Attempt to get connection directory
        Directory<String, Connection> directory =
                context.getConnectionDirectory();

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
        Connection connection = directory.get(identifier);
        connection.setConfiguration(config);

        // Update connection
        directory.update(connection);

    }

}

