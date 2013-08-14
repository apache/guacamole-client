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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.basic.AuthenticatingHttpServlet;

/**
 * Simple HttpServlet which handles moving connections.
 *
 * @author Michael Jumper
 */
public class Move extends AuthenticatingHttpServlet {

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws GuacamoleException {

        // Get ID
        String identifier = request.getParameter("id");
        
        // Get the identifier of the new parent connection group
        String parentID   = request.getParameter("parentID");

        // Attempt to get the new parent connection directory
        Directory<String, Connection> newParentDirectory =
                ConnectionUtility.findConnectionDirectory(context, parentID);

        // Attempt to get root connection directory
        Directory<String, Connection> directory =
                context.getRootConnectionGroup().getConnectionDirectory();

        // Move connection
        directory.move(identifier, newParentDirectory);

    }

}

