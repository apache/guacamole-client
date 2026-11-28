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

package org.apache.guacamole.auth.saml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.saml.acs.AssertedIdentity;
import org.apache.guacamole.auth.saml.acs.SAMLAuthenticationSessionManager;
import org.apache.guacamole.auth.saml.acs.SAMLService;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.apache.guacamole.auth.saml.conf.SAMLConfiguration;
import org.apache.guacamole.auth.saml.user.SAMLAuthenticatedUser;
import org.apache.guacamole.auth.sso.SSOAuthenticationProviderService;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableMessage;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Service that authenticates Guacamole users by processing the responses of
 * one or more SAML identity providers.
 */
@Singleton
public class AuthenticationProviderService implements SSOAuthenticationProviderService {

    /**
     * The name of the query parameter that identifies an active authentication
     * session (in-progress SAML authentication attempt).
     */
    public static final String AUTH_SESSION_QUERY_PARAM = "state";

    /**
     * The name of the query parameter included in multi-IdP callback redirects
     * indicating which SAML identity provider authenticated the user, so that
     * single logout can be routed to the same identity provider.
     */
    public static final String AUTH_IDP_QUERY_PARAM = "idp";

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<SAMLAuthenticatedUser> authenticatedUserProvider;

    /**
     * Manager of active SAML authentication attempts.
     */
    @Inject
    private SAMLAuthenticationSessionManager sessionManager;

    /**
     * Service for processing SAML requests/responses.
     */
    @Inject
    private SAMLService saml;

    /**
     * Service for retrieving SAML configuration information.
     */
    @Inject
    private ConfigurationService confService;

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

    /**
     * Builds the logout redirect URI for the given SAML identity provider
     * configuration, or returns null if no logout endpoint is configured for
     * that identity provider.
     *
     * @param config
     *     The SAML identity provider configuration.
     *
     * @return
     *     The logout redirect URI, or null if no logout endpoint is configured.
     *
     * @throws GuacamoleException
     *     If configuration information required to build the logout URI cannot
     *     be parsed.
     */
    private URI buildLogoutURI(SAMLConfiguration config) throws GuacamoleException {

        // If no logout endpoint is configured, return null
        URI logoutEndpoint = config.getLogoutEndpoint();
        if (logoutEndpoint == null)
            return null;

        // Build the logout URI with post-logout redirect
        UriBuilder logoutUriBuilder = UriBuilder.fromUri(logoutEndpoint);
        logoutUriBuilder.queryParam("RelayState",
                confService.getPostLogoutRedirectURI(config));

        return logoutUriBuilder.build();

    }

    @Override
    public SAMLAuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Use established SAML identity if already provided by a SAML IdP
        AssertedIdentity identity = sessionManager.getIdentity(
                getSessionIdentifier(credentials));

        if (identity != null) {
            SAMLAuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(identity, credentials);
            return authenticatedUser;
        }

        // Automatically redirect only if a default SAML IdP is configured
        // (single-IdP configuration or multi-IdP with an explicit default).
        // When multiple IdPs are configured with no default, return null so
        // the login UI presents all available SAML IdP choices.
        SAMLConfiguration defaultConfig = confService.getDefaultConfiguration();
        if (defaultConfig == null)
            return null;

        // Redirect to default SAML IdP if no SAML identity is associated with
        // the Guacamole authentication request
        throw new GuacamoleInvalidCredentialsException("Redirecting to SAML IdP.",
                new CredentialsInfo(Arrays.asList(new Field[] {
                    new RedirectField(AUTH_SESSION_QUERY_PARAM, saml.createRequest(defaultConfig),
                            new TranslatableMessage("LOGIN.INFO_IDP_REDIRECT_PENDING"))
                }))
        );

    }

    @Override
    public URI getLoginURI() throws GuacamoleException {
        return saml.createRequest();
    }

    /**
     * Returns the full URI of the SAML SSO login endpoint for the specified
     * identity provider.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @return
     *     The full URI of the specified SAML identity provider's SSO endpoint.
     *
     * @throws GuacamoleException
     *     If no identity provider with the given ID is configured, or if the
     *     login URI cannot be generated.
     */
    public URI getLoginURI(String idpId) throws GuacamoleException {
        return saml.createRequest(idpId);
    }

    @Override
    public URI getLogoutURI(String idToken) throws GuacamoleException {

        SAMLConfiguration defaultConfig = confService.getDefaultConfiguration();
        if (defaultConfig == null)
            return null;

        return buildLogoutURI(defaultConfig);

    }

    /**
     * Returns the full URI of the SAML logout endpoint for the specified
     * identity provider, or null if no logout endpoint is configured for that
     * identity provider.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @param idToken
     *     Optional ID token (unused by SAML).
     *
     * @return
     *     The logout redirect URI for the specified identity provider, or null
     *     if no logout endpoint is configured.
     *
     * @throws GuacamoleException
     *     If no identity provider with the given ID is configured, or if
     *     configuration cannot be parsed.
     */
    public URI getLogoutURI(String idpId, String idToken) throws GuacamoleException {
        return buildLogoutURI(confService.getRequiredConfiguration(idpId));
    }

    @Override
    public void shutdown() {
        sessionManager.shutdown();
    }

}
