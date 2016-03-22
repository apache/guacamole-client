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

package org.apache.guacamole.rest.connection;

import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * A simple connection to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
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
     * The protocol of this connection.
     */
    private String protocol;
    
    /**
     * Map of all associated parameter values, indexed by parameter name.
     */
    private Map<String, String> parameters;
    
    /**
     * Map of all associated attributes by attribute identifier.
     */
    private Map<String, String> attributes;

    /**
     * The count of currently active connections using this connection.
     */
    private int activeConnections;
    
    /**
     * Create an empty APIConnection.
     */
    public APIConnection() {}
    
    /**
     * Create an APIConnection from a Connection record. Parameters for the
     * connection will not be included.
     *
     * @param connection The connection to create this APIConnection from.
     * @throws GuacamoleException If a problem is encountered while
     *                            instantiating this new APIConnection.
     */
    public APIConnection(Connection connection) 
            throws GuacamoleException {

        // Set connection information
        this.name = connection.getName();
        this.identifier = connection.getIdentifier();
        this.parentIdentifier = connection.getParentIdentifier();
        this.activeConnections = connection.getActiveConnections();
        
        // Set protocol from configuration
        GuacamoleConfiguration configuration = connection.getConfiguration();
        this.protocol = configuration.getProtocol();

        // Associate any attributes
        this.attributes = connection.getAttributes();

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

    /**
     * Returns the protocol for this connection.
     * @return The protocol for this connection.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol for this connection.
     * @param protocol protocol for this connection.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns the number of currently active connections using this
     * connection.
     *
     * @return
     *     The number of currently active usages of this connection.
     */
    public int getActiveConnections() {
        return activeConnections;
    }

    /**
     * Set the number of currently active connections using this connection.
     *
     * @param activeConnections
     *     The number of currently active usages of this connection.
     */
    public void setActiveUsers(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    /**
     * Returns a map of all attributes associated with this connection. Each
     * entry key is the attribute identifier, while each value is the attribute
     * value itself.
     *
     * @return
     *     The attribute map for this connection.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Sets the map of all attributes associated with this connection. Each
     * entry key is the attribute identifier, while each value is the attribute
     * value itself.
     *
     * @param attributes
     *     The attribute map for this connection.
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

}
