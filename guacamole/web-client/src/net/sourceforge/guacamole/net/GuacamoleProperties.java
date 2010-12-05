
package net.sourceforge.guacamole.net;

import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletException;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.authentication.basic.BasicLogin;

public class GuacamoleProperties {

    private static final Properties properties = new Properties();
    private static GuacamoleException exception;

    static {

        try {
            properties.load(BasicLogin.class.getResourceAsStream("/guacamole.properties"));
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
