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

package org.glyptodon.guacamole.environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.properties.GuacamoleProperty;
import org.glyptodon.guacamole.protocols.ProtocolInfo;
import org.glyptodon.guacamole.xml.DocumentHandler;
import org.glyptodon.guacamole.xml.protocol.ProtocolTagHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The environment of the locally-running Guacamole instance, describing
 * available protocols, configuration parameters, and the GUACAMOLE_HOME
 * directory.
 *
 * @author Michael Jumper
 */
public class LocalEnvironment implements Environment {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(LocalEnvironment.class);

    /**
     * Array of all known protocol names.
     */
    private static final String[] KNOWN_PROTOCOLS = new String[]{
        "vnc", "rdp", "ssh", "telnet"};

    /**
     * All properties read from guacamole.properties.
     */
    private final Properties properties;

    /**
     * The location of GUACAMOLE_HOME, which may not truly exist.
     */
    private final File guacHome;

    /**
     * The map of all available protocols.
     */
    private final Map<String, ProtocolInfo> availableProtocols;

    /**
     * Creates a new Environment, initializing that environment based on the
     * location of GUACAMOLE_HOME and the contents of guacamole.properties.
     * 
     * @throws GuacamoleException If an error occurs while determining the
     *                            environment of this Guacamole instance.
     */
    public LocalEnvironment() throws GuacamoleException {

        // Determine location of GUACAMOLE_HOME
        guacHome = findGuacamoleHome();

        // Read properties
        properties = new Properties();
        try {

            InputStream stream;

            // If not a directory, load from classpath
            if (!guacHome.isDirectory()) {

                // Read from classpath
                stream = LocalEnvironment.class.getResourceAsStream("/guacamole.properties");
                if (stream == null)
                    throw new GuacamoleServerException(
                        "guacamole.properties not loaded from " + guacHome
                      + " (not a directory), and guacamole.properties could"
                      + " not be found as a resource in the classpath.");

            }

            // Otherwise, try to load from file
            else
                stream = new FileInputStream(new File(guacHome, "guacamole.properties"));

            // Load properties, always close stream
            try { properties.load(stream); }
            finally { stream.close(); }

        }
        catch (IOException e) {
            throw new GuacamoleServerException("Error reading guacamole.properties", e);
        }

        // Read all protocols
        availableProtocols = readProtocols();

    }

    /**
     * Locates the Guacamole home directory by checking, in order:
     * the guacamole.home system property, the GUACAMOLE_HOME environment
     * variable, and finally the .guacamole directory in the home directory of
     * the user running the servlet container.
     *
     * @return The File representing the Guacamole home directory, which may
     *         or may not exist, and may turn out to not be a directory.
     */
    private static File findGuacamoleHome() {

        // Attempt to find Guacamole home
        File guacHome;

        // Use system property by default
        String desiredDir = System.getProperty("guacamole.home");

        // Failing that, try the GUACAMOLE_HOME environment variable
        if (desiredDir == null) desiredDir = System.getenv("GUACAMOLE_HOME");

        // If successful, use explicitly specified directory
        if (desiredDir != null)
            guacHome = new File(desiredDir);

        // If not explicitly specified, use ~/.guacamole
        else
            guacHome = new File(System.getProperty("user.home"), ".guacamole");

        // Return discovered directory
        return guacHome;

    }

    /**
     * Parses the given XML file, returning the parsed ProtocolInfo.
     *
     * @param input An input stream containing XML describing the parameters
     *              associated with a protocol supported by Guacamole.
     * @return A new ProtocolInfo object which contains the parameters described
     *         by the XML file parsed.
     * @throws GuacamoleException If an error occurs while parsing the XML file.
     */
    private ProtocolInfo readProtocol(InputStream input)
            throws GuacamoleException {

        // Parse document
        try {

            // Get handler for root element
            ProtocolTagHandler protocolTagHandler =
                    new ProtocolTagHandler();

            // Set up document handler
            DocumentHandler contentHandler = new DocumentHandler(
                    "protocol", protocolTagHandler);

            // Set up XML parser
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(contentHandler);

            // Read and parse file
            InputStream xml = new BufferedInputStream(input);
            parser.parse(new InputSource(xml));
            xml.close();

            // Return parsed protocol
            return protocolTagHandler.asProtocolInfo();

        }
        catch (IOException e) {
            throw new GuacamoleException("Error reading basic user mapping file.", e);
        }
        catch (SAXException e) {
            throw new GuacamoleException("Error parsing basic user mapping XML.", e);
        }

    }

    /**
     * Reads through all pre-defined protocols and any protocols within the
     * "protocols" subdirectory of GUACAMOLE_HOME, returning a map containing
     * each of these protocols. The key of each entry will be the name of that
     * protocol, as would be passed to guacd during connection.
     *
     * @return A map of all available protocols.
     * @throws GuacamoleException If an error occurs while reading the various
     *                            protocol XML files.
     */
    private Map<String, ProtocolInfo> readProtocols() throws GuacamoleException {

        // Map of all available protocols
        Map<String, ProtocolInfo> protocols = new HashMap<String, ProtocolInfo>();

        // Get protcols directory
        File protocol_directory = new File(getGuacamoleHome(), "protocols");

        // Read protocols from directory if it exists
        if (protocol_directory.isDirectory()) {

            // Get all XML files
            File[] files = protocol_directory.listFiles(
                new FilenameFilter() {

                    @Override
                    public boolean accept(File file, String string) {
                        return string.endsWith(".xml");
                    }

                }
            );

            // Warn if directory contents are not available
            if (files == null) {
                logger.error("Unable to read contents of \"{}\".", protocol_directory.getAbsolutePath());
                files = new File[0];
            }
            
            // Load each protocol from each file
            for (File file : files) {

                try {

                    // Parse protocol
                    FileInputStream stream = new FileInputStream(file);
                    ProtocolInfo protocol = readProtocol(stream);
                    stream.close();

                    // Store protocol
                    protocols.put(protocol.getName(), protocol);

                }
                catch (IOException e) {
                    logger.error("Unable to read connection parameter information from \"{}\": {}", file.getAbsolutePath(), e.getMessage());
                    logger.debug("Error reading protocol XML.", e);
                }

            }

        }

        // If known protocols are not already defined, read from classpath
        for (String protocol : KNOWN_PROTOCOLS) {

            // If protocol not defined yet, attempt to load from classpath
            if (!protocols.containsKey(protocol)) {

                InputStream stream = LocalEnvironment.class.getResourceAsStream(
                        "/org/glyptodon/guacamole/protocols/"
                        + protocol + ".xml");

                // Parse XML if available
                if (stream != null)
                    protocols.put(protocol, readProtocol(stream));

            }

        }

        // Protocols map now fully populated
        return protocols;

    }

    @Override
    public File getGuacamoleHome() {
        return guacHome;
    }

    @Override
    public <Type> Type getProperty(GuacamoleProperty<Type> property) throws GuacamoleException {
        return property.parseValue(properties.getProperty(property.getName()));
    }

    @Override
    public <Type> Type getProperty(GuacamoleProperty<Type> property,
            Type defaultValue) throws GuacamoleException {

        Type value = getProperty(property);
        if (value == null)
            return defaultValue;

        return value;

    }

    @Override
    public <Type> Type getRequiredProperty(GuacamoleProperty<Type> property)
            throws GuacamoleException {

        Type value = getProperty(property);
        if (value == null)
            throw new GuacamoleServerException("Property " + property.getName() + " is required.");

        return value;

    }

    @Override
    public Map<String, ProtocolInfo> getProtocols() {
        return availableProtocols;
    }

    @Override
    public ProtocolInfo getProtocol(String name) {
        return availableProtocols.get(name);
    }

}
