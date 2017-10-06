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

package org.apache.guacamole.extension;

import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a safe wrapper around an AuthenticationProvider subclass, such that
 * authentication attempts can cleanly fail, and errors can be properly logged,
 * even if the AuthenticationProvider cannot be instantiated.
 */
public class AuthenticationProviderFacade implements AuthenticationProvider {

    /**
     * Logger for this class.
     */
    private Logger logger = LoggerFactory.getLogger(AuthenticationProviderFacade.class);

    /**
     * The underlying authentication provider, or null if the authentication
     * provider could not be instantiated.
     */
    private final AuthenticationProvider authProvider;

    /**
     * The identifier to provide for the underlying authentication provider if
     * the authentication provider could not be loaded.
     */
    private final String facadeIdentifier = UUID.randomUUID().toString();

    /**
     * Creates a new AuthenticationProviderFacade which delegates all function
     * calls to an instance of the given AuthenticationProvider subclass. If
     * an instance of the given class cannot be created, creation of this
     * facade will still succeed, but its use will result in errors being
     * logged, and all authentication attempts will fail.
     *
     * @param authProviderClass
     *     The AuthenticationProvider subclass to instantiate.
     */
    public AuthenticationProviderFacade(Class<? extends AuthenticationProvider> authProviderClass) {
        authProvider = ProviderFactory.newInstance("authentication provider",
            authProviderClass);
    }

    @Override
    public String getIdentifier() {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("The authentication system could not be loaded. Please check for errors earlier in the logs.");
            return facadeIdentifier;
        }

        // Delegate to underlying auth provider
        return authProvider.getIdentifier();

    }

    @Override
    public Object getResource() throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("The authentication system could not be loaded. Please check for errors earlier in the logs.");
            return null;
        }

        // Delegate to underlying auth provider
        return authProvider.getResource();

    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("Authentication attempt denied because the authentication system could not be loaded. Please check for errors earlier in the logs.");
            throw new GuacamoleInvalidCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);
        }

        // Delegate to underlying auth provider
        return authProvider.authenticateUser(credentials);

    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("Reauthentication attempt denied because the authentication system could not be loaded. Please check for errors earlier in the logs.");
            throw new GuacamoleInvalidCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);
        }

        // Delegate to underlying auth provider
        return authProvider.updateAuthenticatedUser(authenticatedUser, credentials);

    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("User data retrieval attempt denied because the authentication system could not be loaded. Please check for errors earlier in the logs.");
            return null;
        }

        // Delegate to underlying auth provider
        return authProvider.getUserContext(authenticatedUser);
        
    }

    @Override
    public UserContext updateUserContext(UserContext context,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("User data refresh attempt denied because the authentication system could not be loaded. Please check for errors earlier in the logs.");
            return null;
        }

        // Delegate to underlying auth provider
        return authProvider.updateUserContext(context, authenticatedUser, credentials);
        
    }

    @Override
    public void shutdown() {
        if (authProvider != null)
            authProvider.shutdown();
    }

}
