package net.sourceforge.guacamole.net.basic.crud.connectiongroups;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.basic.AuthenticatingHttpServlet;

/**
 * Simple HttpServlet which handles connection group creation.
 *
 * @author James Muehlner
 */
public class Create extends AuthenticatingHttpServlet {

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws GuacamoleException {

        // Get name and type
        String name     = request.getParameter("name");
        String type     = request.getParameter("type");
        
        // Get the ID of the parent connection group
        String parentID = request.getParameter("parentID");

        // Find the correct connection group directory
        Directory<String, ConnectionGroup> directory = 
                ConnectionGroupUtility.findConnectionGroupDirectory(context, parentID);
        
        if(directory == null)
            throw new GuacamoleException("Connection group directory not found.");

        // Create connection skeleton
        ConnectionGroup connectionGroup = new DummyConnectionGroup();
        connectionGroup.setName(name);
        
        if("balancing".equals(type))
            connectionGroup.setType(ConnectionGroup.Type.BALANCING);
        else if("organizational".equals(type))
            connectionGroup.setType(ConnectionGroup.Type.ORGANIZATIONAL);

        // Add connection
        directory.add(connectionGroup);

    }

}

