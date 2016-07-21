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

package org.apache.guacamole.auth.jdbc.user;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.sharing.ConnectionSharingService;
import org.apache.guacamole.auth.jdbc.sharing.SharedConnectionUser;
import org.apache.guacamole.auth.jdbc.sharing.SharedConnectionUserContext;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Service which authenticates users based on credentials and provides for
 * the creation of corresponding, new UserContext objects for authenticated
 * users.
 *
 * @author Michael Jumper
 */
public class AuthenticationProviderService  {

    /**
     * Service for accessing users.
     */
    @Inject
    private UserService userService;

    /**
     * Provider for retrieving UserContext instances.
     */
    @Inject
    private Provider<UserContext> userContextProvider;

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

    /**
     * Authenticates the user having the given credentials, returning a new
     * AuthenticatedUser instance only if the credentials are valid. If the
     * credentials are invalid or expired, an appropriate GuacamoleException
     * will be thrown.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the user is being
     *     authenticated.
     *
     * @param credentials
     *     The credentials to use to produce the AuthenticatedUser.
     *
     * @return
     *     A new AuthenticatedUser instance for the user identified by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs during authentication, or if the given
     *     credentials are invalid or expired.
     */
    public AuthenticatedUser authenticateUser(AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException {

        AuthenticatedUser user;

        // Check whether user is authenticating with a valid sharing key
        user = sharingService.retrieveSharedConnectionUser(authenticationProvider, credentials);
        if (user != null)
            return user;

        // Authenticate user
        user = userService.retrieveAuthenticatedUser(authenticationProvider, credentials);
        if (user != null)
            return user;

        // Otherwise, unauthorized
        throw new GuacamoleInvalidCredentialsException("Invalid login", CredentialsInfo.USERNAME_PASSWORD);

    }

    /**
     * Returning a new UserContext instance for the given already-authenticated
     * user. A new placeholder account will be created for any user that does
     * not already exist within the database.
     *
     * @param authenticatedUser
     *     The credentials to use to produce the UserContext.
     *
     * @return
     *     A new UserContext instance for the user identified by the given
     *     credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs during authentication, or if the given
     *     credentials are invalid or expired.
     */
    public org.apache.guacamole.net.auth.UserContext getUserContext(
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // Produce sharing-specific user context if this is the user of a shared connection
        if (authenticatedUser instanceof SharedConnectionUser) {
            SharedConnectionUserContext context = sharedUserContextProvider.get();
            context.init((SharedConnectionUser) authenticatedUser);
            return context;
        }

        // Retrieve user account for already-authenticated user
        ModeledUser user = userService.retrieveUser(authenticatedUser);
        if (user == null)
            return null;

        // Link to user context
        UserContext context = userContextProvider.get();
        context.init(user.getCurrentUser());
        return context;

    }

}
