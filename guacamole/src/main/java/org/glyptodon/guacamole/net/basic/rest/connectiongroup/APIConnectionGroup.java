package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.ConnectionGroup.Type;

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

/**
 * A simple connection group to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
public class APIConnectionGroup {

    /**
     * The name of this connection group.
     */
    private String name;
    
    /**
     * The identifier of this connection group.
     */
    private String identifier;
    
    /**
     * The type of this connection group.
     */
    private Type type;
    
    /**
     * Create an empty APIConnectionGroup.
     */
    public APIConnectionGroup() {}
    
    /**
     * Create a new APIConnectionGroup from the given ConnectionGroup record.
     * 
     * @param connectionGroup The ConnectionGroup record to initialize this 
     *                        APIConnectionGroup from.
     */
    public APIConnectionGroup(ConnectionGroup connectionGroup) {
        this.identifier = connectionGroup.getIdentifier();
        this.name = connectionGroup.getName();
        this.type = connectionGroup.getType();
    }

    /**
     * Returns the name of this connection group.
     * @return The name of this connection group.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this connection group.
     * @param name The name of this connection group.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the identifier of this connection group.
     * @return The identifier of this connection group.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier of this connection group.
     * @param identifier The identifier of this connection group.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the type of this connection group.
     * @return The type of this connection group.
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the type of this connection group.
     * @param type The Type of this connection group.
     */
    public void setType(Type type) {
        this.type = type;
    }
}
