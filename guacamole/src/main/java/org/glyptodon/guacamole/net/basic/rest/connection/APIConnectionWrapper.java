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

import java.util.Collections;
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
        
        // Create the GuacamoleConfiguration with current protocol
        GuacamoleConfiguration configuration = new GuacamoleConfiguration();
        configuration.setProtocol(apiConnection.getProtocol());

        // Add parameters, if available
        Map<String, String> parameters = apiConnection.getParameters();
        if (parameters != null)
            configuration.setParameters(parameters);
        
        return configuration;
    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) {
        
        // Set protocol and parameters
        apiConnection.setProtocol(config.getProtocol());
        apiConnection.setParameters(config.getParameters());

    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException {
        return Collections.EMPTY_LIST;
    }
    
}
