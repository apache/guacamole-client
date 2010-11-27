
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

import javax.servlet.ServletContext;
import net.sourceforge.guacamole.GuacamoleException;

public class GuacamoleConfiguration extends Configuration {

    private String guacd_hostname;
    private int guacd_port;

    public GuacamoleConfiguration(ServletContext context) throws GuacamoleException {

        super(context);

        guacd_hostname       = context.getInitParameter("guacd-hostname");
        guacd_port           = readIntParameter("guacd-port", null);

    }

    public int getProxyPort() {
        return guacd_port;
    }

    public String getProxyHostname() {
        return guacd_hostname;
    }
}
