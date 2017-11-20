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

package org.apache.guacamole.auth.totp;

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.TextField;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;

/**
 * Service for verifying the identity of a user using TOTP.
 */
public class UserVerificationService {

    /**
     * The name of the HTTP parameter which will contain the TOTP code provided
     * by the user to verify their identity.
     */
    private static final String TOTP_PARAMETER_NAME = "guac-totp";

    /**
     * The field which should be exposed to the user to request that they
     * provide their TOTP code.
     */
    private static final Field TOTP_FIELD = new TextField(TOTP_PARAMETER_NAME);

    /**
     * CredentialsInfo object describing the credentials expected for a user
     * who has verified their identity with TOTP.
     */
    private static final CredentialsInfo TOTP_CREDENTIALS = new CredentialsInfo(
            Collections.singletonList(TOTP_FIELD)
    );

    /**
     * Verifies the identity of the given user using TOTP. If a authentication
     * code from the user's TOTP device has not already been provided, a code is
     * requested in the form of additional expected credentials. Any provided
     * code is cryptographically verified. If no code is present, or the
     * received code is invalid, an exception is thrown.
     *
     * @param context
     *     The UserContext provided for the user by another authentication
     *     extension.
     *
     * @param authenticatedUser
     *     The user whose identity should be verified using TOTP.
     *
     * @throws GuacamoleException
     *     If required TOTP-specific configuration options are missing or
     *     malformed, or if the user's identity cannot be verified.
     */
    public void verifyIdentity(UserContext context,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // Pull the original HTTP request used to authenticate
        Credentials credentials = authenticatedUser.getCredentials();
        HttpServletRequest request = credentials.getRequest();

        // Ignore anonymous users
        if (authenticatedUser.getIdentifier().equals(AuthenticatedUser.ANONYMOUS_IDENTIFIER))
            return;

        // Retrieve TOTP from request
        String totp = request.getParameter(TOTP_PARAMETER_NAME);

        // If no TOTP provided, request one
        if (totp == null)
            throw new GuacamoleInsufficientCredentialsException(
                    "LOGIN.INFO_TOTP_REQUIRED", TOTP_CREDENTIALS);

        // FIXME: Hard-coded code
        if (!totp.equals("123456"))
            throw new GuacamoleClientException("LOGIN.INFO_TOTP_VERIFICATION_FAILED");

    }

}
