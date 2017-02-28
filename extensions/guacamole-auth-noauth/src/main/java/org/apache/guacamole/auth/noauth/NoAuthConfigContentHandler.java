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

package org.apache.guacamole.auth.noauth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML parser for the configuration file used by the NoAuth auth provider.
 */
public class NoAuthConfigContentHandler extends DefaultHandler {

    /**
     * Map of all configurations, indexed by name.
     */
    private Map<String, GuacamoleConfiguration> configs = new HashMap<String, GuacamoleConfiguration>();

    /**
     * The name of the current configuration, if any.
     */
    private String current = null;

    /**
     * The current configuration being parsed, if any.
     */
    private GuacamoleConfiguration currentConfig = null;

    /**
     * Returns the a map of all available configurations as parsed from the
     * XML file. This map is unmodifiable.
     *
     * @return A map of all available configurations.
     */
    public Map<String, GuacamoleConfiguration> getConfigs() {
        return Collections.unmodifiableMap(configs);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        // If end of config element, add to map
        if (localName.equals("config")) {

            // Add to map
            configs.put(current, currentConfig);

            // Reset state for next configuration
            currentConfig = null;
            current = null;

        }

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        // Begin configuration parsing if config element
        if (localName.equals("config")) {

            // Ensure this config is on the top level
            if (current != null)
                throw new SAXException("Configurations cannot be nested.");

            // Read name
            String name = attributes.getValue("name");
            if (name == null)
                throw new SAXException("Each configuration must have a name.");

            // Read protocol
            String protocol = attributes.getValue("protocol");
            if (protocol == null)
                throw new SAXException("Each configuration must have a protocol.");

            // Create config stub
            current = name;
            currentConfig = new GuacamoleConfiguration();
            currentConfig.setProtocol(protocol);

        }

        // Add parameters to existing configuration
        else if (localName.equals("param")) {

            // Ensure a corresponding config exists
            if (currentConfig == null)
                throw new SAXException("Parameter without corresponding configuration.");

            currentConfig.setParameter(attributes.getValue("name"), attributes.getValue("value"));

        }

    }

}
