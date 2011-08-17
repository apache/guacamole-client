
package net.sourceforge.guacamole.net.auth;

import java.util.Map;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

public interface AuthenticationProvider<CredentialType> {

    public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(CredentialType credentials) throws GuacamoleException;

}
