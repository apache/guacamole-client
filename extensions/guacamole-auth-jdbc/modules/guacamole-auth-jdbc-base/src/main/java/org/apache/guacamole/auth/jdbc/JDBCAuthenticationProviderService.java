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

package org.apache.guacamole.auth.jdbc;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicyService;
import org.apache.guacamole.auth.jdbc.sharing.user.SharedAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUserContext;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * AuthenticationProviderService implementation which authenticates users with
 * a username/password pair, producing new UserContext objects which are backed
 * by an underlying, arbitrary database.
 */
public class JDBCAuthenticationProviderService implements AuthenticationProviderService  {

    /**
     * The environment of the Guacamole server.
     */
    @Inject
    private JDBCEnvironment environment;

    /**
     * Service for accessing users.
     */
    @Inject
    private UserService userService;

    /**
     * Service for enforcing password complexity policies.
     */
    @Inject
    private PasswordPolicyService passwordPolicyService;

    /**
     * Provider for retrieving UserContext instances.
     */
    @Inject
    private Provider<ModeledUserContext> userContextProvider;

    @Override
    public AuthenticatedUser authenticateUser(AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException {

        // Authenticate user
        AuthenticatedUser user = userService.retrieveAuthenticatedUser(authenticationProvider, credentials);
        if (user != null)
            return user;

        // Otherwise, unauthorized
        throw new GuacamoleInvalidCredentialsException("Invalid login", CredentialsInfo.USERNAME_PASSWORD);

    }

    @Override
    public ModeledUserContext getUserContext(AuthenticationProvider authenticationProvider,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // Retrieve user account for already-authenticated user
        ModeledUser user = userService.retrieveUser(authenticationProvider, authenticatedUser);
        if (user != null && !user.isDisabled()) {

            // Account restrictions specific to this extension apply if this
            // extension authenticated the user OR if an account from this
            // extension is explicitly required
            if (authenticatedUser instanceof ModeledAuthenticatedUser
                    || environment.isUserRequired()) {

                // Verify user account is still valid as of today
                if (!user.isAccountValid())
                    throw new GuacamoleClientException("LOGIN.ERROR_NOT_VALID");

                // Verify user account is allowed to be used at the current time
                if (!user.isAccountAccessible())
                    throw new GuacamoleClientException("LOGIN.ERROR_NOT_ACCESSIBLE");

                // Update password if password is expired
                if (user.isExpired() || passwordPolicyService.isPasswordExpired(user))
                    userService.resetExpiredPassword(user, authenticatedUser.getCredentials());

            }

            // Link to user context
            ModeledUserContext context = userContextProvider.get();
            context.init(user.getCurrentUser());
            return context;

        }

        // Do not invalidate the authentication result of users who were
        // authenticated via our own connection sharing links
        if (authenticatedUser instanceof SharedAuthenticatedUser)
            return null;

        // Simply return no data if a database user account is not required
        if (!environment.isUserRequired())
            return null;

        // Otherwise, invalidate the authentication result, as database user
        // accounts are absolutely required
        throw new GuacamoleInvalidCredentialsException("Invalid login",
                CredentialsInfo.USERNAME_PASSWORD);

    }

    @Override
    public UserContext updateUserContext(AuthenticationProvider authenticationProvider,
            UserContext context, AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleException {

        // No need to update the context
        return context;

    }

}
