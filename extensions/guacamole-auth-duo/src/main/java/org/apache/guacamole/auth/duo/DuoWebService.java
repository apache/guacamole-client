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

import com.duosecurity.duoweb.DuoWeb;
import com.duosecurity.duoweb.DuoWebException;
import com.google.inject.Inject;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.duo.conf.ConfigurationService;
import org.apache.guacamole.net.auth.AuthenticatedUser;

/**
 * Service which wraps the DuoWeb Java API, providing predictable behavior and
 * error handling.
 */
public class DuoWebService {

    /**
     * A regular expression which matches a valid signature part of a Duo
     * signed response. A signature part may not contain pipe symbols (which
     * act as delimiters between parts) nor colons (which act as delimiters
     * between signatures).
     */
    private final String SIGNATURE_PART = "[^:|]*";

    /**
     * A regular expression which matches a valid signature within a Duo
     * signed response. Each signature is made up of three distinct parts,
     * separated by pipe symbols.
     */
    private final String SIGNATURE = SIGNATURE_PART + "\\|" + SIGNATURE_PART + "\\|" + SIGNATURE_PART;

    /**
     * A regular expression which matches a valid Duo signed response. Each
     * response is made up of two signatures, separated by a colon.
     */
    private final String RESPONSE = SIGNATURE + ":" + SIGNATURE;

    /**
     * A Pattern which matches valid Duo signed responses. Strings which will
     * be passed to DuoWeb.verifyResponse() MUST be matched against this
     * Pattern. Strings which do not match this Pattern may cause
     * DuoWeb.verifyResponse() to throw unchecked exceptions.
     */
    private final Pattern RESPONSE_PATTERN = Pattern.compile(RESPONSE);

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
     *     invalid, or if an error occurs within the DuoWeb API which prevents
     *     generation of the signed request.
     */
    public String createSignedRequest(AuthenticatedUser authenticatedUser)
        throws GuacamoleException {

        // Retrieve username from externally-authenticated user
        String username = authenticatedUser.getIdentifier();

        // Retrieve Duo-specific keys from configuration
        String ikey = confService.getIntegrationKey();
        String skey = confService.getSecretKey();
        String akey = confService.getApplicationKey();

        // Create signed request for the provided user
        String signedRequest = DuoWeb.signRequest(ikey, skey, akey, username);

        if (DuoWeb.ERR_AKEY.equals(signedRequest))
            throw new GuacamoleServerException("The Duo application key "
                    + "must is not valid. Duo application keys must be at "
                    + "least 40 characters long.");
        
        if (DuoWeb.ERR_IKEY.equals(signedRequest))
            throw new GuacamoleServerException("The provided Duo integration "
                    + "key is not valid. Integration keys must be exactly 20 "
                    + "characters long.");

        if (DuoWeb.ERR_SKEY.equals(signedRequest))
            throw new GuacamoleServerException("The provided Duo secret key "
                    + "is not valid. Secret keys must be exactly 40 "
                    + "characters long.");

        if (DuoWeb.ERR_USER.equals(signedRequest))
            throw new GuacamoleServerException("The provided username is "
                    + "not valid. Duo usernames may not be blank, nor may "
                    + "they contain pipe symbols (\"|\").");

        if (DuoWeb.ERR_UNKNOWN.equals(signedRequest))
            throw new GuacamoleServerException("An unknown error within the "
                    + "DuoWeb API prevented the signed request from being "
                    + "generated.");

        // Return signed request if no error is indicated
        return signedRequest;

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
     *     invalid, or if an error occurs within the DuoWeb API which prevents
     *     validation of the signed response.
     */
    public boolean isValidSignedResponse(AuthenticatedUser authenticatedUser,
            String signedResponse) throws GuacamoleException {

        // Verify signature response format will not cause
        // DuoWeb.verifyResponse() to fail with unchecked exceptions
        Matcher responseMatcher = RESPONSE_PATTERN.matcher(signedResponse);
        if (!responseMatcher.matches())
            throw new GuacamoleClientException("Invalid Duo response format.");

        // Retrieve username from externally-authenticated user
        String username = authenticatedUser.getIdentifier();

        // Retrieve Duo-specific keys from configuration
        String ikey = confService.getIntegrationKey();
        String skey = confService.getSecretKey();
        String akey = confService.getApplicationKey();

        // Verify validity of signed response
        String verifiedUsername;
        try {
            verifiedUsername = DuoWeb.verifyResponse(ikey, skey, akey,
                    signedResponse);
        }

        // Rethrow any errors as appropriate GuacamoleExceptions
        catch (IOException e) {
            throw new GuacamoleClientException("Decoding of Duo response "
                    + "failed: Invalid base64 content.", e);
        }
        catch (NumberFormatException e) {
            throw new GuacamoleClientException("Decoding of Duo response "
                    + "failed: Invalid expiry timestamp.", e);
        }
        catch (InvalidKeyException e) {
            throw new GuacamoleServerException("Unable to produce HMAC "
                    + "signature: " + e.getMessage(), e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new GuacamoleServerException("Environment is missing "
                    + "support for producing HMAC-SHA1 signatures.", e);
        }
        catch (DuoWebException e) {
            throw new GuacamoleClientException("Duo response verification "
                    + "failed: " + e.getMessage(), e);
        }

        // Signed response is valid iff the associated username matches the
        // user's username
        return username.equals(verifiedUsername);

    }

}
