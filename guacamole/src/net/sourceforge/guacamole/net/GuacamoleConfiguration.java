
package net.sourceforge.guacamole.net;

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

import javax.servlet.ServletContext;
import net.sourceforge.guacamole.GuacamoleException;

public class GuacamoleConfiguration extends Configuration {

    private String password;
    private int outputBPP;
    private boolean compressStream;
    private String protocol;
    private boolean swapRedAndBlue;

    public GuacamoleConfiguration(ServletContext context) throws GuacamoleException {

        super(context);

        password       = context.getInitParameter("password");
        outputBPP      = readIntParameter("output-bpp", 8, 8, 24);
        compressStream = readBooleanParameter("compress-stream", false);
        protocol       = readParameter("protocol", "vnc", "vnc");
        swapRedAndBlue = readBooleanParameter("swap-red-blue", false);

    }

    public int getOutputBPP() {
        return outputBPP;
    }

    public String getPassword() {
        return password;
    }

    public boolean getCompressStream() {
        return compressStream;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean getSwapRedAndBlue() {
        return swapRedAndBlue;
    }

}
