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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Logs out the current user by invalidating the associated HttpSession and
 * redirecting the user to the login page.
 *
 * @author Michael Jumper
 */
public class BasicLogout extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException {

        // Invalidate session, if any
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null)
            httpSession.invalidate();

        // Redirect to index
        response.sendRedirect("index.xhtml");

    }

}

