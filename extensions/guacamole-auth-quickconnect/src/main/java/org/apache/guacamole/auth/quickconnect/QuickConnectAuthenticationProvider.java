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

import java.util.Collections;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * Class providing the necessary hooks into the Guacamole Client authentication
 * process so that the QuickConnect functionality can be initialized and be used
 * throughout the web client.
 */
public class QuickConnectAuthenticationProvider extends SimpleAuthenticationProvider {

    /**
     * userContext for this authentication provider.
     */
    private UserContext userContext;

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

        String username = credentials.getUsername();
        if (username == null || username.isEmpty())
            throw new GuacamoleInvalidCredentialsException("You must login.", CredentialsInfo.USERNAME_PASSWORD);

        return null;

    }

    @Override
    public Map<String, GuacamoleConfiguration>
        getAuthorizedConfigurations(Credentials credentials)
        throws GuacamoleException {

        return Collections.<String, GuacamoleConfiguration>emptyMap();

    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        return new QuickConnectUserContext(this, authenticatedUser.getIdentifier());

    }

}