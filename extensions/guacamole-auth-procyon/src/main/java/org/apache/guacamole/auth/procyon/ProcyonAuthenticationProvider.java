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

package org.apache.guacamole.auth.procyon;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Guacamole authentication backend which authenticates users using an
 * arbitrary external HTTP header. No storage for connections is
 * provided - only authentication. Storage must be provided by some other
 * extension.
 */
public class ProcyonAuthenticationProvider extends AbstractAuthenticationProvider {
    private static final Logger logger = Logger.getLogger(ProcyonAuthenticationProvider.class.getName());
    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;

    /**
     * Creates a new ProcyonAuthenticationProvider that authenticates users
     * using HTTP headers.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public ProcyonAuthenticationProvider() throws GuacamoleException {

        // Set up Guice injector.
        injector = Guice.createInjector(
            new ProcyonAuthenticationProviderModule(this)
        );

    }

    @Override
    public String getIdentifier() {
        return "procyon";
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        logger.log(Level.INFO, "ProcyonAuthenticationProvider.authenticateUser() called for: " + credentials.getUsername());

        // Pass credentials to authentication service.
        AuthenticationProviderService authProviderService = injector.getInstance(AuthenticationProviderService.class);
        return authProviderService.authenticateUser(credentials);

    }

}
