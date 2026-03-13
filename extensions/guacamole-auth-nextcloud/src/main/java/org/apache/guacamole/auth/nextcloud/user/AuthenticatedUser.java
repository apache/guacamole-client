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

package org.apache.guacamole.auth.nextcloud.user;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Set;
import org.apache.guacamole.net.auth.AbstractAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * A Nextcloud JWT implementation of AuthenticatedUser, associating a
 * username and particular set of credentials with the HTTP authentication
 * provider.
 */
public class AuthenticatedUser extends AbstractAuthenticatedUser {

    /**
     * Reference to the authentication provider associated with this
     * authenticated user.
     */
    @Inject
    private AuthenticationProvider authProvider;

    /**
     * The credentials provided when this user was authenticated.
     */
    private Credentials credentials;

    /**
     * The UID (User ID) which is defined inside the JWT payload.
     */
    private String identifier;

    /**
     * Constructs an {@code AuthenticatedUser} with the specified identifier, authentication provider, and credentials.
     *
     * @param identifier
     *     The unique identifier for the authenticated user.
     *
     * @param authProvider
     *     The authentication provider used for this user.
     *
     * @param credentials
     *     The credentials of HTTP request.
     */
    public AuthenticatedUser(String identifier, AuthenticationProvider authProvider, Credentials credentials) {
        this.identifier = identifier;
        this.authProvider = authProvider;
        this.credentials = credentials;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public Set<String> getEffectiveUserGroups() {
        return Collections.emptySet();
    }

    @Override
    public void invalidate() {
        // No invalidation logic needed
    }

}
