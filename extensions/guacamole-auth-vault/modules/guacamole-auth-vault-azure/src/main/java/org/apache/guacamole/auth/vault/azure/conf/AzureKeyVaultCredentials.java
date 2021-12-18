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

package org.apache.guacamole.auth.vault.azure.conf;

import com.google.inject.Inject;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.guacamole.GuacamoleException;

/**
 * KeyVaultCredentials implementation which retrieves the required client ID
 * and key from guacamole.properties. Note that KeyVaultCredentials as
 * implemented in the Azure Java SDK is NOT THREADSAFE; it leverages a
 * non-concurrent HashMap for authentication result caching and does not
 * perform any synchronization.
 */
public class AzureKeyVaultCredentials extends KeyVaultCredentials {

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private AzureKeyVaultConfigurationService confService;

    /**
     * {@inheritDoc}
     *
     * @throws AzureKeyVaultAuthenticationException
     *     If an error occurs preventing successful authentication. Note that
     *     this exception is unchecked. Uses of this class which need to be
     *     aware of errors in the authentication process must manually catch
     *     this exception.
     */
    @Override
    public String doAuthenticate(String authorization, String resource,
            String scope) throws AzureKeyVaultAuthenticationException {

        // Read Azure credentials from guacamole.properties
        ClientCredential credentials;
        try {
            credentials = confService.getClientCredentials();
        }
        catch (GuacamoleException e) {
            throw new AzureKeyVaultAuthenticationException("Azure "
                    + "credentials could not be read.", e);
        }

        ExecutorService service = Executors.newFixedThreadPool(1);
        try {

            // Attempt to aquire authentication token from Azure
            AuthenticationContext context = new AuthenticationContext(authorization, false, service);
            Future<AuthenticationResult> future = context.acquireToken(resource, credentials, null);

            // Wait for response
            AuthenticationResult result = future.get();

            // The semantics of a null return value are not documented, however
            // example code provided with the Azure Java SDK demonstrates that
            // a null check is required, albeit without explanation
            if (result == null)
                throw new AzureKeyVaultAuthenticationException(
                        "Authentication result from Azure was empty.");

            // Return authentication token from successful response
            return result.getAccessToken();

        }

        // Rethrow any errors which occur during the authentication process as
        // AzureKeyVaultAuthenticationExceptions
        catch (MalformedURLException e) {
            throw new AzureKeyVaultAuthenticationException("Azure "
                    + "authentication URL is malformed.", e);
        }
        catch (InterruptedException e) {
            throw new AzureKeyVaultAuthenticationException("Azure "
                    + "authentication process was interrupted.", e);
        }
        catch (ExecutionException e) {
            throw new AzureKeyVaultAuthenticationException("Authentication "
                    + "against Azure failed.", e);
        }

        finally {
            service.shutdown();
        }

    }

}
