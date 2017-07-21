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

package org.apache.guacamole.auth.quickconnect;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.StandardTokens;
import org.apache.guacamole.token.TokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickConnectAuthenticationProvider extends SimpleAuthenticationProvider {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(QuickConnectAuthenticationProvider.class);

    private UserContext userContext = null;

    @Inject
    private Provider<QuickConnectUserContext> userContextProvider;

    @Override
    public String getIdentifier() {
        return "quickconnect";
    }


    /**
     * For QuickConnect, authenticateUser simply returns null because this
     * extension is designed to provide only a connection directory to users
     * that are already authenticated and not any actual authentication.
     *
     * @param credentials
     *     Credentials object passed in from Guacamole login.
     *
     * @returns
     *     Returns null, which causes the client to move on to the next
     *     module.
     */
    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
        throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< authenticateUser running for user {}.", credentials.getUsername());

        String username = credentials.getUsername();
        if(username == null || username.isEmpty())
            throw new GuacamoleInvalidCredentialsException("You must login.", CredentialsInfo.USERNAME_PASSWORD);

        userContext = new QuickConnectUserContext(this, credentials.getUsername());

        return null;

    }

    @Override
    public Map<String, GuacamoleConfiguration>
        getAuthorizedConfigurations(Credentials credentials)
        throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< Retrieving configurations for user {}", credentials.getUsername());

        if(userContext == null)
            userContext = new QuickConnectUserContext(this, credentials.getUsername());

        return Collections.<String, GuacamoleConfiguration>emptyMap();

    }

    private Map<String, GuacamoleConfiguration>
            getFilteredAuthorizedConfigurations(Credentials credentials)
            throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< Filtering configurations.");

        // Get configurations
        Map<String, GuacamoleConfiguration> configs =
                getAuthorizedConfigurations(credentials);

        // Return as unauthorized if not authorized to retrieve configs
        if (configs == null)
            return null;

        // Build credential TokenFilter
        TokenFilter tokenFilter = new TokenFilter();
        StandardTokens.addStandardTokens(tokenFilter, credentials);

        // Filter each configuration
        for (GuacamoleConfiguration config : configs.values())
            tokenFilter.filterValues(config.getParameters());

        return configs;

    }

    private Map<String, GuacamoleConfiguration>
            getFilteredAuthorizedConfigurations(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Pull using credentials
        return getFilteredAuthorizedConfigurations(authenticatedUser.getCredentials());

    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        logger.debug(">>>QuickConnect<<< getUserContext for {}", authenticatedUser.getCredentials().getUsername());

        // Get configurations
        // Map<String, GuacamoleConfiguration> configs =
        //         getFilteredAuthorizedConfigurations(authenticatedUser);

        // Return as unauthorized if not authorized to retrieve configs
        // if (configs == null)
        //     return null;

        // Return user context restricted to authorized configs
        // return new QuickConnectUserContext(this, authenticatedUser.getIdentifier(), configs);

        return new QuickConnectUserContext(this, authenticatedUser.getIdentifier());

    }

}
