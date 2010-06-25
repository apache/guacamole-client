
package net.sourceforge.guacamole.vnc;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
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

import net.sourceforge.guacamole.net.*;
import javax.servlet.ServletContext;

public class VNCConfiguration extends Configuration {

    private String password;
    private int bpp;

    public VNCConfiguration(ServletContext context) throws GuacamoleException {

        super(context);

        password  = context.getInitParameter("password");
        bpp = readIntParameter("bpp", 24, 8, 16, 24);

    }

    public int getBPP() {
        return bpp;
    }

    public String getPassword() {
        return password;
    }

}
