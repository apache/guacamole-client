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

package org.apache.guacamole.auth.oauth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.glyptodon.guacamole.GuacamoleException;
import org.apache.guacamole.auth.oauth.conf.ConfigurationService;
import org.apache.guacamole.auth.oauth.form.OAuthCodeField;
import org.apache.guacamole.auth.oauth.user.AuthenticatedUser;
import org.glyptodon.guacamole.form.Field;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing convenience functions for the OAuth AuthenticationProvider
 * implementation.
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

    /**
     * Service for retrieving OAuth configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<AuthenticatedUser> authenticatedUserProvider;

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An AuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        String code = null;

        // Pull OAuth code from request if present
        HttpServletRequest request = credentials.getRequest();
        if (request != null)
            code = request.getParameter(OAuthCodeField.PARAMETER_NAME);

        // TODO: Actually complete authentication using received code
        if (code != null) {
            AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
            authenticatedUser.init("STUB", credentials);
            return authenticatedUser;
        }

        // Request auth code
        throw new GuacamoleInvalidCredentialsException("Invalid login.",
            new CredentialsInfo(Arrays.asList(new Field[] {

                // Normal username/password fields
                CredentialsInfo.USERNAME,
                CredentialsInfo.PASSWORD,

                // OAuth-specific code (will be rendered as an appropriate
                // "Log in with..." button
                new OAuthCodeField(
                    confService.getAuthorizationEndpoint(),
                    confService.getClientID(),
                    confService.getRedirectURI()
                )

            }))
        );

    }

}
