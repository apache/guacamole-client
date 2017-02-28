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

package org.apache.guacamole.auth.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.apache.guacamole.xml.DocumentHandler;
import org.apache.guacamole.properties.FileGuacamoleProperty;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Authenticates users against a static list of username/password pairs.
 * Each username/password may be associated with multiple configurations.
 * This list is stored in an XML file which is reread if modified.
 */
public class FileAuthenticationProvider extends SimpleAuthenticationProvider {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(FileAuthenticationProvider.class);

    /**
     * The time the user mapping file was last modified. If the file has never
     * been read, and thus no modification time exists, this will be
     * Long.MIN_VALUE.
     */
    private long lastModified = Long.MIN_VALUE;

    /**
     * The parsed UserMapping read when the user mapping file was last parsed.
     */
    private UserMapping cachedUserMapping;

    /**
     * Guacamole server environment.
     */
    private final Environment environment;

    /**
     * The XML file to read the user mapping from. This property has been
     * deprecated, as the name "basic" is ridiculous, and providing for
     * configurable user-mapping.xml locations is unnecessary complexity. Use
     * GUACAMOLE_HOME/user-mapping.xml instead.
     */
    @Deprecated
    public static final FileGuacamoleProperty BASIC_USER_MAPPING = new FileGuacamoleProperty() {

        @Override
        public String getName() { return "basic-user-mapping"; }

    };

    /**
     * The filename to use for the user mapping.
     */
    public static final String USER_MAPPING_FILENAME = "user-mapping.xml";
    
    /**
     * Creates a new FileAuthenticationProvider that authenticates users against
     * simple, monolithic XML file.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public FileAuthenticationProvider() throws GuacamoleException {
        environment = new LocalEnvironment();
    }

    @Override
    public String getIdentifier() {
        return "default";
    }

    /**
     * Returns a UserMapping containing all authorization data given within
     * the XML file specified by the "basic-user-mapping" property in
     * guacamole.properties. If the XML file has been modified or has not yet
     * been read, this function may reread the file.
     *
     * @return
     *     A UserMapping containing all authorization data within the user
     *     mapping XML file, or null if the file cannot be found/parsed.
     */
    @SuppressWarnings("deprecation") // We must continue to use the "basic-user-mapping" property until it is truly no longer supported
    private UserMapping getUserMapping() {

        // Get user mapping file, defaulting to GUACAMOLE_HOME/user-mapping.xml
        File userMappingFile;
        try {

            // Continue supporting deprecated property, but warn in the logs
            userMappingFile = environment.getProperty(BASIC_USER_MAPPING);
            if (userMappingFile != null)
                logger.warn("The \"basic-user-mapping\" property is deprecated. Please use the \"GUACAMOLE_HOME/user-mapping.xml\" file instead.");

            // Read user mapping from GUACAMOLE_HOME
            if (userMappingFile == null)
                userMappingFile = new File(environment.getGuacamoleHome(), USER_MAPPING_FILENAME);

        }

        // Abort if property cannot be parsed
        catch (GuacamoleException e) {
            logger.warn("Unable to read user mapping filename from properties: {}", e.getMessage());
            logger.debug("Error parsing user mapping property.", e);
            return null;
        }

        // Abort if user mapping does not exist
        if (!userMappingFile.exists()) {
            logger.debug("User mapping file \"{}\" does not exist and will not be read.", userMappingFile);
            return null;
        }

        // Refresh user mapping if file has changed
        if (lastModified < userMappingFile.lastModified()) {

            logger.debug("Reading user mapping file: \"{}\"", userMappingFile);

            // Parse document
            try {

                // Get handler for root element
                UserMappingTagHandler userMappingHandler =
                        new UserMappingTagHandler();

                // Set up document handler
                DocumentHandler contentHandler = new DocumentHandler(
                        "user-mapping", userMappingHandler);

                // Set up XML parser
                XMLReader parser = XMLReaderFactory.createXMLReader();
                parser.setContentHandler(contentHandler);

                // Read and parse file
                InputStream input = new BufferedInputStream(new FileInputStream(userMappingFile));
                parser.parse(new InputSource(input));
                input.close();

                // Store mod time and user mapping
                lastModified = userMappingFile.lastModified();
                cachedUserMapping = userMappingHandler.asUserMapping();

            }

            // If the file is unreadable, return no mapping
            catch (IOException e) {
                logger.warn("Unable to read user mapping file \"{}\": {}", userMappingFile, e.getMessage());
                logger.debug("Error reading user mapping file.", e);
                return null;
            }

            // If the file cannot be parsed, return no mapping
            catch (SAXException e) {
                logger.warn("User mapping file \"{}\" is not valid: {}", userMappingFile, e.getMessage());
                logger.debug("Error parsing user mapping file.", e);
                return null;
            }

        }

        // Return (possibly cached) user mapping
        return cachedUserMapping;

    }

    @Override
    public Map<String, GuacamoleConfiguration>
            getAuthorizedConfigurations(Credentials credentials)
            throws GuacamoleException {

        // Abort authorization if no user mapping exists
        UserMapping userMapping = getUserMapping();
        if (userMapping == null)
            return null;

        // Validate and return info for given user and pass
        Authorization auth = userMapping.getAuthorization(credentials.getUsername());
        if (auth != null && auth.validate(credentials.getUsername(), credentials.getPassword()))
            return auth.getConfigurations();

        // Unauthorized
        return null;

    }

}
