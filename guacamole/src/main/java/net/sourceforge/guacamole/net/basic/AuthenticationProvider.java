
package net.sourceforge.guacamole.net.basic;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

public interface AuthenticationProvider {

    public GuacamoleConfiguration getAuthorizedConfiguration(String username, String password) throws GuacamoleException;

}
