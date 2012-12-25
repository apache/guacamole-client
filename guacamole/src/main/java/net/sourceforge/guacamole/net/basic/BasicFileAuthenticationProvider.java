
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
import net.sourceforge.guacamole.net.auth.AuthenticationProvider;
import net.sourceforge.guacamole.net.auth.Credentials;
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
public class BasicFileAuthenticationProvider implements AuthenticationProvider {

    private Logger logger = LoggerFactory.getLogger(BasicFileAuthenticationProvider.class);

    private long mappingTime;
    private UserMapping mapping;

    /**
     * The filename of the XML file to read the user mapping from.
     */
    public static final FileGuacamoleProperty BASIC_USER_MAPPING = new FileGuacamoleProperty() {

        @Override
        public String getName() { return "basic-user-mapping"; }

    };

    private File getUserMappingFile() throws GuacamoleException {

        // Get user mapping file
        return GuacamoleProperties.getProperty(BASIC_USER_MAPPING);

    }

    public synchronized void init() throws GuacamoleException {

        // Get user mapping file
        File mapFile = getUserMappingFile();
        if (mapFile == null)
            throw new GuacamoleException("Missing \"basic-user-mapping\" parameter required for basic login.");

        logger.info("Reading user mapping file: {}", mapFile);

        // Parse document
        try {

            UserMappingTagHandler userMappingHandler =
                    new UserMappingTagHandler();
            
            // Set up parser
            DocumentHandler contentHandler = new DocumentHandler(
                    "user-mapping", userMappingHandler);

            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(contentHandler);

            // Read and parse file
            Reader reader = new BufferedReader(new FileReader(mapFile));
            parser.parse(new InputSource(reader));
            reader.close();

            // Init mapping and record mod time of file
            mappingTime = mapFile.lastModified();
            mapping = userMappingHandler.asUserMapping();

        }
        catch (IOException e) {
            throw new GuacamoleException("Error reading basic user mapping file.", e);
        }
        catch (SAXException e) {
            throw new GuacamoleException("Error parsing basic user mapping XML.", e);
        }

    }

    @Override
    public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(Credentials credentials) throws GuacamoleException {

        // Check mapping file mod time
        File userMappingFile = getUserMappingFile();
        if (userMappingFile.exists() && mappingTime < userMappingFile.lastModified()) {

            // If modified recently, gain exclusive access and recheck
            synchronized (this) {
                if (userMappingFile.exists() && mappingTime < userMappingFile.lastModified()) {
                    logger.info("User mapping file {} has been modified.", userMappingFile);
                    init(); // If still not up to date, re-init
                }
            }

        }

        // If no mapping available, report as such
        if (mapping == null)
            throw new GuacamoleException("User mapping could not be read.");

        // Validate and return info for given user and pass
        Authorization auth = mapping.getAuthorization(credentials.getUsername());
        if (auth != null && auth.validate(credentials.getUsername(), credentials.getPassword()))
            return auth.getConfigurations();

        // Unauthorized
        return null;

    }

}