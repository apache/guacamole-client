
package net.sourceforge.guacamole.net;

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
