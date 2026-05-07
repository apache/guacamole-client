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

package org.apache.guacamole.vault.openbao.vault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.authentication.AuthenticationSteps;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.AuthenticationSteps.HttpRequest;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.VaultException;
import org.springframework.web.client.RestTemplate;

public final class FileTokenAuthentication implements ClientAuthentication {

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
            // This might be recoverable. So throw a VautException
            throw new VaultException(
                    "Cannot read Vault token sink: " + tokenPath, e);
        }

        return VaultToken.of(token);
    }
}
