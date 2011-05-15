
package net.sourceforge.guacamole.properties;

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

/**
 * Simple utility class for reading properties from the guacamole.properties
 * file in the root of the classpath.
 *
 * @author Michael Jumper
 */
public class GuacamoleProperties {

    private GuacamoleProperties() {}

    /**
     * The hostname of the server where guacd (the Guacamole proxy server) is
     * running.
     */
    public static final StringGuacamoleProperty GUACD_HOSTNAME = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "guacd-hostname"; }

    };

    /**
     * The port that guacd (the Guacamole proxy server) is listening on.
     */
    public static final IntegerGuacamoleProperty GUACD_PORT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "guacd-port"; }

    };

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

    /**
     * Given a GuacamoleProperty, parses and returns the value set for that
     * property in guacamole.properties, if any.
     *
     * @param <Type> The type that the given property is parsed into.
     * @param property The property to read from guacamole.properties.
     * @return The parsed value of the property as read from
     *         guacamole.properties.
     * @throws GuacamoleException If an error occurs while parsing the value
     *                            for the given property in
     *                            guacamole.properties.
     */
    public static <Type> Type getProperty(GuacamoleProperty<Type> property) throws GuacamoleException {

        if (exception != null)
            throw exception;

        return property.parseValue(properties.getProperty(property.getName()));

    }

}
