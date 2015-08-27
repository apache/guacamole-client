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

package net.sourceforge.guacamole.net.basic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.environment.LocalEnvironment;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.glyptodon.guacamole.net.basic.auth.Authorization;
import org.glyptodon.guacamole.net.basic.auth.UserMapping;
import org.glyptodon.guacamole.xml.DocumentHandler;
import org.glyptodon.guacamole.net.basic.xml.usermapping.UserMappingTagHandler;
import org.glyptodon.guacamole.properties.FileGuacamoleProperty;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
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
 *
 * @author Michael Jumper, Michal Kotas
 */
public class BasicFileAuthenticationProvider extends SimpleAuthenticationProvider {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(BasicFileAuthenticationProvider.class);

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
     * The XML file to read the user mapping from.
     */
    public static final FileGuacamoleProperty BASIC_USER_MAPPING = new FileGuacamoleProperty() {

        @Override
        public String getName() { return "basic-user-mapping"; }

    };

    /**
     * The default filename to use for the user mapping, if not defined within
     * guacamole.properties.
     */
    public static final String DEFAULT_USER_MAPPING = "user-mapping.xml";
    
    /**
     * Creates a new BasicFileAuthenticationProvider that authenticates users
     * against simple, monolithic XML file.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public BasicFileAuthenticationProvider() throws GuacamoleException {
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
    private UserMapping getUserMapping() {

        // Get user mapping file, defaulting to GUACAMOLE_HOME/user-mapping.xml
        File userMappingFile;
        try {
            userMappingFile = environment.getProperty(BASIC_USER_MAPPING);
            if (userMappingFile == null)
                userMappingFile = new File(environment.getGuacamoleHome(), DEFAULT_USER_MAPPING);
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
        Authorization auth = getUserMapping().getAuthorization(credentials.getUsername());
        if (auth != null && auth.validate(credentials.getUsername(), credentials.getPassword()))
            return auth.getConfigurations();

        // Unauthorized
        return null;

    }

}
