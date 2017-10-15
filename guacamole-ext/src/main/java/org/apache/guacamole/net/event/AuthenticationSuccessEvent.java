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

import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;

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
public class AuthenticationSuccessEvent implements UserEvent, CredentialEvent, AuthenticatedUserEvent {

    /**
     * The UserContext associated with the request that is connecting the
     * tunnel, if any.
     */
    private UserContext context;

    /**
     * The credentials which passed authentication.
     */
    private AuthenticatedUser authenticatedUser;

    /**
     * Creates a new AuthenticationSuccessEvent which represents a successful
     * authentication attempt with the given credentials.
     *
     * @param context The UserContext created as a result of successful
     *                authentication.
     * @param authenticatedUser The user which passed authentication.
     */
    public AuthenticationSuccessEvent(UserContext context, AuthenticatedUser authenticatedUser) {
        this.context = context;
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    public UserContext getUserContext() {
        return context;
    }

    @Override
    public Credentials getCredentials() {
        if (authenticatedUser == null)
            return null;
        else
            return authenticatedUser.getCredentials();
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

}
