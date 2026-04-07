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
import com.google.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.auth.openid.conf.ConfigurationService;
import org.apache.guacamole.auth.openid.OpenIDAuthenticationSessionManager;
import org.apache.guacamole.auth.openid.OpenIDRedirectResource;
import org.apache.guacamole.auth.openid.token.TokenValidationService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.sso.NonceService;
import org.apache.guacamole.auth.sso.SSOAuthenticationProviderService;
import org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableMessage;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.jose4j.jwt.JwtClaims;

/**
 * Service that authenticates Guacamole users by processing OpenID tokens.
 */
@Singleton
public class AuthenticationProviderService implements SSOAuthenticationProviderService {

    /**
     * The standard HTTP parameter which will be included within the URL by all
     * OpenID services upon successful implicit flow authentication.
     */
    public static final String IMPLICIT_TOKEN_PARAMETER_NAME = "id_token";

    /**
     * The standard HTTP parameter which will be included within the URL by all
     * OpenID services upon successful code flow authentication. Used to recover
     * the stored user state.
     */
    public static final String CODE_TOKEN_PARAMETER_NAME = "code";
    
    /**
     * The name of the query parameter that identifies an active authentication
     * session (in-progress OpenID authentication attempt).
     */
    public static final String AUTH_SESSION_QUERY_PARAM = "state";

    /**
     * The REST API used for a local rediect and callback for the IdP to
     * return a code and access stored PKCE verifiers
     */
    public static final String CODE_REDIRECT_API = "/api/ext/openid/redirect";

    /**
     * Service for retrieving OpenID configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Manager of active SAML authentication attempts.
     */
    @Inject
    private OpenIDAuthenticationSessionManager sessionManager;

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
        if (isImplicit()) {
            return authenticateUserImplicit(credentials);
        } else {
            return authenticateUserCode(credentials);
        }
    }

    private SSOAuthenticatedUser authenticateUserImplicit(Credentials credentials)
            throws GuacamoleException {

        String username = null;
        Set<String> groups = null;
        Map<String,String> tokens = Collections.emptyMap();

        // Validate OpenID token in request, if present, and derive username
        String token = credentials.getParameter(IMPLICIT_TOKEN_PARAMETER_NAME);
        if (token != null) {
            JwtClaims claims = tokenService.validateTokenImplicit(token);
            if (claims != null) {
                username = tokenService.processUsername(claims);
                groups = tokenService.processGroups(claims);
                tokens = tokenService.processAttributes(claims);
            }
        }

        // If the username was successfully retrieved from the token, produce
        // authenticated user
        if (username != null) {

            // Create corresponding authenticated user
            SSOAuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(username, credentials, groups, tokens);
            return authenticatedUser;

        }

        // Request OpenID token (will automatically redirect the user to the
        // OpenID authorization page via JavaScript)
        throw new GuacamoleInvalidCredentialsException("Invalid login.",
            new CredentialsInfo(Arrays.asList(new Field[] {
                new RedirectField(IMPLICIT_TOKEN_PARAMETER_NAME, getLoginURI(),
                        new TranslatableMessage("LOGIN.INFO_IDP_REDIRECT_PENDING"))
            }))
        );

    }

    /**
     * Return the value of the session identifier associated with the given
     * credentials, or null if no session identifier is found in the
     * credentials.
     *
     * @param credentials
     *     The credentials from which to extract the session identifier.
     *
     * @return
     *     The session identifier associated with the given credentials, or
     *     null if no identifier is found.
     */
    public static String getSessionIdentifier(Credentials credentials) {

        // Return the session identifier from the request params, if set, or
        // null otherwise
        return credentials != null ? credentials.getParameter(AUTH_SESSION_QUERY_PARAM) : null;
    }

    private SSOAuthenticatedUser authenticateUserCode(Credentials credentials)
            throws GuacamoleException {

        String username = null;
        Set<String> groups = null;
        Map<String,String> tokens = Collections.emptyMap();

        // Recover session
        String identifier = getSessionIdentifier(credentials);
        OpenIDAuthenticationSession session = sessionManager.resume(identifier);
        
        // Validate OIDC token in request, if present, and derive username
        if (session != null) {
            String code = (String) session.getCode();
            if (code != null) {
                JwtClaims claims = tokenService.validateTokenCode(code, session);
                if (claims != null) {
                    username = tokenService.processUsername(claims);
                    groups = tokenService.processGroups(claims);
                    tokens = tokenService.processAttributes(claims);
                }
            }
        }

        // If the username was successfully retrieved from the token, produce
        // authenticated user
        if (username != null) {
            // Create corresponding authenticated user
            SSOAuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(username, credentials, groups, tokens);
            return authenticatedUser;

        }

        // Request OpenID token (will automatically redirect the user to the
        // OpenID authorization page via JavaScript)
        throw new GuacamoleInvalidCredentialsException("Invalid login.",
            new CredentialsInfo(Arrays.asList(new Field[] {
                new RedirectField(AUTH_SESSION_QUERY_PARAM, getLoginURI(),
                        new TranslatableMessage("LOGIN.INFO_IDP_REDIRECT_PENDING"))
            }))
        );
    }

    private Boolean isImplicit() throws GuacamoleException {
        String flowType = confService.getFlowType();
        return flowType.equals("implicit");
    }

    @Override
    public URI getLoginURI() throws GuacamoleException {
        if (isImplicit()) {
            return UriBuilder.fromUri(confService.getAuthorizationEndpoint())
                    .queryParam("scope", confService.getScope())
                    .queryParam("response_type", IMPLICIT_TOKEN_PARAMETER_NAME)
                    .queryParam("client_id", confService.getClientID())
                    .queryParam("redirect_uri", confService.getRedirectURI())
                    .queryParam("nonce", nonceService.generate(confService.getMaxNonceValidity() * 60000L))
                    .build();
        } else {
           /**
            * For Code flow and PKCE support we need access to the users session to
            * store the PKCE verifier and be able to pass the returned code form the
            * IdP to authenticateUser. Guacamole doesn't expose the user session here.
            * We get around that by creating a redirect to a local REST api that then
            * does the redirection/callback and stores the code and verifier in the
            * user session.
            */
            URI redirect = UriBuilder.fromUri(confService.getRedirectURI())
                         .path(CODE_REDIRECT_API).build();
            return UriBuilder.fromUri(redirect)
                    .queryParam("scope", confService.getScope())
                    .queryParam("response_type", CODE_TOKEN_PARAMETER_NAME)
                    .queryParam("client_id", confService.getClientID())
                    .queryParam("redirect_uri", redirect)
                    .build();
        }
    }

    @Override
    public URI getLogoutURI(String idToken) throws GuacamoleException {

        // If no logout endpoint is configured, return null
        URI logoutEndpoint = confService.getLogoutEndpoint();
        if (logoutEndpoint == null)
            return null;

        // Build the logout URI with appropriate parameters
        UriBuilder logoutUriBuilder = UriBuilder.fromUri(logoutEndpoint);

        // Add post_logout_redirect_uri parameter
        logoutUriBuilder.queryParam("post_logout_redirect_uri",
                confService.getPostLogoutRedirectURI());

        // Add id_token_hint if available, otherwise add client_id
        if (idToken != null && !idToken.isEmpty())
            logoutUriBuilder.queryParam("id_token_hint", idToken);
        else
            logoutUriBuilder.queryParam("client_id", confService.getClientID());

        return logoutUriBuilder.build();
    }

    @Override
    public void shutdown() {
        sessionManager.shutdown();
    }

}
