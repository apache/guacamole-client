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

package org.apache.guacamole.auth.sso.user;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.AbstractAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An AuthenticatedUser whose identity has been supplied by an arbitrary SSO
 * service. An SSOAuthenticatedUser may additionally be associated with a set
 * of user-specific parameter tokens to be injected into any connections used
 * by that user.
 */
public class SSOAuthenticatedUser extends AbstractAuthenticatedUser {

    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SSOAuthenticatedUser.class);
    
    /**
     * Reference to the authentication provider associated with this
     * authenticated user.
     */
    @Inject
    private AuthenticationProvider authProvider;
    
    /**
     * The environment in which this instance of Guacamole is running.
     */
    @Inject
    private Environment environment;

    /**
     * The credentials provided when this user was authenticated.
     */
    private Credentials credentials;

    /**
     * The groups that this user belongs to.
     */
    private Set<String> effectiveGroups;

    /**
     * Parameter tokens to be automatically injected for any connections used
     * by this user.
     */
    private Map<String, String> tokens;

    /**
     * Initializes this SSOAuthenticatedUser, associating it with the given
     * username, credentials, groups, and parameter tokens. This function must
     * be invoked for every SSOAuthenticatedUser created.
     *
     * @param username
     *     The username of the user that was authenticated.
     *
     * @param credentials
     *     The credentials provided when this user was authenticated.
     *
     * @param effectiveGroups
     *     The groups that the authenticated user belongs to.
     *
     * @param tokens
     *     A map of all the name/value pairs that should be available
     *     as tokens when connections are established by this user.
     */
    public void init(String username, Credentials credentials,
            Set<String> effectiveGroups, Map<String, String> tokens) {
        this.credentials = credentials;
        this.effectiveGroups = Collections.unmodifiableSet(effectiveGroups);
        this.tokens = Collections.unmodifiableMap(tokens);
        setIdentifier(username);
    }

    /**
     * Returns a Map of the parameter tokens that should be automatically
     * injected into connections used by this user during their session. If
     * there are no parameter tokens applicable to the SSO implementation, this
     * may simply be an empty map.
     *
     * @return
     *     A map of the parameter token name/value pairs that should be
     *     automatically injected into connections used by this user.
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
    
    @Override
    public boolean isCaseSensitive() {
        try {
            return environment.getCaseSensitiveUsernames();
        }
        catch (GuacamoleException e) {
            // Most SSO systems do not consider usernames to be case-sensitive;
            // however, in order to avoid any surprises created by the introduction
            // of case-sensitivity, we've opted to continue to evaluate these
            // usernames in a case-sensitive manner by default.
            LOGGER.error("Error occurred when trying to retrieve case-sensitivity configuration: {}. "
                    + "Usernames comparisons will be done in a case-sensitive manner.", e.getMessage());
            LOGGER.debug("Exception caught when trying to access the case-sensitivity property.", e);
            return true;
        }
    }

}
