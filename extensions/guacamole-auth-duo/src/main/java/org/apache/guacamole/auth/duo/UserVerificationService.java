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

package org.apache.guacamole.auth.duo;

import com.google.inject.Inject;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.duo.api.DuoService;
import org.apache.guacamole.auth.duo.conf.ConfigurationService;
import org.apache.guacamole.auth.duo.form.DuoSignedResponseField;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.language.TranslatableGuacamoleClientException;
import org.apache.guacamole.language.TranslatableGuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;

/**
 * Service for verifying the identity of a user against Duo.
 */
public class UserVerificationService {

    /**
     * Service for retrieving Duo configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for verifying users against Duo.
     */
    @Inject
    private DuoService duoService;

    /**
     * Verifies the identity of the given user via the Duo multi-factor
     * authentication service. If a signed response from Duo has not already
     * been provided, a signed response from Duo is requested in the
     * form of additional expected credentials. Any provided signed response
     * is cryptographically verified. If no signed response is present, or the
     * signed response is invalid, an exception is thrown.
     *
     * @param authenticatedUser
     *     The user whose identity should be verified against Duo.
     *
     * @throws GuacamoleException
     *     If required Duo-specific configuration options are missing or
     *     malformed, or if the user's identity cannot be verified.
     */
    public void verifyAuthenticatedUser(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {

        // Pull the original HTTP request used to authenticate
        Credentials credentials = authenticatedUser.getCredentials();
        HttpServletRequest request = credentials.getRequest();

        // Ignore anonymous users
        if (authenticatedUser.getIdentifier().equals(AuthenticatedUser.ANONYMOUS_IDENTIFIER))
            return;

        // Retrieve signed Duo response from request
        String signedResponse = request.getParameter(DuoSignedResponseField.PARAMETER_NAME);

        // If no signed response, request one
        if (signedResponse == null) {

            // Create field which requests a signed response from Duo that
            // verifies the identity of the given user via the configured
            // Duo API endpoint
            Field signedResponseField = new DuoSignedResponseField(
                    confService.getAPIHostname(),
                    duoService.createSignedRequest(authenticatedUser));

            // Create an overall description of the additional credentials
            // required to verify identity
            CredentialsInfo expectedCredentials = new CredentialsInfo(
                        Collections.singletonList(signedResponseField));

            // Request additional credentials
            throw new TranslatableGuacamoleInsufficientCredentialsException(
                    "Verification using Duo is required before authentication "
                    + "can continue.", "LOGIN.INFO_DUO_AUTH_REQUIRED",
                    expectedCredentials);

        }

        // If signed response does not verify this user's identity, abort auth
        if (!duoService.isValidSignedResponse(authenticatedUser, signedResponse))
            throw new TranslatableGuacamoleClientException("Provided Duo "
                    + "validation code is incorrect.",
                    "LOGIN.INFO_DUO_VALIDATION_CODE_INCORRECT");

    }

}
