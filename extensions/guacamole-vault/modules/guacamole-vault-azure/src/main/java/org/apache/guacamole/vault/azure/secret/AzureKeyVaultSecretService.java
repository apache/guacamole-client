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

package org.apache.guacamole.vault.azure.secret;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.rest.ServiceCallback;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.azure.conf.AzureKeyVaultAuthenticationException;
import org.apache.guacamole.vault.azure.conf.AzureKeyVaultConfigurationService;
import org.apache.guacamole.vault.secret.CachedVaultSecretService;

/**
 * Service which retrieves secrets from Azure Key Vault.
 */
@Singleton
public class AzureKeyVaultSecretService extends CachedVaultSecretService {

    /**
     * Pattern which matches contiguous groups of characters which are not
     * allowed within Azure Key Vault secret names.
     */
    private static final Pattern DISALLOWED_CHARACTERS = Pattern.compile("[^a-zA-Z0-9-]+");

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private AzureKeyVaultConfigurationService confService;

    /**
     * Provider for Azure Key Vault credentials.
     */
    @Inject
    private Provider<KeyVaultCredentials> credentialProvider;

    /**
     * {@inheritDoc}
     *
     * <p>Azure Key Vault allows strictly a-z, A-Z, 0-9, and "-". This
     * implementation strips out all contiguous groups of characters which are
     * not allowed by Azure Key Vault, replacing them with a single dash.
     */
    @Override
    public String canonicalize(String nameComponent) {
        Matcher disallowed = DISALLOWED_CHARACTERS.matcher(nameComponent);
        return disallowed.replaceAll("-");
    }

    @Override
    protected CachedSecret refreshCachedSecret(String name)
            throws GuacamoleException {

        int ttl = confService.getSecretTTL();
        String url = confService.getVaultURL();

        CompletableFuture<String> retrievedValue = new CompletableFuture<>();

        // getSecretAsync() still blocks for around half a second, despite
        // technically being asynchronous
        (new Thread() {

            @Override
            public void run() {
                try {

                    // Retrieve requested secret from Azure Key Vault
                    KeyVaultClient client = new KeyVaultClient(credentialProvider.get());
                    client.getSecretAsync(url, name, new ServiceCallback<SecretBundle>() {

                        @Override
                        public void failure(Throwable t) {
                            retrievedValue.completeExceptionally(t);
                        }

                        @Override
                        public void success(SecretBundle secret) {
                            String value = (secret != null) ? secret.value() : null;
                            retrievedValue.complete(value);
                        }

                    });

                }
                catch (AzureKeyVaultAuthenticationException e) {
                    retrievedValue.completeExceptionally(e);
                }
            }

        }).start();

        // Cache retrieved value
        return new CachedSecret(retrievedValue, ttl);

    }

    @Override
    public Map<String, Future<String>> getTokens(GuacamoleConfiguration config,
            TokenFilter filter) throws GuacamoleException {
        return Collections.emptyMap();
    }

}
