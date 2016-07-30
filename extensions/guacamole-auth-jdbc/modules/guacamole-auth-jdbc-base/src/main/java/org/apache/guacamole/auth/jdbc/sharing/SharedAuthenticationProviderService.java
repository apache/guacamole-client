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

package org.apache.guacamole.auth.jdbc.sharing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.AuthenticationProviderService;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Service which authenticates users based on share keys and provides for the
 * creation of corresponding. The created UserContext objects are restricted to
 * the connections associated with those share keys via a common
 * ConnectionSharingService.
 *
 * @author Michael Jumper
 */
public class SharedAuthenticationProviderService implements AuthenticationProviderService {

    /**
     * Provider for retrieving SharedConnectionUserContext instances.
     */
    @Inject
    private Provider<SharedConnectionUserContext> sharedUserContextProvider;

    /**
     * Service for sharing active connections.
     */
    @Inject
    private ConnectionSharingService sharingService;

    @Override
    public AuthenticatedUser authenticateUser(AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException {

        // Check whether user is authenticating with a valid sharing key
        AuthenticatedUser user = sharingService.retrieveSharedConnectionUser(authenticationProvider, credentials);
        if (user != null)
            return user;

        // Otherwise, unauthorized
        throw new GuacamoleInvalidCredentialsException("Invalid login", CredentialsInfo.USERNAME_PASSWORD);

    }

    @Override
    public org.apache.guacamole.net.auth.UserContext getUserContext(
            AuthenticationProvider authenticationProvider,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // Produce sharing-specific user context if this is the user of a shared connection
        if (authenticatedUser instanceof SharedConnectionUser) {
            SharedConnectionUserContext context = sharedUserContextProvider.get();
            context.init((SharedConnectionUser) authenticatedUser);
            return context;
        }

        // No shared connections otherwise
        return null;

    }

}
