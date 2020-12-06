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

package org.apache.guacamole.auth.cas.user;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.net.auth.AbstractAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * An CAS-specific implementation of AuthenticatedUser, associating a
 * username and particular set of credentials with the CAS authentication
 * provider.
 */
public class CASAuthenticatedUser extends AbstractAuthenticatedUser {

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
     * Tokens associated with this authenticated user.
     */
    private Map<String, String> tokens;

    /**
     * The unique identifiers of all user groups which this user is a member of.
     */
    private Set<String> effectiveGroups;

    /**
     * Initializes this AuthenticatedUser using the given username and
     * credentials, and an empty map of parameter tokens.
     *
     * @param username
     *     The username of the user that was authenticated.
     *
     * @param credentials
     *     The credentials provided when this user was authenticated.
     */
    public void init(String username, Credentials credentials) {
        this.init(username, credentials, Collections.emptyMap(), Collections.emptySet());
    }
    
    /**
     * Initializes this AuthenticatedUser using the given username,
     * credentials, and parameter tokens.
     *
     * @param username
     *     The username of the user that was authenticated.
     *
     * @param credentials
     *     The credentials provided when this user was authenticated.
     * 
     * @param tokens
     *     A map of all the name/value pairs that should be available
     *     as tokens when connections are established with this user.
     */
    public void init(String username, Credentials credentials,
            Map<String, String> tokens, Set<String> effectiveGroups) {
        this.credentials = credentials;
        this.tokens = Collections.unmodifiableMap(tokens);
        this.effectiveGroups = effectiveGroups;
        setIdentifier(username.toLowerCase());
    }

    /**
     * Returns a Map containing the name/value pairs that can be applied
     * as parameter tokens when connections are established by the user.
     * 
     * @return
     *     A Map containing all of the name/value pairs that can be
     *     used as parameter tokens by this user.
     */
    public Map<String, String> getTokens() {
        return tokens;
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
        return effectiveGroups;
    }

}
