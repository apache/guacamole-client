/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;

/**
 * A wrapper to make an APIConnection look like a ConnectionGroup.
 * Useful where a org.glyptodon.guacamole.net.auth.ConnectionGroup is required.
 * 
 * @author James Muehlner
 */
public class APIConnectionGroupWrapper implements ConnectionGroup {

    /**
     * The wrapped APIConnectionGroup.
     */
    private APIConnectionGroup apiConnectionGroup;
    
    /**
     * Create a new APIConnectionGroupWrapper to wrap the given 
     * APIConnectionGroup as a ConnectionGroup.
     * @param apiConnectionGroup the APIConnectionGroup to wrap.
     */
    public APIConnectionGroupWrapper(APIConnectionGroup apiConnectionGroup) {
        this.apiConnectionGroup = apiConnectionGroup;
    }
    
    @Override
    public String getName() {
        return apiConnectionGroup.getName();
    }

    @Override
    public void setName(String name) {
        apiConnectionGroup.setName(name);
    }

    @Override
    public String getIdentifier() {
        return apiConnectionGroup.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        apiConnectionGroup.setIdentifier(identifier);
    }

    @Override
    public String getParentIdentifier() {
        return apiConnectionGroup.getParentIdentifier();
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {
        apiConnectionGroup.setParentIdentifier(parentIdentifier);
    }

    @Override
    public void setType(Type type) {
        apiConnectionGroup.setType(type);
    }

    @Override
    public Type getType() {
        return apiConnectionGroup.getType();
    }

    @Override
    public Directory<String, Connection> getConnectionDirectory() throws GuacamoleException {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Directory<String, ConnectionGroup> getConnectionGroupDirectory() throws GuacamoleException {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        throw new UnsupportedOperationException("Operation not supported.");
    }

}
