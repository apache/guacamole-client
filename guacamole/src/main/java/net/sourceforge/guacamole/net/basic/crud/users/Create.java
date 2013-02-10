package net.sourceforge.guacamole.net.basic.crud.users;

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
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.basic.AuthenticatingHttpServlet;

/**
 * Simple HttpServlet which handles user creation.
 *
 * @author Michael Jumper
 */
public class Create extends AuthenticatingHttpServlet {

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

        // Create user as specified
        String username = request.getParameter("name");
        
        try {

            // Attempt to get user directory
            Directory<String, User> directory =
                    context.getUserDirectory();

            // Create user skeleton
            User user = new DummyUser();
            user.setUsername(username);
            user.setPassword(UUID.randomUUID().toString());
            
            // Add user
            directory.add(user);
            
        }
        catch (GuacamoleException e) {
            throw new ServletException("Unable to create user.", e);
        }
 
        
    }

}

