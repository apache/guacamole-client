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
 
// This is a minimal backport of this class to version 2.3.4 of spring-vault-core
// It is compatible with lease renewal using SecretLeaseContainer

package org.apache.guacamole.vault.openbao.secret;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.VaultException;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.RestTemplate;

public class UsernamePasswordAuthentication implements ClientAuthentication {

    private final UsernamePasswordAuthenticationOptions options;
    private final RestTemplate restTemplate;
    private final VaultEndpoint endpoint;

    public UsernamePasswordAuthentication(
            UsernamePasswordAuthenticationOptions options,
            VaultEndpoint endpoint,
            RestTemplate restTemplate) {

        Assert.notNull(options, "Options must not be null");
        Assert.notNull(endpoint, "VaultEndpoint must not be null");
        Assert.notNull(restTemplate, "RestTemplate must not be null");

        this.options = options;
        this.endpoint = endpoint;
        this.restTemplate = restTemplate;
    }

    @Override
    public VaultToken login() throws VaultException {

        String loginPath = String.format(
                "%s://%s:%d/v1/auth/%s/login/%s",
                endpoint.getScheme(),
                endpoint.getHost(),
                endpoint.getPort(),
                options.getMountPath(),
                options.getUsername());

        Map<String, Object> body =
                Collections.singletonMap("password", options.getPassword());

        ResponseEntity<VaultResponse> response =
                restTemplate.postForEntity(loginPath, body, VaultResponse.class);

        VaultResponse vaultResponse = response.getBody();

        if (vaultResponse == null || vaultResponse.getAuth() == null) {
            throw new VaultException("No auth section returned from Vault");
        }

        String token = (String) vaultResponse.getAuth().get("client_token");

        if (token == null) {
            throw new VaultException("No client_token in Vault auth response");
        }

        return VaultToken.of(token);
    }
}
