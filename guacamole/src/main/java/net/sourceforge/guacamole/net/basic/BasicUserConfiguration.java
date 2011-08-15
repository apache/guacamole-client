
package net.sourceforge.guacamole.net.basic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.guacamole.net.auth.UserConfiguration;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

public class BasicUserConfiguration implements UserConfiguration {

    private Map<String, GuacamoleConfiguration> configs =
            new HashMap<String, GuacamoleConfiguration>();

    @Override
    public GuacamoleConfiguration getConfiguration(String id) {
        return configs.get(id);
    }

    @Override
    public Collection<String> listConfigurations() {
        return configs.keySet();
    }
    
    protected void setConfiguration(String id, GuacamoleConfiguration config) {
        configs.put(id, config);
    }
    
}
