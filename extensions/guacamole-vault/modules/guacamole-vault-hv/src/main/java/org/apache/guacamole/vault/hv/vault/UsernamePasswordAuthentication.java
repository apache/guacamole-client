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

package org.apache.guacamole.vault.hv.vault;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LoginToken;
import org.springframework.vault.VaultException;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestTemplate;

/**
 * This is a minimal backport of this class to version 2.3.4 of spring-vault-core
 * It is compatible with lease renewal using a life cycle aware session manager
 */
public class UsernamePasswordAuthentication implements ClientAuthentication {
    /**
     * A class containing all of the configuration options for 
     * username/password authentication
     */
    private final UsernamePasswordAuthenticationOptions options;

    /**
     * A spring RestTemplate used to communicate with the vault server
     */
    private final RestTemplate restTemplate;

    /**
     * The Vault endpoint to communicate to
     */
    private final VaultEndpoint endpoint;

    /**
     * Contructor for the Username/Password clientAuthentication
     *
     * @param options
     *     The configuration of the username/password to use for authentication
     *
     * @param endpoint
     *     The endpoint of teh VAult to use
     *
     * @param restTemplate
     *     The spring-framework RestTemplate used to communicate with the Vault
     */
    public UsernamePasswordAuthentication(
            final UsernamePasswordAuthenticationOptions options,
            final VaultEndpoint endpoint,
            final RestTemplate restTemplate) {

        Assert.notNull(options, "Options must not be null");
        Assert.notNull(endpoint, "VaultEndpoint must not be null");
        Assert.notNull(restTemplate, "RestTemplate must not be null");

        this.options = options;
        this.endpoint = endpoint;
        this.restTemplate = restTemplate;
    }

    /**
     * A login method that attempts a username/password login to the Vault
     *
     * @return
     *      A LoginToken for the authenticated used for use with future operations with
     *      the vault
     *
     * @throws VaultException
     *      In case of a recoverable login issue, throws a VaultException, so that the
     *      SessionManager knows to try the authentication again
     */
    @Override
    public VaultToken login() throws VaultException {

        final String loginPath = String.format(
                "%s://%s:%d/v1/auth/%s/login/%s",
                endpoint.getScheme(),
                endpoint.getHost(),
                endpoint.getPort(),
                options.getMountPath(),
                options.getUsername());

        final Map<String, Object> body =
                Collections.singletonMap("password", options.getPassword());

        final ResponseEntity<VaultResponse> response =
                restTemplate.postForEntity(loginPath, body, VaultResponse.class);

        final VaultResponse vaultResponse = response.getBody();

        if (vaultResponse == null || vaultResponse.getAuth() == null) {
            throw new VaultException("No auth section returned from Vault");
        }

        final String token = (String) vaultResponse.getAuth().get("client_token");
        if (token == null) {
            throw new VaultException("No client_token in Vault auth response");
        }

        final Number lease = (Number) vaultResponse.getAuth().get("lease_duration");
        final Boolean renewable = (Boolean) vaultResponse.getAuth().get("renewable");
        final long leaseDuration = lease != null ? lease.longValue() : 0;
        final boolean isRenewable = renewable != null && renewable;
        return  LoginToken.builder().token(token)
                .leaseDuration(Duration.ofSeconds(leaseDuration))
                .renewable(isRenewable)
                .build();
    }
}
