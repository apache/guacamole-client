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
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.auth.saml.user.SAMLAuthenticatedUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.saml.acs.AssertedIdentity;
import org.apache.guacamole.auth.saml.acs.AuthenticationSessionManager;
import org.apache.guacamole.auth.saml.acs.SAMLService;
import org.apache.guacamole.auth.sso.SSOAuthenticationProviderService;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableMessage;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Service that authenticates Guacamole users by processing the responses of
 * SAML identity providers.
 */
@Singleton
public class AuthenticationProviderService implements SSOAuthenticationProviderService {

    /**
     * The name of the query parameter that identifies an active authentication
     * session (in-progress SAML authentication attempt).
     */
    public static final String AUTH_SESSION_QUERY_PARAM = "state";

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<SAMLAuthenticatedUser> authenticatedUserProvider;

    /**
     * Manager of active SAML authentication attempts.
     */
    @Inject
    private AuthenticationSessionManager sessionManager;

    /**
     * Service for processing SAML requests/responses.
     */
    @Inject
    private SAMLService saml;

    @Override
    public SAMLAuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // No authentication can be attempted without a corresponding HTTP
        // request
        HttpServletRequest request = credentials.getRequest();
        if (request == null)
            return null;

        // Use established SAML identity if already provided by the SAML IdP
        AssertedIdentity identity = sessionManager.getIdentity(request.getParameter(AUTH_SESSION_QUERY_PARAM));
        if (identity != null) {

            // Back-port the username to the credentials
            credentials.setUsername(identity.getUsername());

            // Configure the AuthenticatedUser and return it
            SAMLAuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init(identity, credentials);
            return authenticatedUser;

        }

        // Redirect to SAML IdP if no SAML identity is associated with the
        // Guacamole authentication request
        throw new GuacamoleInvalidCredentialsException("Redirecting to SAML IdP.",
                new CredentialsInfo(Arrays.asList(new Field[] {
                    new RedirectField(AUTH_SESSION_QUERY_PARAM, getLoginURI(),
                            new TranslatableMessage("LOGIN.INFO_IDP_REDIRECT_PENDING"))
                }))
        );

    }

    @Override
    public URI getLoginURI() throws GuacamoleException {
        return saml.createRequest();
    }

    @Override
    public void shutdown() {
        sessionManager.shutdown();
    }
    
}
