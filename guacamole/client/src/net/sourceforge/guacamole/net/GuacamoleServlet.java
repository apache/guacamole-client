
package net.sourceforge.guacamole.net;

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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.GuacamoleException;

public abstract class GuacamoleServlet extends HttpServlet  {

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            handleRequest(req, resp);
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            handleRequest(req, resp);
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }
    }

    private final void handleRequest(HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {
        GuacamoleSession session = new GuacamoleSession(request.getSession(shouldCreateSession()));
        handleRequest(session, request, response);
    }

    protected abstract void handleRequest(GuacamoleSession session, HttpServletRequest request, HttpServletResponse response) throws GuacamoleException;

    protected boolean shouldCreateSession() {
        return false;
    }

}
