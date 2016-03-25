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

import java.lang.reflect.InvocationTargetException;
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
 *
 * @author Michael Jumper
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

        AuthenticationProvider instance = null;
        
        try {
            // Attempt to instantiate the authentication provider
            instance = authProviderClass.getConstructor().newInstance();
        }
        catch (NoSuchMethodException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("AuthenticationProvider is missing a default constructor.", e);
        }
        catch (SecurityException e) {
            logger.error("The Java security mananager is preventing authentication extensions "
                       + "from being loaded. Please check the configuration of Java or your "
                       + "servlet container.");
            logger.debug("Creation of AuthenticationProvider disallowed by security manager.", e);
        }
        catch (InstantiationException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("AuthenticationProvider cannot be instantiated.", e);
        }
        catch (IllegalAccessException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("Default constructor of AuthenticationProvider is not public.", e);
        }
        catch (IllegalArgumentException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("Default constructor of AuthenticationProvider cannot accept zero arguments.", e);
        } 
        catch (InvocationTargetException e) {

            // Obtain causing error - create relatively-informative stub error if cause is unknown
            Throwable cause = e.getCause();
            if (cause == null)
                cause = new GuacamoleException("Error encountered during initialization.");
            
            logger.error("Authentication extension failed to start: {}", cause.getMessage());
            logger.debug("AuthenticationProvider instantiation failed.", e);

        }
       
        // Associate instance, if any
        authProvider = instance;

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
            AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("User data refresh attempt denied because the authentication system could not be loaded. Please check for errors earlier in the logs.");
            return null;
        }

        // Delegate to underlying auth provider
        return authProvider.updateUserContext(context, authenticatedUser);
        
    }

}
