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

package org.apache.guacamole.vault.openbao.secret;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.authentication.AuthenticationSteps;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.AuthenticationSteps.HttpRequest;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LoginToken;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.VaultException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileTokenAuthentication implements ClientAuthentication {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(FileTokenAuthentication.class);

    /**
     * The path to the file containing the token
     */
    private final Path tokenPath;

    /**
     * The vault endpoint to lookup token metadata
     */
    private final VaultEndpoint endpoint;

    /**
     * A reusable restTemplate
     */
    private final RestTemplate restTemplate;

    /**
     * An instantiator for a Token Authentication class where the token
     * is reread from a file on renewal requests. This allows integration
     * with a VaultAgent for complex authentication methods
     *
     * @param String tokenPath
     *     A path to a readable file containing the token
     *
     * @param VaultEndpoint endpoint
     *     The Vault endpoint used for token metadata lookup
     */
    public FileTokenAuthentication(String tokenPath, VaultEndpoint endpoint, RestTemplate restTemplate) {
        this.tokenPath = Path.of(tokenPath);
        this.endpoint = endpoint;
        this.restTemplate = restTemplate;
    }

    /*
     * Returns the current token
     *
     * @return VaultToken
     *      The current vault token
     */
    @Override
    public VaultToken login() {
       String token;
        try {
            token = Files.readString(tokenPath).trim();
        }
        catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read Vault token sink: " + tokenPath, e);
        }

        String lookupPath = String.format(
                "%s://%s:%d/v1/auth/token/lookup-self",
                endpoint.getScheme(),
                endpoint.getHost(),
                endpoint.getPort());

        ResponseEntity<VaultResponse> response =
                restTemplate.postForEntity(lookupPath, null, VaultResponse.class);

        VaultResponse vaultResponse = response.getBody();

        Boolean renewable = (Boolean) vaultResponse.getAuth().get("renewable");
        Duration leaseDuration = Duration.ofSeconds((long) vaultResponse.getAuth().get("lease_duration"));
        String accessor = (String) vaultResponse.getAuth().get("accessor");
        String type = (String) vaultResponse.getAuth().get("type");

        if (token == null) {
            throw new VaultException("No client_token in Vault auth response");
        }

        return  LoginToken.builder().token(token)
                .leaseDuration(leaseDuration)
                .renewable(renewable)
                .accessor(accessor)
                .type(type)
                .build();
    }
}
