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

import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.ConnectionGroup.Type;

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
     * The identifier of the parent connection group for this connection group.
     */
    private String parentIdentifier;
    
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
        this.parentIdentifier = connectionGroup.getParentIdentifier();
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
     * Returns the unique identifier for this connection group.
     * @return The unique identifier for this connection group.
     */
    public String getParentIdentifier() {
        return parentIdentifier;
    }
    /**
     * Sets the parent connection group identifier for this connection group.
     * @param parentIdentifier The parent connection group identifier 
     *                         for this connection group.
     */
    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
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
