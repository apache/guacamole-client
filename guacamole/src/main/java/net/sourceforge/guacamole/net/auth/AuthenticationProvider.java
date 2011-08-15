
package net.sourceforge.guacamole.net.auth;

import net.sourceforge.guacamole.GuacamoleException;

public interface AuthenticationProvider {

    public UserConfiguration getUserConfiguration(String username, String password) throws GuacamoleException;

}
