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

package org.apache.guacamole.environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.codehaus.jackson.map.ObjectMapper;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.properties.GuacamoleProperty;
import org.apache.guacamole.protocols.ProtocolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The environment of the locally-running Guacamole instance, describing
 * available protocols, configuration parameters, and the GUACAMOLE_HOME
 * directory.
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
     * The hostname to use when connecting to guacd if no hostname is provided
     * within guacamole.properties.
     */
    private static final String DEFAULT_GUACD_HOSTNAME = "localhost";

    /**
     * The port to use when connecting to guacd if no port is provided within
     * guacamole.properties.
     */
    private static final int DEFAULT_GUACD_PORT = 4822;

    /**
     * Whether SSL/TLS is enabled for connections to guacd if not specified
     * within guacamole.properties.
     */
    private static final boolean DEFAULT_GUACD_SSL = false;

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
     * The Jackson parser for parsing JSON files.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

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
        logger.info("GUACAMOLE_HOME is \"{}\".", guacHome.getAbsolutePath());

        // Read properties
        properties = new Properties();
        try {

            InputStream stream = null;

            // If not a directory, load from classpath
            if (!guacHome.isDirectory())
                stream = LocalEnvironment.class.getResourceAsStream("/guacamole.properties");

            // Otherwise, try to load from file
            else {
                File propertiesFile = new File(guacHome, "guacamole.properties");
                if (propertiesFile.exists())
                    stream = new FileInputStream(propertiesFile);
            }

            // Load properties from stream, if any, always closing stream when done
            if (stream != null) {
                try { properties.load(stream); }
                finally { stream.close(); }
            }

            // Notify if we're proceeding without guacamole.properties
            else
                logger.info("No guacamole.properties file found within GUACAMOLE_HOME or the classpath. Using defaults.");

        }
        catch (IOException e) {
            logger.warn("The guacamole.properties file within GUACAMOLE_HOME cannot be read: {}", e.getMessage());
            logger.debug("Error reading guacamole.properties.", e);
        }

        // Read all protocols
        availableProtocols = readProtocols();

    }

    /**
     * Locates the Guacamole home directory by checking, in order:
     * the guacamole.home system property, the GUACAMOLE_HOME environment
     * variable, and finally the .guacamole directory in the home directory of
     * the user running the servlet container. If even the .guacamole directory
     * doesn't exist, then /etc/guacamole will be used.
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

        // If not explicitly specified, use standard locations
        else {

            // Try ~/.guacamole first
            guacHome = new File(System.getProperty("user.home"), ".guacamole");

            // If that doesn't exist, try /etc/guacamole if the /etc directory
            // exists on this system
            if (!guacHome.exists() && new File("/etc").exists())
                guacHome = new File("/etc/guacamole");

        }

        // Return discovered directory
        return guacHome;

    }

    /**
     * Parses the given JSON file, returning the parsed ProtocolInfo. The JSON
     * format is conveniently and intentionally identical to a serialized
     * ProtocolInfo object, which is identical to the JSON format used by the
     * protocol REST service built into the Guacamole web application.
     *
     * @param input
     *     An input stream containing JSON describing the forms and parameters
     *     associated with a protocol supported by Guacamole.
     *
     * @return
     *     A new ProtocolInfo object which contains the forms and parameters
     *     described by the JSON file parsed.
     *
     * @throws IOException
     *     If an error occurs while parsing the JSON file.
     */
    private ProtocolInfo readProtocol(InputStream input)
            throws IOException {
        return mapper.readValue(input, ProtocolInfo.class);
    }

    /**
     * Reads through all pre-defined protocols and any protocols within the
     * "protocols" subdirectory of GUACAMOLE_HOME, returning a map containing
     * each of these protocols. The key of each entry will be the name of that
     * protocol, as would be passed to guacd during connection.
     *
     * @return
     *     A map of all available protocols.
     */
    private Map<String, ProtocolInfo> readProtocols() {

        // Map of all available protocols
        Map<String, ProtocolInfo> protocols = new HashMap<String, ProtocolInfo>();

        // Get protcols directory
        File protocol_directory = new File(getGuacamoleHome(), "protocols");

        // Read protocols from directory if it exists
        if (protocol_directory.isDirectory()) {

            // Get all JSON files
            File[] files = protocol_directory.listFiles(
                new FilenameFilter() {

                    @Override
                    public boolean accept(File file, String string) {
                        return string.endsWith(".json");
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
                    logger.debug("Error reading protocol JSON.", e);
                }

            }

        }

        // If known protocols are not already defined, read from classpath
        for (String protocol : KNOWN_PROTOCOLS) {

            // If protocol not defined yet, attempt to load from classpath
            if (!protocols.containsKey(protocol)) {

                InputStream stream = LocalEnvironment.class.getResourceAsStream(
                        "/org/apache/guacamole/protocols/"
                        + protocol + ".json");

                // Parse JSON if available
                if (stream != null) {
                    try {
                        protocols.put(protocol, readProtocol(stream));
                    }
                    catch (IOException e) {
                        logger.error("Unable to read pre-defined connection parameter information for protocol \"{}\": {}", protocol, e.getMessage());
                        logger.debug("Error reading pre-defined protocol JSON.", e);
                    }
                }

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

    @Override
    public GuacamoleProxyConfiguration getDefaultGuacamoleProxyConfiguration()
            throws GuacamoleException {

        // Parse guacd hostname/port/ssl properties
        return new GuacamoleProxyConfiguration(
            getProperty(Environment.GUACD_HOSTNAME, DEFAULT_GUACD_HOSTNAME),
            getProperty(Environment.GUACD_PORT, DEFAULT_GUACD_PORT),
            getProperty(Environment.GUACD_SSL, DEFAULT_GUACD_SSL)
        );

    }

}
