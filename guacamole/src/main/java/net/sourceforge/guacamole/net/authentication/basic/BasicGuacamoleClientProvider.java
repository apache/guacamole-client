
package net.sourceforge.guacamole.net.authentication.basic;

import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleTCPClient;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.Configuration;
import net.sourceforge.guacamole.net.GuacamoleProperties;
import net.sourceforge.guacamole.net.authentication.GuacamoleClientProvider;

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

public class BasicGuacamoleClientProvider implements GuacamoleClientProvider {

    public GuacamoleTCPClient createClient(HttpSession session) throws GuacamoleException {

        // Retrieve authorized config data from session
        Configuration config = (Configuration) session.getAttribute("BASIC-LOGIN-AUTH");

        // If no data, not authorized
        if (config == null)
            throw new GuacamoleException("Unauthorized");

        String hostname = GuacamoleProperties.getProperty("guacd-hostname");
        int port = GuacamoleProperties.getIntProperty("guacd-port", null);

        GuacamoleTCPClient client = new GuacamoleTCPClient(hostname, port);
        client.connect(config);

        // Return authorized session
        return client;

    }

}
