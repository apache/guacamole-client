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

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Guacamole authentication backend which authenticates users using an
 * arbitrary external system implementing OAuth. No storage for connections is
 * provided - only authentication. Storage must be provided by some other
 * extension.
 */
public class OAuthAuthenticationProvider implements AuthenticationProvider {

    @Override
    public String getIdentifier() {
        return "oauth";
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // STUB
        throw new GuacamoleInvalidCredentialsException(
            "Invalid login.",
            CredentialsInfo.USERNAME_PASSWORD
        );

    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        // No update necessary
        return authenticatedUser;

    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // No associated data whatsoever
        return null;

    }

    @Override
    public UserContext updateUserContext(UserContext context,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // No update necessary
        return context;

    }

}
