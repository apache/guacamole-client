/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.basic.rest.connection;

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
    public String getParentIdentifier() {
        return apiConnection.getParentIdentifier();
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {
        apiConnection.setParentIdentifier(parentIdentifier);
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {
        
        // Create the GuacamoleConfiguration from the parameter map
        GuacamoleConfiguration configuration = new GuacamoleConfiguration();
        
        Map<String, String> parameters = apiConnection.getParameters();
        
        for(Map.Entry<String, String> entry : parameters.entrySet())
            configuration.setParameter(entry.getKey(), entry.getValue());
        
        configuration.setProtocol(apiConnection.getProtocol());
        
        return configuration;
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) {
        
        // Create a parameter map from the GuacamoleConfiguration
        Map<String, String> parameters = apiConnection.getParameters();
        for(String key : config.getParameterNames())
            parameters.put(key, config.getParameter(key));
        
        // Set the protocol
        apiConnection.setProtocol(config.getProtocol());
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
