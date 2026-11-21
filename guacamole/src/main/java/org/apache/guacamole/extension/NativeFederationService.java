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

package org.apache.guacamole.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.guacamole.resource.ByteArrayResource;
import org.apache.guacamole.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which collects the Native Federation configurations of all
 * extensions and generates from them the Native Federation manifest and
 * configuration, which the web application reads on startup to load the
 * Angular part of each extension.
 */
public class NativeFederationService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(NativeFederationService.class);

    /**
     * The Jackson parser for parsing Native Federation configurations and
     * generating the Native Federation manifest and configuration.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Map of the Native Federation configurations of all extensions added to
     * this service, where the key of each entry is the namespace of the
     * extension. Entries are kept in the order the extensions were added.
     */
    private final Map<String, NativeFederationConfiguration> nativeFederationConfiguration = new LinkedHashMap<>();

    /**
     * Map of the URLs of the remoteEntry.json files of all extensions added
     * to this service, relative to the root of the web application, where the
     * key of each entry is the namespace of the extension. Entries are kept
     * in the order the extensions were added.
     */
    private final Map<String, String> nativeFederationManifest = new LinkedHashMap<>();

    /**
     * Adds the given Native Federation configuration of the extension having
     * the given namespace, such that the web application loads that
     * extension through Native Federation. If no configuration is given,
     * nothing is added. If the configuration does not exist or cannot be
     * parsed, an error is logged and nothing is added.
     *
     * @param namespace
     *     The namespace of the extension. The extension must provide its
     *     remoteEntry.json as a static resource at the root of its .jar, with
     *     this namespace as the name of the remote.
     *
     * @param resource
     *     The Native Federation configuration of the extension, or null if
     *     the extension does not provide one. This resource must have the
     *     mimetype "application/json".
     */
    public void addNativeFederationConfigurationResource(String namespace, Resource resource) {

        // Nothing to add for extensions without Native Federation
        if (resource == null)
            return;

        // Read configuration from the extension .jar
        try (InputStream stream = resource.asStream()) {

            // Omit extension if its configuration does not exist
            if (stream == null) {
                logger.error("Native Federation configuration of extension \"{}\" does not exist.", namespace);
                return;
            }

            // Parse configuration
            NativeFederationConfiguration config = mapper.readValue(stream, NativeFederationConfiguration.class);
            nativeFederationConfiguration.put(namespace, config);

            // Point to the remoteEntry.json among the static resources of the
            // extension, relative to the root of the web application
            nativeFederationManifest.put(namespace, "./app/ext/" + namespace + "/remoteEntry.json");

        }

        // Omit extension if its configuration cannot be read
        catch (IOException e) {
            logger.error("Unable to parse Native Federation configuration of extension \"{}\": {}",
                    namespace, e.getMessage(), e);
        }

    }

    /**
     * Returns the Native Federation configuration of all extensions added so
     * far, as a JSON object mapping the namespace of each extension to its
     * configuration. The web application reads this configuration on
     * startup, calls the bootstrap function of each extension and adds the
     * returned routes below the route path of that extension. If the
     * configuration cannot be generated, an error is logged and the resource
     * contains an empty JSON object, such that the web application starts
     * without extensions.
     *
     * @return
     *     A resource containing the Native Federation configuration of all
     *     extensions added so far, with the mimetype "application/json".
     *     Extensions added later are not included.
     */
    public Resource getNativeFederationConfiguration() {

        // Generate configuration from those of all extensions
        try {
            return new ByteArrayResource("application/json", mapper.writeValueAsBytes(nativeFederationConfiguration));
        }

        // Fall back to a configuration without extensions
        catch (JsonProcessingException e) {
            logger.error("Unable to generate Native Federation configuration: {}", e.getMessage(), e);
            return new ByteArrayResource("application/json", "{}".getBytes(StandardCharsets.UTF_8));
        }

    }

    /**
     * Returns the Native Federation manifest of all extensions added so far,
     * as a JSON object mapping the namespace of each extension to the URL of
     * its remoteEntry.json. When the web application starts, Native
     * Federation reads this manifest and loads the remoteEntry.json of each
     * extension. If the manifest cannot be generated, an error is logged and
     * the resource contains an empty JSON object, such that the web
     * application starts without extensions.
     *
     * @return
     *     A resource containing the Native Federation manifest of all
     *     extensions added so far, with the mimetype "application/json".
     *     Extensions added later are not included.
     */
    public Resource getNativeFederationManifest() {

        // Generate manifest from the remoteEntry.json URLs of all extensions
        try {
            return new ByteArrayResource("application/json", mapper.writeValueAsBytes(nativeFederationManifest));
        }

        // Fall back to a manifest without extensions
        catch (JsonProcessingException e) {
            logger.error("Unable to generate Native Federation manifest: {}", e.getMessage(), e);
            return new ByteArrayResource("application/json", "{}".getBytes(StandardCharsets.UTF_8));
        }

    }

}
