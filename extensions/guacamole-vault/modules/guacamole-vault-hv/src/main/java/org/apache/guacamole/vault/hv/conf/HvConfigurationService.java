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

package org.apache.guacamole.vault.hv.conf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.vault.conf.VaultConfigurationService;

@Singleton
public class HvConfigurationService extends VaultConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * The name of the file which contains the YAML mapping of connection
     * parameter token to secrets within Hashicorp Vault.
     */
    private static final String TOKEN_MAPPING_FILENAME = "hv-token-mapping.yml";

    /**
     * The name of the properties file containing Guacamole configuration
     * properties whose values are the names of corresponding secrets within
     * Hashicorp Vault.
     */
    private static final String PROPERTIES_FILENAME = "guacamole.properties.hv";

    /**
     * The base64-encoded configuration information.
     */
    private static final StringGuacamoleProperty HV_CONFIG = new StringGuacamoleProperty() {
        @Override
        public String getName() {
            return "hv-config";
        }
    };

    /**
     * Whether unverified server certificates should be accepted.
     */
    private static final BooleanGuacamoleProperty ALLOW_UNVERIFIED_CERT = new BooleanGuacamoleProperty() {
        @Override
        public String getName() {
            return "hv-allow-unverified-cert";
        }
    };

    /**
     * Whether users should be able to supply their own HV configurations.
     */
    private static final BooleanGuacamoleProperty ALLOW_USER_CONFIG = new BooleanGuacamoleProperty() {
        @Override
        public String getName() {
            return "hv-allow-user-config";
        }
    };

    /**
     * Creates a new HvConfigurationService which reads the configuration
     * from "hv-token-mapping.yml" and properties from
     * "guacamole.properties.hv". The token mapping is a YAML file which lists
     * each connection parameter token and the name of the secret from which
     * the value for that token should be read, while the properties file is an
     * alternative to guacamole.properties where each property value is the
     * name of a secret containing the actual value.
     */
    public HvConfigurationService() {
        super(TOKEN_MAPPING_FILENAME, PROPERTIES_FILENAME);
    }

    /**
     * Return whether user-level HV configs should be enabled. If this
     * flag is set to true, users can edit their own HV configs, as can
     * admins. If set to false, no existing user-specific HV configuration
     * will be exposed through the UI or used when looking up secrets.
     *
     * @return
     *     true if user-specific HV configuration is enabled, false otherwise.
     *
     * @throws GuacamoleException
     *     If the value specified within guacamole.properties cannot be
     *     parsed.
     */
    public boolean getAllowUserConfig() throws GuacamoleException {
        return environment.getProperty(ALLOW_USER_CONFIG, false);
    }

    // Not used
    @Override
    public boolean getSplitWindowsUsernames() throws GuacamoleException {
        return false;
    }

    // Not used
    @Override
    public boolean getMatchUserRecordsByDomain() throws GuacamoleException {
        return false;
    }

    /**
     * Return the globally-defined base-64-encoded JSON HV configuration blob
     * as a string.
     *
     * @return
     *     The globally-defined base-64-encoded JSON HV configuration blob
     *     as a string.
     *
     * @throws GuacamoleException
     *     If the value specified within guacamole.properties cannot be
     *     parsed or does not exist.
     */
    @Nonnull
    @SuppressWarnings("null")
    public String getHvConfig() throws GuacamoleException {

        // This will always return a non-null value; an exception would be
        // thrown if the required value is not set
        return environment.getRequiredProperty(HV_CONFIG);
    }

    /**
     * Given a base64-encoded JSON HV configuration, parse and return a
     * KeyValueStorage object.
     *
     * @param value
     *     The base64-encoded JSON HV configuration to parse.
     *
     * @return
     *     The KeyValueStorage that is a result of the parsing operation
     *
     * @throws GuacamoleException
     *     If the provided value is not valid base-64 encoded JSON HV configuration.
     */
    public static Map<String, String> parseHvConfig(String value) throws GuacamoleException {
        try {
            Map<String, String> config = new HashMap<>();
            String valueDecoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(valueDecoded);
            jsonNode.properties().forEach(entry -> {
                config.put(entry.getKey(), entry.getValue().asText());
            });

            return config;

        } catch (IOException e) {
            throw new GuacamoleServerException("Invalid JSON configuration for Hashicorp Vault.", e);
        } catch (IllegalArgumentException e) {
            throw new GuacamoleServerException("Invalid base64 configuration for Hashicorp Vault.", e);
        }
    }

}
