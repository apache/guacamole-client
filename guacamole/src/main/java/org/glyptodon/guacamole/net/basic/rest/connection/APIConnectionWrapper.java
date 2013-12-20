package org.glyptodon.guacamole.net.basic.rest.connection;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * A wrapper to make an APIConnection look like a Connection. Useful where a
 * org.glyptodon.guacamole.net.auth.Connection is required.
 * 
 * @author James Muehlner
 */
public class APIConnectionWrapper implements Connection {

    private final APIConnection apiConnection;
    
    public APIConnectionWrapper(APIConnection apiConnection) {
        this.apiConnection = apiConnection;
    }

    @Override
    public String getName() {
        return apiConnection.getName();
    }

    @Override
    public void setName(String name) {
        apiConnection.setName(name);
    }

    @Override
    public String getIdentifier() {
        return apiConnection.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        apiConnection.setIdentifier(identifier);
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {
        
        // Create the GuacamoleConfiguration from the parameter map
        GuacamoleConfiguration configuration = new GuacamoleConfiguration();
        
        Map<String, String> parameters = apiConnection.getParameters();
        
        for(Map.Entry<String, String> entry : parameters.entrySet())
            configuration.setParameter(entry.getKey(), entry.getValue());
        
        return configuration;
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) {
        
        // Create a parameter map from the GuacamoleConfiguration
        Map<String, String> newParameters = new HashMap<String, String>();
        for(String key : config.getParameterNames())
            newParameters.put(key, config.getParameter(key));
        
        apiConnection.setParameters(newParameters);
    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException {
        return apiConnection.getHistory();
    }
    
}
