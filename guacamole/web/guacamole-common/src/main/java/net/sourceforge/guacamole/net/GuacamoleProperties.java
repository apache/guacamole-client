
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
import java.io.InputStream;
import java.util.Properties;
import net.sourceforge.guacamole.GuacamoleException;

public class GuacamoleProperties {

    private static final Properties properties;
    private static GuacamoleException exception;

    static {

        properties = new Properties();

        try {

            InputStream stream = GuacamoleProperties.class.getResourceAsStream("/guacamole.properties");
            if (stream == null)
                throw new IOException("Resource /guacamole.properties not found.");

            properties.load(stream);
        }
        catch (IOException e) {
            exception = new GuacamoleException("Error reading guacamole.properties", e);
        }

    }

    public static String getProperty(String name) throws GuacamoleException {
        if (exception != null) throw exception;
        return properties.getProperty(name);
    }

}
