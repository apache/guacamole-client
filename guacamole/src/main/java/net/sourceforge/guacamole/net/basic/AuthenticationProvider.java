
package net.sourceforge.guacamole.net.basic;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.Configuration;

public interface AuthenticationProvider {

    public Configuration getAuthorizedConfiguration(String username, String password) throws GuacamoleException;

}
