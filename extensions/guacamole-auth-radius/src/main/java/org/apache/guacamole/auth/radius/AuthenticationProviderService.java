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

package org.apache.guacamole.auth.radius;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.auth.radius.user.AuthenticatedUser;
import org.apache.guacamole.auth.radius.form.RadiusChallengeResponseField;
import org.apache.guacamole.auth.radius.form.RadiusStateField;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.jradius.dictionary.Attr_State;
import net.jradius.dictionary.Attr_ReplyMessage;
import net.jradius.exception.UnknownAttributeException;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.AccessAccept;
import net.jradius.packet.AccessChallenge;
import net.jradius.packet.AccessReject;
import net.jradius.packet.AccessRequest;
import net.jradius.packet.AccessResponse;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.packet.attribute.RadiusAttribute;

/**
 * Service providing convenience functions for the RADIUS AuthenticationProvider
 * implementation.
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

    /**
     * Service for creating and managing connections to RADIUS servers.
     */
    @Inject
    private RadiusConnectionService radiusService;

    /**
     * Service for retrieving RADIUS server configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<AuthenticatedUser> authenticatedUserProvider;

    /**
     * Returns the expected credentials from a RADIUS challenge.
     *
     * @param challengePacket
     *     The AccessChallenge RadiusPacket received from the RADIUS 
     *     server.
     *
     * @return
     *     A CredentialsInfo object that represents fields that need to
     *     be presented to the user in order to complete authentication.
     *     One of these must be the RADIUS state.
     */
    private CredentialsInfo getRadiusChallenge(RadiusPacket challengePacket) {

        // Try to get the state attribute - if it's not there, we have a problem
        RadiusAttribute stateAttr = challengePacket.findAttribute(Attr_State.TYPE);
        if (stateAttr == null) {
            logger.error("Something went wrong, state attribute not present.");
            logger.debug("State Attribute turned up null, which shouldn't happen in AccessChallenge.");
            return null;
        }

        // We need to get the reply message so we know what to ask the user
        RadiusAttribute replyAttr = challengePacket.findAttribute(Attr_ReplyMessage.TYPE);
        if (replyAttr == null) {
            logger.error("No reply message received from the server.");
            logger.debug("Expecting a Attr_ReplyMessage attribute on this packet, and did not get one.");
            return null;
        }

        // We have the required attributes - convert to strings and then generate the additional login box/field
        String replyMsg = replyAttr.toString();
        String radiusState = new String(stateAttr.getValue().getBytes());
        Field radiusResponseField = new RadiusChallengeResponseField(replyMsg);
        Field radiusStateField = new RadiusStateField(radiusState);

        // Return the CredentialsInfo object that has the state and the expected response.
        return new CredentialsInfo(Arrays.asList(radiusResponseField,radiusStateField));
    }

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

        // Ignore anonymous users
        if (credentials.getUsername() == null || credentials.getUsername().isEmpty())
            return null;

        // Password is required
        if (credentials.getPassword() == null || credentials.getPassword().isEmpty())
            return null;

        // Grab the HTTP Request from the credentials object
        HttpServletRequest request = credentials.getRequest();

        // Try to get parameters to see if this is a post-challenge attempt
        String challengeResponse = request.getParameter(RadiusChallengeResponseField.PARAMETER_NAME);

        // RadiusPacket object to store response from server.
        RadiusPacket radPack;

        // We do not have a challenge response, proceed with username/password authentication.
        if (challengeResponse == null) {

            // Attempt RADIUS authentication with username/password.
            try {
                radPack = radiusService.authenticate(credentials.getUsername(),
                                                credentials.getPassword());
            }
            catch (GuacamoleException e) {
                logger.error("Cannot configure RADIUS server: {}", e.getMessage());
                logger.debug("Error configuring RADIUS server.", e);
                throw new GuacamoleInvalidCredentialsException("Authentication error.", CredentialsInfo.USERNAME_PASSWORD);
            }

            // If no RadiusPacket is returned, we've encountered an error.
            if (radPack == null) {
                logger.debug("Nothing in the RADIUS packet.");
                throw new GuacamoleInvalidCredentialsException("Authentication error.", CredentialsInfo.USERNAME_PASSWORD);
            }

            // If we get back an AccessReject packet, login is denied.
            else if (radPack instanceof AccessReject) {
                logger.debug("Login has been rejected by RADIUS server.");
                throw new GuacamoleInvalidCredentialsException("Authentication failed.", CredentialsInfo.USERNAME_PASSWORD);
            }

            // If we receive an AccessChallenge package, the server needs more information -
            // We create a new form/field with the challenge message.
            else if (radPack instanceof AccessChallenge) {
                CredentialsInfo expectedCredentials = getRadiusChallenge(radPack);

                if (expectedCredentials == null)
                    throw new GuacamoleInvalidCredentialsException("Authentication error.", CredentialsInfo.USERNAME_PASSWORD);

                throw new GuacamoleInsufficientCredentialsException("LOGIN.INFO_RADIUS_ADDL_REQUIRED", expectedCredentials);
            }

            // If we receive AccessAccept, authentication has succeeded
            else if (radPack instanceof AccessAccept) {
                try {
                    AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
                    authenticatedUser.init(credentials);
                    return authenticatedUser;
                }
                finally {
                    radiusService.disconnect();
                }
            }

            // Something unanticipated happened, so we panic and go back to login.
            else {
                logger.error("Unexpected failure authenticating with RADIUS server.");
                throw new GuacamoleInvalidCredentialsException("Unknown error trying to authenticate.", CredentialsInfo.USERNAME_PASSWORD);
            }
        }

        // The user responded to the challenge, send that back to the server
        else {

            // Attempt to authenticate with response to challenge.
            try {
                radPack = radiusService.authenticate(credentials.getUsername(),
                                                     request.getParameter(RadiusStateField.PARAMETER_NAME),
                                                     challengeResponse);
            }
            catch (GuacamoleException e) {
                logger.error("Cannot configure RADIUS server: {}", e.getMessage());
                logger.debug("Error configuring RADIUS server.", e);
                throw new GuacamoleInvalidCredentialsException("Authentication error.", CredentialsInfo.USERNAME_PASSWORD);
            }
            finally {
                radiusService.disconnect();
            }

            // Check the server response, see if we get accepted or not
            if (radPack instanceof AccessAccept) {
                AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
                authenticatedUser.init(credentials);
                return authenticatedUser;
            }

            // Authentication failed
            else {
                logger.warn("RADIUS Challenge/Response authentication failed.");
                logger.debug("Received something other than AccessAccept packet from the RADIUS server.");
                throw new GuacamoleInvalidCredentialsException("Authentication failed.", CredentialsInfo.USERNAME_PASSWORD);
            }
        }
    }

}
