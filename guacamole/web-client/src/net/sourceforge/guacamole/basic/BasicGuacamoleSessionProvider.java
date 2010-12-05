
package net.sourceforge.guacamole.basic;

import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSession;
import net.sourceforge.guacamole.net.GuacamoleSessionProvider;

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

public class BasicGuacamoleSessionProvider implements GuacamoleSessionProvider {

    public GuacamoleSession createSession(HttpSession session) throws GuacamoleException {

        // Retrieve authorized config data from session
        BasicLogin.AuthorizedConfiguration config = (BasicLogin.AuthorizedConfiguration)
                session.getAttribute("BASIC-LOGIN-AUTH");

        // If no data, not authorized
        if (config == null)
            throw new GuacamoleException("Unauthorized");

        // Configure session from authorized config info
        GuacamoleSession guacSession = new GuacamoleSession(session);
        guacSession.setConnection(config.getProtocol(), config.getHostname(), config.getPort());
        if (config.getPassword() != null)
            guacSession.setPassword(config.getPassword());

        // Return authorized session
        return guacSession;

    }

}
