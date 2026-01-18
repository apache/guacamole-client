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

package org.apache.guacamole.auth.sso;

import java.net.URI;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;

/**
 * Service that authenticates Guacamole users by leveraging an arbitrary SSO
 * service.
 */
public interface SSOAuthenticationProviderService {

    /**
     * Returns an SSOAuthenticatedUser representing the user authenticated by
     * the given credentials. Tokens associated with the returned
     * SSOAuthenticatedUser will automatically be injected into any connections
     * used by that user during their session.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An SSOAuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    SSOAuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException;

    /**
     * Returns the full URI of the login endpoint to which a user must be
     * redirected in order to authenticate with the SSO identity provider.
     *
     * @return 
     *     The full URI of the SSO login endpoint.
     *
     * @throws GuacamoleException
     *     If configuration information required for generating the login URI
     *     cannot be read.
     */
    URI getLoginURI() throws GuacamoleException;

    /**
     * Returns the full URI of the logout endpoint to which a user should be
     * redirected when they log out from Guacamole. This allows the user to
     * also log out from the SSO identity provider. If no logout endpoint is
     * configured, null is returned.
     *
     * @param idToken
     *     The ID token or authentication token from the user's session, if
     *     available. This may be used as an id_token_hint parameter in the
     *     logout request. May be null if not available.
     *
     * @return
     *     The full URI of the SSO logout endpoint, or null if not configured.
     *
     * @throws GuacamoleException
     *     If configuration information required for generating the logout URI
     *     cannot be read.
     */
    default URI getLogoutURI(String idToken) throws GuacamoleException {
        return null;
    }

    /**
     * Frees all resources associated with the relevant
     * SSOAuthenticationProvider implementation. This function is automatically
     * invoked when an implementation of SSOAuthenticationProvider is shut
     * down.
     */
    void shutdown();

}
