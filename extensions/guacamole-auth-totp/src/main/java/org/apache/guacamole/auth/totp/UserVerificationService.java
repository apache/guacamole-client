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

import com.google.common.io.BaseEncoding;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.Map;
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
import org.apache.guacamole.totp.TOTPGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for verifying the identity of a user using TOTP.
 */
public class UserVerificationService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(UserVerificationService.class);

    /**
     * The name of the user attribute which stores the TOTP key.
     */
    private static final String TOTP_KEY_ATTRIBUTE_NAME = "guac-totp-key";

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
     * BaseEncoding instance which decoded/encodes base32.
     */
    private static final BaseEncoding BASE32 = BaseEncoding.base32();

    /**
     * Retrieves the base32-encoded TOTP key associated with user having the
     * given UserContext. If no TOTP key is associated with the user, null is
     * returned.
     *
     * @param context
     *     The UserContext of the user whose TOTP key should be retrieved.
     *
     * @return
     *     The base32-encoded TOTP key associated with user having the given
     *     UserContext, or null if no TOTP key is associated with the user.
     */
    public String getKey(UserContext context){
        Map<String, String> attributes = context.self().getAttributes();
        return attributes.get(TOTP_KEY_ATTRIBUTE_NAME);
    }

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

        // Ignore anonymous users
        String username = authenticatedUser.getIdentifier();
        if (username.equals(AuthenticatedUser.ANONYMOUS_IDENTIFIER))
            return;

        // Ignore users which do not have an associated key
        String encodedKey = getKey(context);
        if (encodedKey == null)
            return;

        // Pull the original HTTP request used to authenticate
        Credentials credentials = authenticatedUser.getCredentials();
        HttpServletRequest request = credentials.getRequest();

        // Retrieve TOTP from request
        String code = request.getParameter(TOTP_PARAMETER_NAME);

        // If no TOTP provided, request one
        if (code == null)
            throw new GuacamoleInsufficientCredentialsException(
                    "LOGIN.INFO_TOTP_REQUIRED", TOTP_CREDENTIALS);

        try {

            // Verify provided TOTP against value produced by generator
            byte[] key = BASE32.decode(encodedKey);
            TOTPGenerator totp = new TOTPGenerator(key, TOTPGenerator.Mode.SHA1, 6);
            if (code.equals(totp.generate()) || code.equals(totp.previous()))
                return;

        }
        catch (InvalidKeyException e) {
            logger.warn("User \"{}\" is associated with an invalid TOTP key.", username);
            logger.debug("TOTP key is not valid.", e);
        }
        catch (IllegalArgumentException e) {
            logger.warn("TOTP key of user \"{}\" is not valid base32.", username);
            logger.debug("TOTP key is not valid base32.", e);
        }

        // Provided code is not valid
        throw new GuacamoleClientException("LOGIN.INFO_TOTP_VERIFICATION_FAILED");

    }

}
