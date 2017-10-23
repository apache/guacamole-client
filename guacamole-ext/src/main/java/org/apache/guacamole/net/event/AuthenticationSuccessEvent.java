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

package org.apache.guacamole.net.event;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An event which is triggered whenever a user's credentials pass
 * authentication. The credentials that passed authentication are included
 * within this event, and can be retrieved using getCredentials().
 * <p>
 * If a {@link org.apache.guacamole.net.event.listener.Listener} throws
 * a GuacamoleException when handling an event of this type, successful authentication
 * is effectively <em>vetoed</em> and will be subsequently processed as though the
 * authentication failed.
 */
public class AuthenticationSuccessEvent implements
        UserEvent, CredentialEvent, AuthenticatedUserEvent, AuthenticationProviderEvent {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationSuccessEvent.class);

    /**
     * The UserContext, if any, for the user which passed authentication.
     */
    private UserContext userContext;

    /**
     * The AuthenticatedUser which passed authentication.
     */
    private AuthenticatedUser authenticatedUser;

    /**
     * The credentials which passed authentication.
     */
    private Credentials credentials;

    /**
     * The authentication provider which accepted the credentials.
     */
    private AuthenticationProvider authProvider;

    /**
     * Creates a new AuthenticationSuccessEvent which represents a successful
     * authentication attempt with the given credentials.
     *
     * @param authenticatedUser The user which passed authentication.
     */
    public AuthenticationSuccessEvent(AuthenticatedUser authenticatedUser) throws GuacamoleException {
        this.authenticatedUser = authenticatedUser;
        try {
            if (authenticatedUser != null) {
                this.userContext = authenticatedUser.getAuthenticationProvider().getUserContext(authenticatedUser);
                this.credentials = authenticatedUser.getCredentials();
                this.authProvider = authenticatedUser.getAuthenticationProvider();
            }
        }
        catch (GuacamoleResourceNotFoundException e) {
            logger.warn("No user context available while creating AuthenticationSuccessEvent");
            logger.debug("Received an exception attempting to retrieve the UserContext.", e);
        }
    }

    @Override
    public UserContext getUserContext() {
        return userContext;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

}
