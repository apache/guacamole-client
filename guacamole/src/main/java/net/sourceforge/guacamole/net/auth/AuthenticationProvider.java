
package net.sourceforge.guacamole.net.auth;

import net.sourceforge.guacamole.GuacamoleException;

public interface AuthenticationProvider<CredentialType> {

    public UserConfiguration getUserConfiguration(CredentialType credentials) throws GuacamoleException;

}
