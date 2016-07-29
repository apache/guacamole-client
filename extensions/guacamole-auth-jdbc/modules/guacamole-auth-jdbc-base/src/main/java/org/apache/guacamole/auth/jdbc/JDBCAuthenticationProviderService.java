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
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.UserContext;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * AuthenticationProviderService implementation which authenticates users with
 * a username/password pair, producing new UserContext objects which are backed
 * by an underlying, arbitrary database.
 *
 * @author Michael Jumper
 */
public class JDBCAuthenticationProviderService implements AuthenticationProviderService  {

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
    public UserContext getUserContext(AuthenticationProvider authenticationProvider,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // Retrieve user account for already-authenticated user
        ModeledUser user = userService.retrieveUser(authenticationProvider, authenticatedUser);
        if (user == null)
            return null;

        // Link to user context
        UserContext context = userContextProvider.get();
        context.init(user.getCurrentUser());
        return context;

    }

}
