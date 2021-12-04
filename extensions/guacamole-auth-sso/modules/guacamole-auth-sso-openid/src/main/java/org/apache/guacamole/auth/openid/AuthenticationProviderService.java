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

package org.apache.guacamole.auth.openid;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.auth.openid.conf.ConfigurationService;
import org.apache.guacamole.auth.openid.form.TokenField;
import org.apache.guacamole.auth.openid.token.NonceService;
import org.apache.guacamole.auth.openid.token.TokenValidationService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.sso.SSOAuthenticationProviderService;
import org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.language.TranslatableMessage;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.jose4j.jwt.JwtClaims;

/**
 * Service that authenticates Guacamole users by processing OpenID tokens.
 */
public class AuthenticationProviderService implements SSOAuthenticationProviderService {

    /**
     * Service for retrieving OpenID configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for validating and generating unique nonce values.
     */
    @Inject
    private NonceService nonceService;

    /**
     * Service for validating received ID tokens.
     */
    @Inject
    private TokenValidationService tokenService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<SSOAuthenticatedUser> authenticatedUserProvider;

    @Override
    public SSOAuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        String username = null;
        Set<String> groups = null;

        // Validate OpenID token in request, if present, and derive username
        HttpServletRequest request = credentials.getRequest();
        if (request != null) {
            String token = request.getParameter(TokenField.PARAMETER_NAME);
            if (token != null) {
                JwtClaims claims = tokenService.validateToken(token);
                if (claims != null) {
                    username = tokenService.processUsername(claims);
                    groups = tokenService.processGroups(claims);
                }
            }
        }

        // If the username was successfully retrieved from the token, produce
        // authenticated user
        if (username != null) {

            // Create corresponding authenticated user
            SSOAuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(username, credentials, groups, Collections.emptyMap());
            return authenticatedUser;

        }

        // Request OpenID token
        throw new GuacamoleInvalidCredentialsException("Invalid login.",
            new CredentialsInfo(Arrays.asList(new Field[] {

                // OpenID-specific token (will automatically redirect the user
                // to the authorization page via JavaScript)
                new TokenField(
                    confService.getAuthorizationEndpoint(),
                    confService.getScope(),
                    confService.getClientID(),
                    confService.getRedirectURI(),
                    nonceService.generate(confService.getMaxNonceValidity() * 60000L),
                    new TranslatableMessage("LOGIN.INFO_IDP_REDIRECT_PENDING")
                )

            }))
        );

    }

    @Override
    public void shutdown() {
        // Nothing to clean up
    }
    
}
