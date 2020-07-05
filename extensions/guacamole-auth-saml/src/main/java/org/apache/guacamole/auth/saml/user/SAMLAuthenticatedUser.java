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

package org.apache.guacamole.auth.saml.user;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.net.auth.AbstractAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * An SAML-specific implementation of AuthenticatedUser, associating a
 * username and particular set of credentials with the SAML authentication
 * provider.
 */
public class SAMLAuthenticatedUser extends AbstractAuthenticatedUser {

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
     * The effective groups of the authenticated user.
     */
    private Set<String> effectiveGroups;
    
    /**
     * Tokens associated with the authenticated user.
     */
    private Map<String, String> tokens;

    /**
     * Initializes this AuthenticatedUser using the given username and
     * credentials.
     *
     * @param username
     *     The username of the user that was authenticated.
     *
     * @param credentials
     *     The credentials provided when this user was authenticated.
     * 
     * @param tokens
     *     The tokens available from this authentication provider.
     * 
     * @param effectiveGroups
     *     The groups of which this user is a member.
     */
    public void init(String username, Credentials credentials,
            Map<String, String> tokens, Set<String> effectiveGroups) {
        this.credentials = credentials;
        this.effectiveGroups = effectiveGroups;
        this.tokens = tokens;
        setIdentifier(username);
    }
    
    /**
     * Returns a Map of tokens associated with this authenticated user.
     * 
     * @return 
     *     A map of token names and values available from this user account.
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
