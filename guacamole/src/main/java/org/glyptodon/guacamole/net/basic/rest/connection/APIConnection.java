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

package org.glyptodon.guacamole.net.basic.rest.connection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;

/**
 * A simple connection to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
public class APIConnection {

    /**
     * The name of this connection.
     */
    private String name;
    
    /**
     * The identifier of this connection.
     */
    private String identifier;
    
    /**
     * The identifier of the parent connection group for this connection.
     */
    private String parentIdentifier;
    
    /**
     * The history records associated with this connection.
     */
    private List<? extends ConnectionRecord> history;

    /**
     * Map of all associated parameter values, indexed by parameter name.
     */
    private Map<String, String> parameters = new HashMap<String, String>();
    
    /**
     * Create an empty APIConnection.
     */
    public APIConnection() {}
    
    /**
     * Create an APIConnection from a Connection record.
     * @param connection The connection to create this APIConnection from.
     * @throws GuacamoleException If a problem is encountered while
     *                            instantiating this new APIConnection.
     */
    public APIConnection(Connection connection) 
            throws GuacamoleException {
        this.name = connection.getName();
        this.identifier = connection.getIdentifier();
        this.parentIdentifier = connection.getParentIdentifier();
        this.history = connection.getHistory();
    }

    /**
     * Returns the name of this connection.
     * @return The name of this connection.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this connection.
     * @param name The name of this connection.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the unique identifier for this connection.
     * @return The unique identifier for this connection.
     */
    public String getIdentifier() {
        return identifier;
    }
    /**
     * Sets the unique identifier for this connection.
     * @param identifier The unique identifier for this connection.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    /**
     * Returns the unique identifier for this connection.
     * @return The unique identifier for this connection.
     */
    public String getParentIdentifier() {
        return parentIdentifier;
    }
    /**
     * Sets the parent connection group identifier for this connection.
     * @param parentIdentifier The parent connection group identifier 
     *                         for this connection.
     */
    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    /**
     * Returns the history records associated with this connection.
     * @return The history records associated with this connection.
     */
    public List<? extends ConnectionRecord> getHistory() {
        return history;
    }

    /**
     * Returns the parameter map for this connection.
     * @return The parameter map for this connection.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameter map for this connection.
     * @param parameters The parameter map for this connection.
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
}
