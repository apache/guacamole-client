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

package org.apache.guacamole.rest;

import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.GuacamoleSession;

/**
 * Provides easy access and automatic error handling for retrieval of objects.
 */
public class ObjectRetrievalService {

    /**
     * Retrieves a single UserContext from the given GuacamoleSession, which
     * may contain multiple UserContexts.
     *
     * @param session
     *     The GuacamoleSession to retrieve the UserContext from.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider that created the
     *     UserContext being retrieved. Only one UserContext per User per
     *     AuthenticationProvider can exist.
     *
     * @return
     *     The UserContext that was created by the AuthenticationProvider
     *     having the given identifier.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the UserContext, or if the
     *     UserContext does not exist.
     */
    public UserContext retrieveUserContext(GuacamoleSession session,
            String authProviderIdentifier) throws GuacamoleException {

        // Get list of UserContexts
        List<UserContext> userContexts = session.getUserContexts();

        // Locate and return the UserContext associated with the
        // AuthenticationProvider having the given identifier, if any
        for (UserContext userContext : userContexts) {

            // Get AuthenticationProvider associated with current UserContext
            AuthenticationProvider authProvider = userContext.getAuthenticationProvider();

            // If AuthenticationProvider identifier matches, done
            if (authProvider.getIdentifier().equals(authProviderIdentifier))
                return userContext;

        }

        throw new GuacamoleResourceNotFoundException("Session not associated with authentication provider \"" + authProviderIdentifier + "\".");

    }

}
