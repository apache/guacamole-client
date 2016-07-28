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

import com.google.inject.Injector;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.auth.jdbc.user.AuthenticationProviderService;
import org.apache.guacamole.net.auth.AuthenticatedUser;

/**
 * Provides a base implementation of an AuthenticationProvider which is backed
 * by an arbitrary underlying database. It is up to the subclass implementation
 * to configure the underlying database appropriately via Guice.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public abstract class JDBCAuthenticationProvider implements AuthenticationProvider {

    /**
     * Provider of the singleton Injector instance which will manage the object
     * graph of this authentication provider.
     */
    private final JDBCInjectorProvider injectorProvider;

    /**
     * Creates a new AuthenticationProvider that is backed by an arbitrary
     * underlying database.
     *
     * @param injectorProvider
     *     A JDBCInjectorProvider instance which provides singleton instances
     *     of a Guice Injector, pre-configured to set up all injections and
     *     access to the underlying database via MyBatis.
     */
    public JDBCAuthenticationProvider(JDBCInjectorProvider injectorProvider) {
        this.injectorProvider = injectorProvider;
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        Injector injector = injectorProvider.get();

        // Create AuthenticatedUser based on credentials, if valid
        AuthenticationProviderService authProviderService = injector.getInstance(AuthenticationProviderService.class);
        return authProviderService.authenticateUser(this, credentials);

    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleException {

        // No need to update authenticated users
        return authenticatedUser;

    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        Injector injector = injectorProvider.get();

        // Create UserContext based on credentials, if valid
        AuthenticationProviderService authProviderService = injector.getInstance(AuthenticationProviderService.class);
        return authProviderService.getUserContext(authenticatedUser);

    }

    @Override
    public UserContext updateUserContext(UserContext context,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // No need to update the context
        return context;

    }

}
