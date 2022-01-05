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

package org.apache.guacamole.auth.ban;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;

/**
 * AuthenticationProvider implementation that blocks further authentication
 * attempts that are related to past authentication failures flagged by
 * {@link BanningAuthenticationListener}.
 */
public class BanningAuthenticationProvider extends AbstractAuthenticationProvider {

    /**
     * The maximum number of failed authentication attempts allowed before an
     * address is temporarily banned.
     */
    private static final IntegerGuacamoleProperty MAX_ATTEMPTS = new IntegerGuacamoleProperty() {

        @Override
        public String getName() {
            return "ban-max-invalid-attempts";
        }

    };

    /**
     * The length of time that each address should be banned after reaching the
     * maximum number of failed authentication attempts, in seconds.
     */
    private static final IntegerGuacamoleProperty IP_BAN_DURATION = new IntegerGuacamoleProperty() {

        @Override
        public String getName() {
            return "ban-address-duration";
        }

    };

    /**
     * The default maximum number of failed authentication attempts allowed
     * before an address is temporarily banned.
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * The default length of time that each address should be banned after
     * reaching the maximum number of failed authentication attempts, in
     * seconds.
     */
    private static final int DEFAULT_IP_BAN_DURATION = 300;

    /**
     * Shared tracker of addresses that have repeatedly failed authentication.
     */
    private final AuthenticationFailureTracker tracker;

    /**
     * Creates a new BanningAuthenticationProvider which automatically bans
     * further authentication attempts from addresses that have repeatedly
     * failed to authenticate. The ban duration and maximum number of failed
     * attempts allowed before banning are configured within
     * guacamole.properties.
     *
     * @throws GuacamoleException
     *     If an error occurs parsing the configuration properties used by this
     *     extension.
     */
    public BanningAuthenticationProvider() throws GuacamoleException {

        Environment environment = LocalEnvironment.getInstance();
        int maxAttempts = environment.getProperty(MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS);
        int banDuration = environment.getProperty(IP_BAN_DURATION, DEFAULT_IP_BAN_DURATION);

        tracker = new AuthenticationFailureTracker(maxAttempts, banDuration);
        BanningAuthenticationListener.setAuthenticationFailureTracker(tracker);

    }

    @Override
    public String getIdentifier() {
        return "ban";
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials) throws GuacamoleException {
        tracker.notifyAuthenticationRequestReceived(credentials);
        return null;
    }

    @Override
    public UserContext getUserContext(AuthenticatedUser authenticatedUser) throws GuacamoleException {
        tracker.notifyAuthenticationRequestReceived(authenticatedUser.getCredentials());
        return null;
    }

}
