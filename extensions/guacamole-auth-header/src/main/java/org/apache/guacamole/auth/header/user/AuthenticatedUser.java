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

package org.apache.guacamole.auth.header.user;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.header.ConfigurationService;
import org.apache.guacamole.net.auth.AbstractAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An HTTP header implementation of AuthenticatedUser, associating a
 * username and particular set of credentials with the HTTP authentication
 * provider.
 */
public class AuthenticatedUser extends AbstractAuthenticatedUser {

    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedUser.class);
    
    /**
     * Reference to the authentication provider associated with this
     * authenticated user.
     */
    @Inject
    private AuthenticationProvider authProvider;
    
    /**
     * Service for retrieving header configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * The credentials provided when this user was authenticated.
     */
    private Credentials credentials;

    /**
     * Initializes this AuthenticatedUser using the given username and
     * credentials.
     *
     * @param username
     *     The username of the user that was authenticated.
     *
     * @param credentials
     *     The credentials provided when this user was authenticated.
     */
    public void init(String username, Credentials credentials) {
        this.credentials = credentials;
        setIdentifier(username.toLowerCase());
    }

    @Override
    public boolean isCaseSensitive() {
        try {
            return confService.getCaseSensitiveUsernames();
        }
        catch (GuacamoleException e) {
            LOGGER.error("Error when trying to retrieve header configuration: {}."
                    + " Usernames comparison will be case-sensitive.", e);
            LOGGER.debug("Exception caught when retrieving header configuration.", e);
            return true;
        }
    }
    
    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

}
