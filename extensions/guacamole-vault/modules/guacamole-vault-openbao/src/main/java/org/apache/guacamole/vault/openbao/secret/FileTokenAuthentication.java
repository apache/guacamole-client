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
import org.springframework.vault.authentication.AuthenticationSteps;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.AuthenticationSteps.HttpRequest;
import static org.springframework.vault.authentication.AuthenticationSteps.HttpRequestBuilder.get;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LoginToken;
import org.springframework.vault.client.VaultHttpHeaders;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileTokenAuthentication implements ClientAuthentication, AuthenticationStepsFactory {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(FileTokenAuthentication.class);

    /**
     * The path to the file containing the token
     */
    private final Path tokenPath;

    /**
     * An instantiator for a Token Authentication class where the token
     * is reread from a file on renewal requests. This allows integration
     * with a VaultAgent for complex authentication methods
     *
     * @param String tokenPath
     *     A path to a readable file containing the token
     */
    public FileTokenAuthentication(String tokenPath) {
        this.tokenPath = Path.of(tokenPath);
    }

    /*
     * Returns the current token
     *
     * @return VaultToken
     *      The current vault token
     */
    @Override
    public VaultToken login() {
        try {
            String token = Files.readString(tokenPath).trim();
            return VaultToken.of(token);
        }
        catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read Vault token sink: " + tokenPath, e);
        }
    }
    
	  /**
	   * Create 'AuthenticationSteps' for token authentication given a VaultToken
	   *
	   * @param VaultToken token
	   *     token must not be null.
	   *
	   * @param boolean selfLookup 
	   *     Set true to perform a self-lookup using the given VaultToken.
	   *     Self-lookup will create a LoginToken and provide renewability
	   *     and TTL
	   * 
	   * @return AuthenticationSteps
	   *     The AuthenticationSteps for token authentication.
	   */
	  public static AuthenticationSteps createAuthenticationSteps(VaultToken token, boolean selfLookup) {
	      if (token == null) {
	          throw new IllegalStateException("VaultToken must not be null");
	      }

        if (selfLookup) {
            HttpRequest<VaultResponse> httpRequest = get("auth/token/lookup-self").with(VaultHttpHeaders.from(token))
                .as(VaultResponse.class);

            return AuthenticationSteps.fromHttpRequest(httpRequest)
                .login(response -> LoginTokenFrom(token.toCharArray(), response.getRequiredData()));
        }

        return AuthenticationSteps.just(token);
	  }
    
    /**
     * Reimplementation of LoginTokenUtil.from as it is private
     *
     * @param char[] token
     *      The token converted to a char[]
     *
     * @param Map<String, ?> auth
     *     A map of the authentication response.
     *
     * @return LoginToken
     */
    private static LoginToken LoginTokenFrom(char[] token, Map<String, ?> auth ) {
	      if (auth == null) {
	          throw new IllegalStateException("VaultToken must not be null");
	      }    

        Boolean renewable = (Boolean) auth.get("renewable");
        Number leaseDuration = (Number) auth.get("lease_duration");
        String accessor = (String) auth.get("accessor");
        String type = (String) auth.get("type");

        if (leaseDuration == null) {
	          leaseDuration = (Number) auth.get("ttl");
        }

        if (type == null) {
	          type = (String) auth.get("token_type");
        }

        LoginToken.LoginTokenBuilder builder = LoginToken.builder();
        builder.token(token);

        if (accessor != null && !accessor.trim().isEmpty()) {
	          builder.accessor(accessor);
        }

        if (leaseDuration != null) {
	          builder.leaseDuration(Duration.ofSeconds(leaseDuration.longValue()));
        }

        if (renewable != null) {
	          builder.renewable(renewable);
        }

        if (type != null && !type.trim().isEmpty()) {
	          builder.type(type);
        }

        return builder.build();
    }
    
    
	  @Override
	  public AuthenticationSteps getAuthenticationSteps() {
	      VaultToken token = login();
		    return createAuthenticationSteps(token, false);
	  }
}
