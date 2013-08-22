
package net.sourceforge.guacamole.net.auth.noauth;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-auth-noauth.
 *
 * The Initial Developer of the Original Code is
 * Laurent Meunier
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.properties.FileGuacamoleProperty;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * Disable authentication in Guacamole. All users accessing Guacamole are
 * automatically authenticated as "Anonymous" user and are able to use all
 * available GuacamoleConfigurations.
 *
 * GuacamoleConfiguration are read from the XML file defined by `noauth-config`
 * in the Guacamole configuration file (`guacamole.properties`).
 *
 *
 * Example `guacamole.properties`:
 *
 *  auth-provider: net.sourceforge.guacamole.net.auth.noauth.NoAuthenticationProvider
 *  noauth-config: /etc/guacamole/noauth-config.xml
 *
 *
 * Example `noauth-config.xml`:
 *
 *  <configs>
 *    <config name="my-rdp-server" protocol="rdp">
 *      <param name="hostname" value="my-rdp-server-hostname" />
 *      <param name="port" value="3389" />
 *    </config>
 *  </configs>
 *
 */
public class NoAuthenticationProvider extends SimpleAuthenticationProvider {

    private Logger logger = LoggerFactory.getLogger(NoAuthenticationProvider.class);
    private Map<String, GuacamoleConfiguration> configs;
    private long configTime;

    /**
     * The filename of the XML file to read the user mapping from.
     */
    public static final FileGuacamoleProperty NOAUTH_CONFIG = new FileGuacamoleProperty() {
        @Override
        public String getName() {
            return "noauth-config";
        }
    };

    private File getConfigurationFile() throws GuacamoleException {
        // Get configuration file
        return GuacamoleProperties.getProperty(NOAUTH_CONFIG);
    }

    public synchronized void init() throws GuacamoleException {
        // Get configuration file
        File configFile = getConfigurationFile();
        if(configFile == null) {
            throw new GuacamoleException(
                "Missing \"noauth-config\" parameter required for NoAuthenticationProvider."
            );
        }

        logger.info("Reading configuration file: {}", configFile);

        // Parse document
        try {
            // Set up parser
            NoAuthConfigContentHandler contentHandler = new NoAuthConfigContentHandler();

            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(contentHandler);

            // Read and parse file
            Reader reader = new BufferedReader(new FileReader(configFile));
            parser.parse(new InputSource(reader));
            reader.close();

            // Init configs
            configTime = configFile.lastModified();
            configs = contentHandler.getConfigs();
        }
        catch (IOException e) {
            throw new GuacamoleException("Error reading configuration file: " + e.getMessage(), e);
        }
        catch (SAXException e) {
            throw new GuacamoleException("Error parsing XML file: " + e.getMessage(), e);
        }

    }

    @Override
    public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(Credentials credentials) throws GuacamoleException {
        // Check mapping file mod time
        File configFile = getConfigurationFile();
        if (configFile.exists() && configTime < configFile.lastModified()) {
            // If modified recently, gain exclusive access and recheck
            synchronized (this) {
                if (configFile.exists() && configTime < configFile.lastModified()) {
                    logger.info("Config file {} has been modified.", configFile);
                    init(); // If still not up to date, re-init
                }
            }
        }

        // If no mapping available, report as such
        if (configs == null) {
            throw new GuacamoleException("Configuration could not be read.");
        }

        // Guacamole 0.8 wants a username to be set, otherwise the
        // authentication process will fail.
        credentials.setUsername("Anonymous");

        return configs;
    }
}
