
package net.sourceforge.guacamole.net.auth;

import java.util.Collection;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

public interface UserConfiguration {

    public GuacamoleConfiguration getConfiguration(String id);

    public Collection<String> listConfigurations();
    
}
