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

package org.apache.guacamole.auth.cas;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Enumeration;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.auth.cas.conf.ConfigurationService;
import org.apache.guacamole.auth.cas.form.CASTicketField;
import org.apache.guacamole.auth.cas.ticket.TicketValidationService;
import org.apache.guacamole.auth.cas.user.AuthenticatedUser;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing convenience functions for the CAS AuthenticationProvider
 * implementation.
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

    /**
     * Service for retrieving CAS configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Service for validating received ID tickets.
     */
    @Inject
    private TicketValidationService ticketService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<AuthenticatedUser> authenticatedUserProvider;

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An AuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Pull CAS ticket from request if present
        HttpServletRequest request = credentials.getRequest();
        if (request != null) {
            String ticket = request.getParameter(CASTicketField.PARAMETER_NAME);
            if (ticket != null) {
                AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
                AttributePrincipal principal = ticketService.validateTicket(ticket);
                String username = principal.getName();
                credentials.setUsername(username);
                Object credObj = principal.getAttributes().get("credential");
                if (credObj != null) {
                    String clearPass = decryptPassword(credObj.toString());
                    if (clearPass != null && !clearPass.isEmpty())
                        credentials.setPassword(clearPass);
                }
                authenticatedUser.init(username, credentials);
                return authenticatedUser;
            }
        }

        // Request CAS ticket
        throw new GuacamoleInsufficientCredentialsException(
            "LOGIN.INFO_CAS_REDIRECT_PENDING",
            new CredentialsInfo(Arrays.asList(new Field[] {

                // CAS-specific ticket (will automatically redirect the user
                // to the authorization page via JavaScript)
                new CASTicketField(
                    confService.getAuthorizationEndpoint(),
                    confService.getRedirectURI()
                )

            }))
        );

    }

    /**
     * Takes an encrypted string representing a password provided by
     * the CAS ClearPass service and decrypts it using the private
     * key configured for this extension.  Returns null if it is
     * unable to decrypt the password.
     *
     * @param encryptedPassword
     *     A string with the encrypted password provided by the
     *     CAS service.
     *
     * @return
     *     The decrypted password, or null if it is unable to
     *     decrypt the password.
     *
     * @throws GuacamoleException
     *     If unable to get Guacamole configuration data
     */
    private final String decryptPassword(String encryptedPassword)
            throws GuacamoleException {

        // If we get nothing, we return nothing.
        if (encryptedPassword == null || encryptedPassword.isEmpty())
            return null;

        try {

            final Cipher cipher = confService.getClearpassCipher();

            // Decrypt and return a new string.
            final byte[] pass64 = DatatypeConverter.parseBase64Binary(encryptedPassword);
            final byte[] cipherData = cipher.doFinal(pass64);
            return new String(cipherData);
        }
        catch (Throwable t) {
            logger.error("Failed to decrypt the data, password token will not be available.");
            logger.debug("Failed to either convert Base64 or decrypt the password.  CAS Password will not be available inside Guacamole.  Exception is: {}", t);
            return null;
        }

    }

}
