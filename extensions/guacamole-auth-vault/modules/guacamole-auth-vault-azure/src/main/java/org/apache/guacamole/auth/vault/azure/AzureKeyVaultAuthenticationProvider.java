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

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.vault.VaultAuthenticationProvider;

/**
 * VaultAuthenticationProvider implementation which reads secrets from Azure
 * Key Vault.
 */
public class AzureKeyVaultAuthenticationProvider extends VaultAuthenticationProvider {

    /**
     * Creates a new AzureKeyVaultAuthenticationProvider which reads secrets
     * from a configured Azure Key Vault.
     *
     * @throws GuacamoleException
     *     If configuration details cannot be read from guacamole.properties.
     */
    public AzureKeyVaultAuthenticationProvider() throws GuacamoleException {
        super(new AzureKeyVaultAuthenticationProviderModule());
    }

    @Override
    public String getIdentifier() {
        return "azure-keyvault";
    }

}
