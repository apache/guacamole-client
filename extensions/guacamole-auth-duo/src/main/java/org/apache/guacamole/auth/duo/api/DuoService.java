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

package org.apache.guacamole.auth.duo.api;

import com.google.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.duo.conf.ConfigurationService;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which produces signed requests and parses/verifies signed responses
 * as required by Duo's API.
 */
public class DuoService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(DuoService.class);

    /**
     * Pattern which matches valid Duo responses. Each response is made up of
     * two sections, separated from each other by a colon, where each section
     * is a signed Duo cookie.
     */
    private static final Pattern RESPONSE_FORMAT = Pattern.compile("([^:]+):([^:]+)");

    /**
     * The index of the capturing group within RESPONSE_FORMAT which
     * contains the DUO_RESPONSE cookie signed by the secret key.
     */
    private static final int DUO_COOKIE_GROUP = 1;

    /**
     * The index of the capturing group within RESPONSE_FORMAT which
     * contains the APPLICATION cookie signed by the application key.
     */
    private static final int APP_COOKIE_GROUP = 2;

    /**
     * The amount of time that each generated cookie remains valid, in seconds.
     */
    private static final int COOKIE_EXPIRATION_TIME = 300;

    /**
     * Service for retrieving Duo configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Creates and signs a new request to verify the identity of the given
     * user. This request may ultimately be sent to Duo, resulting in a signed
     * response from Duo if that verification succeeds.
     *
     * @param authenticatedUser
     *     The user whose identity should be verified.
     *
     * @return
     *     A signed user verification request which can be sent to Duo.
     *
     * @throws GuacamoleException
     *     If required Duo-specific configuration options are missing or
     *     invalid, or if an error prevents generation of the signature.
     */
    public String createSignedRequest(AuthenticatedUser authenticatedUser)
        throws GuacamoleException {

        // Generate a cookie associating the username with the integration key
        DuoCookie cookie = new DuoCookie(authenticatedUser.getIdentifier(),
                confService.getIntegrationKey(),
                DuoCookie.currentTimestamp() + COOKIE_EXPIRATION_TIME);

        // Sign cookie with secret key
        SignedDuoCookie duoCookie = new SignedDuoCookie(cookie,
                SignedDuoCookie.Type.DUO_REQUEST,
                confService.getSecretKey());

        // Sign cookie with application key
        SignedDuoCookie appCookie = new SignedDuoCookie(cookie,
                SignedDuoCookie.Type.APPLICATION,
                confService.getApplicationKey());

        // Return signed request containing both signed cookies, separated by
        // a colon (as required by Duo)
        return duoCookie + ":" + appCookie;

    }

    /**
     * Returns whether the given signed response is a valid response from Duo
     * which verifies the identity of the given user. If the given response is
     * invalid or does not verify the identity of the given user (including if
     * it is a valid response which verifies the identity of a DIFFERENT user),
     * false is returned.
     *
     * @param authenticatedUser
     *     The user that the given signed response should verify.
     *
     * @param signedResponse
     *     The signed response received from Duo in response to a signed
     *     request.
     *
     * @return
     *     true if the signed response is a valid response from Duo AND verifies
     *     the identity of the given user, false otherwise.
     *
     * @throws GuacamoleException
     *     If required Duo-specific configuration options are missing or
     *     invalid, or if an error occurs prevents validation of the signature.
     */
    public boolean isValidSignedResponse(AuthenticatedUser authenticatedUser,
            String signedResponse) throws GuacamoleException {

        SignedDuoCookie duoCookie;
        SignedDuoCookie appCookie;

        // Retrieve username from externally-authenticated user
        String username = authenticatedUser.getIdentifier();

        // Retrieve Duo-specific keys from configuration
        String applicationKey = confService.getApplicationKey();
        String integrationKey = confService.getIntegrationKey();
        String secretKey = confService.getSecretKey();

        try {

            // Verify format of response
            Matcher matcher = RESPONSE_FORMAT.matcher(signedResponse);
            if (!matcher.matches()) {
                logger.debug("Duo response is not in correct format.");
                return false;
            }

            // Parse signed cookie defining the user verified by Duo
            duoCookie = SignedDuoCookie.parseSignedDuoCookie(secretKey,
                    matcher.group(DUO_COOKIE_GROUP));

            // Parse signed cookie defining the user this application
            // originally requested
            appCookie = SignedDuoCookie.parseSignedDuoCookie(applicationKey,
                    matcher.group(APP_COOKIE_GROUP));

        }

        // Simply return false if signature fails to verify
        catch (GuacamoleException e) {
            logger.debug("Duo signature verification failed.", e);
            return false;
        }

        // Verify neither cookie is expired
        if (duoCookie.isExpired() || appCookie.isExpired()) {
            logger.debug("Duo response contained expired cookie(s).");
            return false;
        }

        // Verify the cookies in the response have the correct types
        if (duoCookie.getType() != SignedDuoCookie.Type.DUO_RESPONSE
         || appCookie.getType() != SignedDuoCookie.Type.APPLICATION) {
            logger.debug("Duo response did not contain correct cookie type(s).");
            return false;
        }

        // Verify integration key matches both cookies
        if (!duoCookie.getIntegrationKey().equals(integrationKey)
         || !appCookie.getIntegrationKey().equals(integrationKey)) {
            logger.debug("Integration key of Duo response is incorrect.");
            return false;
        }

        // Verify both cookies are for the current user
        if (!duoCookie.getUsername().equals(username)
         || !appCookie.getUsername().equals(username)) {
            logger.debug("Username of Duo response is incorrect.");
            return false;
        }

        // All verifications tests pass
        return true;

    }

}
