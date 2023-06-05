/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.templates.connection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.DelegatingConnection;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.templates.form.ConnectionChooserField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Connection implementation which wraps another Connection object, but also
 * adds the functionality to link the Connection to another object as a
 * "template" connection, inheriting parameters and attributes from the
 * template.
 */
public class TemplatedConnection extends DelegatingConnection {
    
    /**
     * The Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplatedConnection.class);
    
    /**
     * The attribute name that provides the connection identifier which should
     * e the template for this connection, and the parameters and attributes
     * of which this connection will inherit.
     */
    public static final String TEMPLATE_ID_ATTRIBUTE_NAME = "template-connection-id";
    
    /**
     * An array of all of the attributes implemented specifically by this
     * connection type.
     */
    public static final List<String> TEMPLATED_CONNECTION_ATTRIBUTES = Arrays.asList(TEMPLATE_ID_ATTRIBUTE_NAME);
    
    /**
     * The form that provides all of the fields that need to be visible for this
     * connection implementation in order to provide the template functionality.
     */
    public static final Form TEMPLATED_CONNECTION_FORM = new Form(
                "templated-connection-form",
                Arrays.asList(new ConnectionChooserField(TEMPLATE_ID_ATTRIBUTE_NAME))
            );
    
    /**
     * The directory in which this connection exists.
     */
    private final Directory<Connection> connectionDirectory;
    
    
    /**
     * Create a new TemplatedConnection, wrapping the given Connection object
     * and delegating functionality to that underlying object, after providing
     * the template capability.
     * 
     * @param connection 
     *     The original connection to wrap.
     * 
     * @param directory
     *     The Connection Directory in which this connection exists, provided
     *     for the purpose of retrieving the template connection.
     */
    public TemplatedConnection(Connection connection, Directory<Connection> directory) {
        super(connection);
        this.connectionDirectory = directory;
    }
    
    /**
     * Retrieve the original connection that this object wraps.
     * 
     * @return 
     *     The original connection that this object wraps.
     */
    public Connection getWrappedConnection() {
        return getDelegateConnection();
    }
    
    @Override
    public Map<String, String> getAttributes() {
        
        // Make a mutable copy of the connection attributes
        Map<String, String> attributes = new HashMap<>(super.getAttributes());
        
        // Loop through and add the ones that aren't present so that they are
        // available on the web page.
        for (String attribute : TEMPLATED_CONNECTION_ATTRIBUTES) { 
            String value = attributes.get(attribute);
            if (value == null || value.isEmpty())
                attributes.put(attribute, null);
        }
        
        // Return the new Map of attributes.
        return attributes;
    }
    
    @Override
    public void setAttributes(Map<String, String> attributes) {
    
        // Make a mutable copy of connection attributes
        attributes = new HashMap<>(attributes);
        
        // Loop through extension-specific attributes, only sending ones
        // that are non-null and non-empty to the underlying storage mechanism.
        for (String attribute : TEMPLATED_CONNECTION_ATTRIBUTES) {
            String value = attributes.get(attribute);
            if (value != null && value.isEmpty())
                attributes.put(attribute, null);
        }

        // Set the complete attributes
        super.setAttributes(attributes);
        
    }
    
    @Override
    public GuacamoleConfiguration getConfiguration() {
        
        // Retrieve the decorated connection's configuration.
        GuacamoleConfiguration config = super.getConfiguration();
        
        // Get the template connection identifier - if its empty, just return this config.
        String templateId = getTemplateConnectionId();
        if (templateId == null || templateId.isEmpty())
            return config;
        
        try {
            // Get the template connection from its identifier.
            Connection templateConnection = connectionDirectory.get(templateId);
            
            // Get the template connection configuration.
            GuacamoleConfiguration mergedConfig = new GuacamoleConfiguration(templateConnection.getConfiguration());
            
            // Loop through values, overriding the template values as required.
            for (String parameter: config.getParameterNames()) {
                String value = config.getParameter(parameter);
                if (value != null && !value.isEmpty()) {
                    mergedConfig.setParameter(parameter, value);
                }
            }
            
            // Return the merged configuration
            return mergedConfig;
            
        } catch (GuacamoleException e) {
            LOGGER.warn("Could not retrieve template connection: {}", templateId);
            LOGGER.debug("Exception retrieving template connection.", e);
            return null;
        }
        
    }
    
    /**
     * Return the identifier of the connection that has been set as this
     * connections template.
     * 
     * @return 
     *     The identifier of the template connection as set in the
     *     connection attributes.
     */
    private String getTemplateConnectionId() {
        
        Map<String, String> attributes = getAttributes();
        String templateId = attributes.get(TEMPLATE_ID_ATTRIBUTE_NAME);
        if (templateId != null && !templateId.isEmpty())
            return templateId;
        return null;
        
    }
    
    
    
}
