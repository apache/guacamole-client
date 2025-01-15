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
import org.apache.guacamole.resource.ByteArrayResource;
import org.apache.guacamole.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 */
public class NativeFederationService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(NativeFederationService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * TODO
     */
    private final Map<String, NativeFederationConfiguration> nativeFederationConfiguration = new HashMap<>();
    private final Map<String, String> nativeFederationManifest = new HashMap<>();


    /**
     * TODO
     *
     * @param resource
     */
    public void addNativeFederationConfigurationResource(String namespace, Resource resource) {

        if (resource == null) {
            return;
        }

        NativeFederationConfiguration config = null;
        try {
            config = objectMapper.readValue(resource.asStream(), NativeFederationConfiguration.class);
            this.nativeFederationConfiguration.put(namespace, config);
            String staticResourcePrefix = "./app/ext/" + namespace + "/";
            this.nativeFederationManifest.put(namespace, staticResourcePrefix + "remoteEntry.json");
        } catch (IOException e) {

            logger.error("Unable to parse native federation configuration for extension in namespace {}: {}", namespace, e.getMessage());
            logger.debug("Error parsing serialize native federation configuration.", e);

        }

    }


    /**
     * TODO
     *
     * @return
     */
    public Resource getNativeFederationConfiguration() {

        try {
            return new ByteArrayResource("application/json", objectMapper.writeValueAsBytes(this.nativeFederationConfiguration));
        } catch (JsonProcessingException e) {

            logger.error("Unable to serialize native federation configuration: {}", e.getMessage());
            logger.debug("Error serializing serialize native federation configuration.", e);

            // TODO: Fallback -  Empty configuration
            return new ByteArrayResource("application/json", "{}".getBytes(StandardCharsets.UTF_8));
        }

    }

    /**
     * TODO
     *
     * @return
     */
    public Resource getNativeFederationManifest() {
        try {
            return new ByteArrayResource("application/json", objectMapper.writeValueAsBytes(this.nativeFederationManifest));
        } catch (JsonProcessingException e) {

            logger.error("Unable to serialize native federation manifest: {}", e.getMessage());
            logger.debug("Error serializing serialize native federation manifest.", e);

            // TODO: Fallback -  Empty configuration
            return new ByteArrayResource("application/json", "{}".getBytes(StandardCharsets.UTF_8));
        }
    }

}
