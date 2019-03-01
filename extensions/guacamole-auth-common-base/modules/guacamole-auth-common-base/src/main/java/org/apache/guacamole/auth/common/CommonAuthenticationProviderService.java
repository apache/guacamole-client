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

package org.apache.guacamole.auth.common;

import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.security.PasswordPolicyService;
import org.apache.guacamole.auth.common.sharing.user.SharedAuthenticatedUser;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.common.user.ModeledUserAbstract;
import org.apache.guacamole.auth.common.user.ModeledUserContextAbstract;
import org.apache.guacamole.auth.common.user.UserServiceInterface;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * AuthenticationProviderService implementation which authenticates users with a
 * username/password pair, producing new UserContext objects which are backed by
 * an underlying, arbitrary database.
 */
public class CommonAuthenticationProviderService
        implements AuthenticationProviderService {

    /**
     * The environment of the Guacamole server.
     */
    @Inject
    private CommonEnvironment environment;

    /**
     * Service for accessing users.
     */
    @Inject
    private UserServiceInterface userService;

    /**
     * Service for enforcing password complexity policies.
     */
    @Inject
    private PasswordPolicyService passwordPolicyService;

    /**
     * Provider for retrieving UserContext instances.
     */
    @Inject
    private Provider<ModeledUserContextAbstract> userContextProvider;

    @Override
    public AuthenticatedUser authenticateUser(
            AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException {

        // Authenticate user
        AuthenticatedUser user = userService
                .retrieveAuthenticatedUser(authenticationProvider, credentials);
        if (user != null)
            return user;

        // Otherwise, unauthorized
        throw new GuacamoleInvalidCredentialsException("Invalid login",
                CredentialsInfo.USERNAME_PASSWORD);

    }

    @Override
    public ModeledUserContextAbstract getUserContext(
            AuthenticationProvider authenticationProvider,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // Always allow but provide no data for users authenticated via our own
        // connection sharing links
        if (authenticatedUser instanceof SharedAuthenticatedUser)
            return null;

        // Set semantic flags based on context
        boolean databaseCredentialsUsed = (authenticatedUser instanceof ModeledAuthenticatedUser);
        boolean databaseRestrictionsApplicable = (databaseCredentialsUsed
                || environment.isUserRequired());

        // Retrieve user account for already-authenticated user
        ModeledUserAbstract user = userService
                .retrieveUser(authenticationProvider, authenticatedUser);
        if (user != null && !user.isDisabled()) {

            // Enforce applicable account restrictions
            if (databaseRestrictionsApplicable) {

                // Verify user account is still valid as of today
                if (!user.isAccountValid())
                    throw new GuacamoleClientException("LOGIN.ERROR_NOT_VALID");

                // Verify user account is allowed to be used at the current time
                if (!user.isAccountAccessible())
                    throw new GuacamoleClientException(
                            "LOGIN.ERROR_NOT_ACCESSIBLE");

            }

            // Update password if password is expired AND the password was
            // actually involved in the authentication process
            if (databaseCredentialsUsed) {
                if (user.isExpired()
                        || passwordPolicyService.isPasswordExpired(user))
                    userService.resetExpiredPassword(user,
                            authenticatedUser.getCredentials());
            }

            // Return all data associated with the authenticated user
            ModeledUserContextAbstract context = (ModeledUserContextAbstract) userContextProvider
                    .get();
            context.init(user.getCurrentUser());
            return context;

        }

        // Veto authentication result only if database-specific account
        // restrictions apply in this situation
        if (databaseRestrictionsApplicable)
            throw new GuacamoleInvalidCredentialsException("Invalid login",
                    CredentialsInfo.USERNAME_PASSWORD);

        // There is no data to be returned for the user, either because they do
        // not exist or because restrictions prevent their data from being
        // retrieved, but no restrictions apply which should prevent the user
        // from authenticating entirely
        return null;

    }

    @Override
    public UserContext updateUserContext(
            AuthenticationProvider authenticationProvider, UserContext context,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        // No need to update the context
        return context;

    }

}
