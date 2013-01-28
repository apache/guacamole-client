
package net.sourceforge.guacamole.net.basic;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import net.sourceforge.guacamole.net.basic.auth.Authorization;
import net.sourceforge.guacamole.net.basic.auth.UserMapping;
import net.sourceforge.guacamole.net.basic.xml.DocumentHandler;
import net.sourceforge.guacamole.net.basic.xml.user_mapping.UserMappingTagHandler;
import net.sourceforge.guacamole.properties.FileGuacamoleProperty;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
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

    private Logger logger = LoggerFactory.getLogger(BasicFileAuthenticationProvider.class);

    /**
     * The time the user mapping file was last modified.
     */
    private long mod_time;

    /**
     * The parsed UserMapping read when the user mapping file was last parsed.
     */
    private UserMapping user_mapping;

    /**
     * The filename of the XML file to read the user user_mapping from.
     */
    public static final FileGuacamoleProperty BASIC_USER_MAPPING = new FileGuacamoleProperty() {

        @Override
        public String getName() { return "basic-user-mapping"; }

    };

    /**
     * Returns a UserMapping containing all authorization data given within
     * the XML file specified by the "basic-user-mapping" property in
     * guacamole.properties. If the XML file has been modified or has not yet
     * been read, this function may reread the file.
     * 
     * @return A UserMapping containing all authorization data within the
     *         user mapping XML file.
     * @throws GuacamoleException If the user mapping property is missing or
     *                            an error occurs while parsing the XML file.
     */
    private UserMapping getUserMapping() throws GuacamoleException {

        // Get user user_mapping file
        File user_mapping_file =
                GuacamoleProperties.getRequiredProperty(BASIC_USER_MAPPING);

        // If user_mapping not yet read, or user_mapping has been modified, reread
        if (user_mapping == null ||
                (user_mapping_file.exists()
                 && mod_time < user_mapping_file.lastModified())) {

            logger.info("Reading user mapping file: {}", user_mapping_file);

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
                Reader reader = new BufferedReader(new FileReader(user_mapping_file));
                parser.parse(new InputSource(reader));
                reader.close();

                // Store mod time and user mapping
                mod_time = user_mapping_file.lastModified();
                user_mapping = userMappingHandler.asUserMapping();

            }
            catch (IOException e) {
                throw new GuacamoleException("Error reading basic user mapping file.", e);
            }
            catch (SAXException e) {
                throw new GuacamoleException("Error parsing basic user mapping XML.", e);
            }

        }

        // Return (possibly cached) user mapping
        return user_mapping;

    }

    @Override
    public Map<String, GuacamoleConfiguration>
            getAuthorizedConfigurations(Credentials credentials)
            throws GuacamoleException {

        // Validate and return info for given user and pass
        Authorization auth = getUserMapping().getAuthorization(credentials.getUsername());
        if (auth != null && auth.validate(credentials.getUsername(), credentials.getPassword()))
            return auth.getConfigurations();

        // Unauthorized
        return null;

    }

}