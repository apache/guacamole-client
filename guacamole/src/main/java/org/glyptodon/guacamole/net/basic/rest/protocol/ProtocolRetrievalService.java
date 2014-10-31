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

package org.glyptodon.guacamole.net.basic.rest.protocol;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.basic.ProtocolInfo;
import org.glyptodon.guacamole.net.basic.xml.DocumentHandler;
import org.glyptodon.guacamole.net.basic.xml.protocol.ProtocolTagHandler;
import org.glyptodon.guacamole.properties.GuacamoleHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A service for retrieving protocol definitions from the 
 * XML files storing the information.
 * 
 * @author James Muehlner
 */
public class ProtocolRetrievalService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ProtocolRetrievalService.class);
    
    /**
     * Array of all known protocol names.
     */
    private static final String[] KNOWN_PROTOCOLS = new String[]{
        "vnc", "rdp", "ssh", "telnet"};

    /**
     * Parses the given XML file, returning the parsed ProtocolInfo.
     *
     * @param input An input stream containing XML describing the parameters
     *              associated with a protocol supported by Guacamole.
     * @return A new ProtocolInfo object which contains the parameters described
     *         by the XML file parsed.
     * @throws GuacamoleException If an error occurs while parsing the XML file.
     */
    private ProtocolInfo getProtocol(InputStream input)
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
            throw new GuacamoleException(e);
        }
        catch (SAXException e) {
            throw new GuacamoleException(e);
        }

    }

    /**
     * Retrieves a map of protocol name to ProtocolInfo for all protocols defined
     * in the XML files.
     *
     * @return A map of all defined protocols.
     * @throws GuacamoleException If an error occurs while retrieving the protocols.
     */
    public Map<String, ProtocolInfo> getProtocolMap() throws GuacamoleException {
        // Map of all available protocols
        Map<String, ProtocolInfo> protocols = new HashMap<String, ProtocolInfo>();

        // Get protcols directory
        File protocol_directory = new File(GuacamoleHome.getDirectory(),
                "protocols");

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

            // Load each protocol from each file
            for (File file : files) {
                try {
                    // Parse protocol
                    FileInputStream stream = new FileInputStream(file);
                    ProtocolInfo protocol = getProtocol(stream);
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
                InputStream stream = ProtocolRetrievalService.class.getResourceAsStream(
                        "/net/sourceforge/guacamole/net/protocols/"
                        + protocol + ".xml");

                // Parse XML if available
                if (stream != null)
                    protocols.put(protocol, getProtocol(stream));

            }
        }
        
        return protocols;
    }

}
