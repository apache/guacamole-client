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

package org.apache.guacamole.auth.cas.ticket;

import com.google.inject.Inject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.Charset;
import javax.xml.bind.DatatypeConverter;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.cas.conf.ConfigurationService;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for validating ID tickets forwarded to us by the client, verifying
 * that they did indeed come from the CAS service.
 */
public class TicketValidationService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TicketValidationService.class);

    /**
     * Service for retrieving CAS configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Validates and parses the given ID ticket, returning the username
     * provided by the CAS server in the ticket.  If the
     * ticket is invalid an exception is thrown.
     *
     * @param ticket
     *     The ID ticket to validate and parse.
     *
     * @param credentials
     *     The Credentials object to store retrieved username and
     *     password values in.
     *
     * @return
     *     The username derived from the ticket.
     *
     * @throws GuacamoleException
     *     If the ID ticket is not valid or guacamole.properties could
     *     not be parsed.
     */
    public String validateTicket(String ticket, Credentials credentials) throws GuacamoleException {

        // Retrieve the configured CAS URL, establish a ticket validator,
        // and then attempt to validate the supplied ticket.  If that succeeds,
        // grab the principal returned by the validator.
        String casServerUrl = confService.getAuthorizationEndpoint();
        Cas20ProxyTicketValidator validator = new Cas20ProxyTicketValidator(casServerUrl);
        validator.setAcceptAnyProxy(true);
        validator.setEncoding("UTF-8");
        try {
            String confRedirectURI = confService.getRedirectURI();
            Assertion a = validator.validate(ticket, confRedirectURI);
            AttributePrincipal principal =  a.getPrincipal();

            // Retrieve username and set the credentials.
            String username = principal.getName();
            if (username != null)
                credentials.setUsername(username);

            // Retrieve password, attempt decryption, and set credentials.
            Object credObj = principal.getAttributes().get("credential");
            if (credObj != null) {
                String clearPass = decryptPassword(credObj.toString());
                if (clearPass != null && !clearPass.isEmpty())
                    credentials.setPassword(clearPass);
            }

            return username;

        } 
        catch (TicketValidationException e) {
            throw new GuacamoleException("Ticket validation failed.", e);
        }
        catch (Throwable t) {
            logger.error("Error validating ticket with CAS server: {}", t.getMessage());
            throw new GuacamoleInvalidCredentialsException("CAS login failed.", CredentialsInfo.USERNAME_PASSWORD);
        }

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
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            logger.warn("No or empty encrypted password, no password will be available.");
            return null;
        }

        final PrivateKey clearpassKey = confService.getClearpassKey();
        if (clearpassKey == null) {
            logger.debug("No private key available to decrypt password.");
            return null;
        }

        try {

            final Cipher cipher = Cipher.getInstance(clearpassKey.getAlgorithm());

            if (cipher == null)
                throw new GuacamoleServerException("Failed to initialize cipher object with private key.");

            // Initialize the Cipher in decrypt mode.
            cipher.init(Cipher.DECRYPT_MODE, clearpassKey);

            // Decode and decrypt, and return a new string.
            final byte[] pass64 = DatatypeConverter.parseBase64Binary(encryptedPassword);
            final byte[] cipherData = cipher.doFinal(pass64);
            return new String(cipherData, Charset.forName("UTF-8"));

        }
        catch (BadPaddingException e) {
            throw new GuacamoleServerException("Bad padding when decrypting cipher data.", e);
        }
        catch (IllegalBlockSizeException e) {
            throw new GuacamoleServerException("Illegal block size while opening private key.", e);
        }
        catch (InvalidKeyException e) {
            throw new GuacamoleServerException("Specified private key for ClearPass decryption is invalid.", e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new GuacamoleServerException("Unexpected algorithm for the private key.", e);
        }
        catch (NoSuchPaddingException e) {
            throw new GuacamoleServerException("No such padding trying to initialize cipher with private key.", e);
        }

    }

}
