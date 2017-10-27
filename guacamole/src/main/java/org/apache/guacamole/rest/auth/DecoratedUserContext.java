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

package org.apache.guacamole.rest.auth;

import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.DelegatingUserContext;
import org.apache.guacamole.net.auth.UserContext;

/**
 * A UserContext which has been decorated by all applicable
 * AuthenticationProviders.
 */
public class DecoratedUserContext extends DelegatingUserContext {

    /**
     * The original, undecorated UserContext.
     */
    private final UserContext original;

    /**
     * Repeatedly decorates the given UserContext, invoking the decorate()
     * function of each given AuthenticationProvider, wrapping the UserContext
     * within successive layers of decoration. The AuthenticationProvider which
     * originated the given UserContext will be ignored.
     *
     * @param userContext
     *     The UserContext to decorate.
     *
     * @param authenticatedUser
     *     The AuthenticatedUser identifying the user associated with the given
     *     UserContext.
     *
     * @param credentials
     *     The credentials associated with the request which produced the given
     *     UserContext.
     *
     * @param authProviders
     *     The AuthenticationProviders which should be used to decorate the
     *     given UserContext. The order of this list dictates the order in
     *     which each AuthenticationProvider's decorate() function will be
     *     invoked.
     *
     * @return
     *     A UserContext instance which has been decorated (wrapped) by all
     *     applicable AuthenticationProviders.
     *
     * @throws GuacamoleException
     *     If any of the given AuthenticationProviders fails while decorating
     *     the UserContext.
     */
    private static UserContext decorate(UserContext userContext,
            AuthenticatedUser authenticatedUser, Credentials credentials,
            List<AuthenticationProvider> authProviders) throws GuacamoleException {

        AuthenticationProvider owner = userContext.getAuthenticationProvider();

        // Poll each AuthenticationProvider to decorate the given UserContext
        for (AuthenticationProvider authProvider : authProviders) {

            // Skip the AuthenticationProvider which produced the UserContext
            // being decorated
            if (authProvider == owner)
                continue;

            // Apply next layer of wrapping around UserContext
            UserContext decorated = authProvider.decorate(userContext,
                    authenticatedUser, credentials);

            // Do not allow misbehaving extensions to wipe out the
            // UserContext entirely
            if (decorated != null)
                userContext = decorated;

        }

        return userContext;

    }

    /**
     * Creates a new DecoratedUserContext, invoking the the decorate() function
     * of the given AuthenticationProviders to decorate the provided
     * UserContext. Decoration by each AuthenticationProvider will occur in the
     * order given. Only AuthenticationProviders which did not originate the
     * given UserContext will be used.
     *
     * @param userContext
     *     The UserContext to decorate.
     *
     * @param authenticatedUser
     *     The AuthenticatedUser identifying the user associated with the given
     *     UserContext.
     *
     * @param credentials
     *     The credentials associated with the request which produced the given
     *     UserContext.
     *
     * @param authProviders
     *     The AuthenticationProviders which should be used to decorate the
     *     given UserContext. The order of this list dictates the order in
     *     which each AuthenticationProvider's decorate() function will be
     *     invoked.
     *
     * @throws GuacamoleException
     *     If any of the given AuthenticationProviders fails while decorating
     *     the UserContext.
     */
    public DecoratedUserContext(UserContext userContext,
            AuthenticatedUser authenticatedUser, Credentials credentials,
            List<AuthenticationProvider> authProviders) throws GuacamoleException {
        super(decorate(userContext, authenticatedUser, credentials, authProviders));
        this.original = userContext;
    }

    /**
     * Returns the original, undecorated UserContext, as provided to the
     * constructor of this DecoratedUserContext.
     *
     * @return
     *     The original, undecorated UserContext.
     */
    public UserContext getOriginal() {
        return original;
    }

}
