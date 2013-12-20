package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

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
