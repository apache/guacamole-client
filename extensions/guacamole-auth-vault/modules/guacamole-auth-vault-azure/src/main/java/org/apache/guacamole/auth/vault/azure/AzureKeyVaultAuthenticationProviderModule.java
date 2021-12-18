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

package org.apache.guacamole.auth.vault.azure;

import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.vault.VaultAuthenticationProviderModule;
import org.apache.guacamole.auth.vault.azure.conf.AzureKeyVaultConfigurationService;
import org.apache.guacamole.auth.vault.azure.conf.AzureKeyVaultCredentials;
import org.apache.guacamole.auth.vault.azure.secret.AzureKeyVaultSecretService;
import org.apache.guacamole.auth.vault.conf.VaultConfigurationService;
import org.apache.guacamole.auth.vault.secret.VaultSecretService;

/**
 * Guice module which configures injections specific to Azure Key Vault
 * support.
 */
public class AzureKeyVaultAuthenticationProviderModule
        extends VaultAuthenticationProviderModule {

    /**
     * Creates a new AzureKeyVaultAuthenticationiProviderModule which
     * configures dependency injection for the Azure Key Vault authentication
     * provider and related services.
     *
     * @throws GuacamoleException
     *     If configuration details in guacamole.properties cannot be parsed.
     */
    public AzureKeyVaultAuthenticationProviderModule() throws GuacamoleException {}

    @Override
    protected void configureVault() {

        // Bind services specific to Azure Key Vault
        bind(VaultConfigurationService.class).to(AzureKeyVaultConfigurationService.class);
        bind(VaultSecretService.class).to(AzureKeyVaultSecretService.class);

        // Bind ADAL credentials implementation required for authenticating
        // against Azure
        bind(KeyVaultCredentials.class).to(AzureKeyVaultCredentials.class);

    }

}
