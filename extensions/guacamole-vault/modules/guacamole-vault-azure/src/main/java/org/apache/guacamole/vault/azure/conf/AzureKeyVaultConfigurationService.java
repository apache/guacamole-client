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

package org.apache.guacamole.vault.azure.conf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.aad.adal4j.ClientCredential;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.vault.conf.VaultConfigurationService;

/**
 * Service for retrieving configuration information regarding the Azure Key
 * Vault authentication extension.
 */
@Singleton
public class AzureKeyVaultConfigurationService extends VaultConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * The name of the file which contains the JSON mapping of connection
     * parameter token to Azure Key Vault secret name.
     */
    private static final String TOKEN_MAPPING_FILENAME = "azure-keyvault-token-mapping.json";

    /**
     * The number of milliseconds that each retrieved secret should be cached
     * for.
     */
    private static final IntegerGuacamoleProperty SECRET_TTL = new IntegerGuacamoleProperty() {

        @Override
        public String getName() {
            return "azure-keyvault-secret-ttl";
        }

    };

    /**
     * The URL of the Azure Key Vault that should be used to populate token
     * values.
     */
    private static final StringGuacamoleProperty VAULT_URL = new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "azure-keyvault-url";
        }

    };

    /**
     * The client ID that should be used to authenticate with Azure Key Vault
     * using ADAL.
     */
    private static final StringGuacamoleProperty CLIENT_ID = new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "azure-keyvault-client-id";
        }

    };

    /**
     * The client key that should be used to authenticate with Azure Key Vault
     * using ADAL.
     */
    private static final StringGuacamoleProperty CLIENT_KEY = new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "azure-keyvault-client-key";
        }

    };

    /**
     * Creates a new AzureKeyVaultConfigurationService which reads the token
     * mapping from "azure-keyvault-token-mapping.json". The token mapping is
     * a JSON file which lists each connection parameter token and the name of
     * the secret from which the value for that token should be read.
     */
    public AzureKeyVaultConfigurationService() {
        super(TOKEN_MAPPING_FILENAME);
    }

    /**
     * Returns the number of milliseconds that each retrieved secret should be
     * cached for. By default, secrets are cached for 10 seconds.
     *
     * @return
     *     The number of milliseconds to cache each retrieved secret.
     *
     * @throws GuacamoleException
     *     If the value specified within guacamole.properties cannot be
     *     parsed.
     */
    public int getSecretTTL() throws GuacamoleException {
        return environment.getProperty(SECRET_TTL, 10000);
    }

    /**
     * Returns the base URL of the Azure Key Vault containing the secrets that
     * should be retrieved to populate connection parameter tokens. The base
     * URL is specified with the "azure-keyvault-url" property.
     *
     * @return
     *     The base URL of the Azure Key Vault.
     *
     * @throws GuacamoleException
     *     If the base URL is not specified within guacamole.properties.
     */
    public String getVaultURL() throws GuacamoleException {
        return environment.getRequiredProperty(VAULT_URL);
    }

    /**
     * Returns the credentials that should be used to authenticate with Azure
     * Key Vault when retrieving secrets. Azure's "ADAL" authentication will be
     * used, requiring a client ID and key. These values are specified with the
     * "azure-keyvault-client-id" and "azure-keyvault-client-key" properties
     * respectively.
     *
     * @return
     *     The credentials that should be used to authenticate with Azure Key
     *     Vault.
     *
     * @throws GuacamoleException
     *     If the client ID or key are not specified within
     *     guacamole.properties.
     */
    public ClientCredential getClientCredentials() throws GuacamoleException {
        return new ClientCredential(
            environment.getRequiredProperty(CLIENT_ID),
            environment.getRequiredProperty(CLIENT_KEY)
        );
    }

}
