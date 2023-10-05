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

import com.duosecurity.Client;
import com.duosecurity.exception.DuoException;
import com.duosecurity.model.Token;
import com.google.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.duo.conf.ConfigurationService;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableGuacamoleClientException;
import org.apache.guacamole.language.TranslatableGuacamoleInsufficientCredentialsException;
import org.apache.guacamole.language.TranslatableMessage;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;

/**
 * Service for verifying the identity of a user against Duo.
 */
public class UserVerificationService {

    /**
     * The name of the parameter which Duo will return in it's GET call-back
     * that contains the code that the client will use to generate a token.
     */
    private static final String DUO_CODE_PARAMETER_NAME = "duo_code";
    
    /**
     * The name of the parameter that will be used in the GET call-back that
     * contains the session state.
     */
    private static final String DUO_STATE_PARAMETER_NAME = "state";
    
    /**
     * The value that will be returned in the token if Duo authentication
     * was successful.
     */
    private static final String DUO_TOKEN_SUCCESS_VALUE = "ALLOW";
    
    /**
     * Service for retrieving Duo configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * The authentication session manager that temporarily stores in-progress
     * authentication attempts.
     */
    @Inject
    private DuoAuthenticationSessionManager duoSessionManager;

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
        
        String username = authenticatedUser.getIdentifier();

        try {

        // Set up the Duo Client
        Client duoClient = new Client.Builder(
                confService.getClientId(),
                confService.getClientSecret(),
                confService.getAPIHostname(),
                confService.getRedirectUrl().toString())
                .build();
        
            duoClient.healthCheck();
        
        
        // Retrieve signed Duo Code and State from the request
        String duoCode = request.getParameter(DUO_CODE_PARAMETER_NAME);
        String duoState = request.getParameter(DUO_STATE_PARAMETER_NAME);

        // If no code or state is received, assume Duo MFA redirect has not occured and do it.
        if (duoCode == null || duoState == null) {

            // Get a new session state from the Duo client
            duoState = duoClient.generateState();
            
            // Add this session 
            duoSessionManager.defer(new DuoAuthenticationSession(confService.getAuthTimeout(), duoState, username), duoState);

            // Request additional credentials
            throw new TranslatableGuacamoleInsufficientCredentialsException(
                "Verification using Duo is required before authentication "
                + "can continue.", "LOGIN.INFO_DUO_AUTH_REQUIRED",
                new CredentialsInfo(Collections.singletonList(
                    new RedirectField(
                            DUO_CODE_PARAMETER_NAME,
                            new URI(duoClient.createAuthUrl(username, duoState)),
                            new TranslatableMessage("LOGIN.INFO_DUO_REDIRECT_PENDING")
                    )
                ))
            );

        }

        // Retrieve the deferred authenticaiton attempt
        DuoAuthenticationSession duoSession = duoSessionManager.resume(duoState);
        
        // Get the token from the DuoClient using the code and username, and check status
        Token token = duoClient.exchangeAuthorizationCodeFor2FAResult(duoCode, duoSession.getUsername());
        if (token == null 
                || token.getAuth_result() == null 
                || !DUO_TOKEN_SUCCESS_VALUE.equals(token.getAuth_result().getStatus()))
            throw new TranslatableGuacamoleClientException("Provided Duo "
                    + "validation code is incorrect.",
                    "LOGIN.INFO_DUO_VALIDATION_CODE_INCORRECT");

        }
        catch (DuoException e) {
            throw new GuacamoleServerException("Duo Client error.", e);
        }
        catch (URISyntaxException e) {
            throw new GuacamoleServerException("Error creating URI from Duo Authentication URL.", e);
        }
    }

}
