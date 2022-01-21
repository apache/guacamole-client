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

package org.apache.guacamole.auth.vault.conf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.vault.VaultAuthenticationProviderModule;
import org.apache.guacamole.environment.Environment;

/**
 * Base class for services which retrieve key vault configuration information.
 * A concrete implementation of this class must be defined and bound for key
 * vault support to work.
 *
 * @see VaultAuthenticationProviderModule
 */
public abstract class VaultConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * ObjectMapper for deserializing JSON.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The name of the file containing a JSON mapping of Guacamole parameter
     * token to vault secret name.
     */
    private final String tokenMappingFilename;

    /**
     * Creates a new VaultConfigurationService which retrieves the token/secret
     * mapping from a JSON file having the given name.
     *
     * @param tokenMappingFilename
     *     The name of the JSON file containing the token/secret mapping.
     */
    protected VaultConfigurationService(String tokenMappingFilename) {
        this.tokenMappingFilename = tokenMappingFilename;
    }

    /**
     * Returns a mapping dictating the name of the secret which maps to each
     * parameter token. In the returned mapping, the value of each entry is the
     * name of the secret to use to populate the value of the parameter token,
     * and the key of each entry is the name of the parameter token which
     * should receive the value of the secret.
     *
     * The name of the secret may contain its own tokens, which will be
     * substituted using values from the given filter. See the definition of
     * VaultUserContext for the names of these tokens and the contexts in which
     * they can be applied to secret names.
     *
     * @return
     *     A mapping dictating the name of the secret which maps to each
     *     parameter token.
     *
     * @throws GuacamoleException
     *     If the JSON file defining the token/secret mapping cannot be read.
     */
    public Map<String, String> getTokenMapping() throws GuacamoleException {

        // Get configuration file from GUACAMOLE_HOME
        File confFile = new File(environment.getGuacamoleHome(), tokenMappingFilename);

        // Deserialize token mapping from JSON
        try {
            return mapper.readValue(confFile, new TypeReference<Map<String, String>>() {});
        }

        // Fail if JSON is invalid/unreadable
        catch (IOException e) {
            throw new GuacamoleServerException("Unable to read token mapping "
                    + "configuration file \"" + tokenMappingFilename + "\".", e);
        }

    }

}
