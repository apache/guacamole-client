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

package org.apache.guacamole.vault.ksm.user;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.language.TranslatableGuacamoleClientException;
import org.apache.guacamole.net.auth.Attributes;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.DelegatingDirectory;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.vault.ksm.conf.KsmAttributeService;
import org.apache.guacamole.vault.ksm.conf.KsmConfig;
import org.apache.guacamole.vault.ksm.conf.KsmConfigurationService;
import org.apache.guacamole.vault.user.VaultDirectoryService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.keepersecurity.secretsManager.core.InMemoryStorage;
import com.keepersecurity.secretsManager.core.SecretsManager;
import com.keepersecurity.secretsManager.core.SecretsManagerOptions;

/**
 * A KSM-specific vault directory service that wraps the connection group directory
 * to enable automatic translation of KSM one-time tokens into base64-encoded JSON
 * config bundles.
 */
public class KsmDirectoryService extends VaultDirectoryService {

    /**
     * Service for retrieving KSM configuration details.
     */
    @Inject
    private KsmConfigurationService configurationService;

    /**
     * A singleton ObjectMapper for converting a Map to a JSON string when
     * generating a base64-encoded JSON KSM config blob.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * All expected fields in the KSM configuration JSON blob.
     */
    private static final List<String> EXPECTED_KSM_FIELDS = (
            Collections.unmodifiableList(Arrays.asList(
                    SecretsManager.KEY_HOSTNAME,
                    SecretsManager.KEY_CLIENT_ID,
                    SecretsManager.KEY_PRIVATE_KEY,
                    SecretsManager.KEY_CLIENT_KEY,
                    SecretsManager.KEY_APP_KEY,
                    SecretsManager.KEY_OWNER_PUBLIC_KEY,
                    SecretsManager.KEY_SERVER_PUBIC_KEY_ID
    )));

    /**
     * Return true if the provided input is a valid base64-encoded string,
     * false otherwise.
     *
     * @param input
     *     The string to check if base-64 encoded.
     *
     * @return
     *     true if the provided input is a valid base64-encoded string,
     *     false otherwise.
     */
    private static boolean isBase64(String input) {

        try {
            Base64.getDecoder().decode(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Given an attributes-enabled entity, check for the presence of the
     * KSM_CONFIGURATION_ATTRIBUTE attribute. If it's set, check if it's a valid
     * KSM one-time token. If so, attempt to translate it to a base-64-encoded
     * json KSM config blob, and set it back to the provided entity.
     * If it's already a KSM config blob, validate it as config blob. If either
     * validation fails, a GuacamoleException will be thrown.
     *
     * @param entity
     *     The attributes-enabled entity for which the KSM configuration
     *     attribute parsing/validation should be performed.
     *
     * @throws GuacamoleException
     *     If the KSM_CONFIGURATION_ATTRIBUTE is set, but fails to validate as
     *     either a KSM one-time-token, or a KSM base64-encoded JSON config blob.
     */
    public void processAttributes(Attributes entity) throws GuacamoleException {

        // By default, if the KSM config attribute isn't being set, pass the
        // provided attributes through without any changes
        Map<String, String> attributes = entity.getAttributes();

        // Get the value of the KSM config attribute in the provided map
        String ksmConfigValue = attributes.get(
                KsmAttributeService.KSM_CONFIGURATION_ATTRIBUTE);

        // Check if the attribute is set to a non-empty value
        if (ksmConfigValue != null && !ksmConfigValue.trim().isEmpty()) {

            // If it's already base64-encoded, it's a KSM configuration blob,
            // so validate it immediately
            if (isBase64(ksmConfigValue)) {

                // Attempt to validate the config as a base64-econded KSM config blob
                try {
                    KsmConfig.parseKsmConfig(ksmConfigValue);

                    // If it validates, the entity can be left alone - it's already valid
                    return;
                }

                catch (GuacamoleException exception) {

                    // If the parsing attempt fails, throw a translatable error for display
                    // on the frontend
                    throw new TranslatableGuacamoleClientException(
                            "Invalid base64-encoded JSON KSM config provided for "
                            + KsmAttributeService.KSM_CONFIGURATION_ATTRIBUTE + " attribute",
                            "CONNECTION_GROUP_ATTRIBUTES.ERROR_INVALID_KSM_CONFIG_BLOB",
                            exception);
                }
            }

            // It wasn't a valid base64-encoded string, it should be a one-time token, so
            // attempt to validat it as such, and if valid, update the attribute to the
            // base64 config blob generated by the token
            try {

                // Create an initially empty storage to be populated using the one-time token
                InMemoryStorage storage = new InMemoryStorage();

                // Populate the in-memory storage using the one-time-token
                SecretsManager.initializeStorage(storage, ksmConfigValue, null);

                // Create an options object using the values we extracted from the one-time token
                SecretsManagerOptions options = new SecretsManagerOptions(
                    storage, null,
                    configurationService.getAllowUnverifiedCertificate());

                // Attempt to fetch secrets using the options we created. This will both validate
                // that the configuration works, and potentially populate missing fields that the
                // initializeStorage() call did not set.
                SecretsManager.getSecrets(options);

                // Create a map to store the extracted values from the KSM storage
                Map<String, String> configMap = new HashMap<>();

                // Go through all the expected fields, extract from the KSM storage,
                // and write to the newly created map
                EXPECTED_KSM_FIELDS.forEach(configKey -> {

                    // Only write the value into the new map if non-null
                    String value = storage.getString(configKey);
                    if (value != null)
                        configMap.put(configKey, value);

                });

                // JSON-encode the value, and then base64 encode that to get the format
                // that KSM would expect
                String jsonString = objectMapper.writeValueAsString(configMap);
                String base64EncodedJson = Base64.getEncoder().encodeToString(
                        jsonString.getBytes(StandardCharsets.UTF_8));

                // Finally, try to parse the newly generated token as a KSM config. If this
                // works, the config should be fully functional
                KsmConfig.parseKsmConfig(base64EncodedJson);

                // Make a copy of the existing attributes, modifying just the value for
                // KSM_CONFIGURATION_ATTRIBUTE
                attributes = new HashMap<>(attributes);
                attributes.put(
                        KsmAttributeService.KSM_CONFIGURATION_ATTRIBUTE, base64EncodedJson);

                // Set the newly updated attributes back to the original object
                entity.setAttributes(attributes);

            }

            // The KSM SDK only throws raw Exceptions, so we can't be more specific
            catch (Exception exception) {

                // If the parsing attempt fails, throw a translatable error for display
                // on the frontend
                throw new TranslatableGuacamoleClientException(
                        "Invalid one-time KSM token provided for "
                        + KsmAttributeService.KSM_CONFIGURATION_ATTRIBUTE + " attribute",
                        "CONNECTION_GROUP_ATTRIBUTES.ERROR_INVALID_KSM_ONE_TIME_TOKEN",
                        exception);
            }
        }

    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory(
            Directory<ConnectionGroup> underlyingDirectory) throws GuacamoleException {

        // A ConnectionGroup directory that will intercept add and update calls to
        // validate KSM configurations, and translate one-time-tokens, if possible
        return new DelegatingDirectory<ConnectionGroup>(underlyingDirectory) {

            @Override
            public void add(ConnectionGroup connectionGroup) throws GuacamoleException {

                // Check for the KSM config attribute and translate the one-time token
                // if possible before adding
                processAttributes(connectionGroup);
                super.add(connectionGroup);
            }

            @Override
            public void update(ConnectionGroup connectionGroup) throws GuacamoleException {

                // Check for the KSM config attribute and translate the one-time token
                // if possible before updating
                processAttributes(connectionGroup);
                super.update(connectionGroup);
            }

        };
    }
}
