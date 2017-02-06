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
import java.util.Collections;
import org.apache.guacamole.auth.radius.user.AuthenticatedUser;
import org.apache.guacamole.auth.radius.form.RadiusChallengeResponseField;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.jradius.exception.UnknownAttributeException;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.AccessAccept;
import net.jradius.packet.AccessChallenge;
import net.jradius.packet.AccessReject;
import net.jradius.packet.AccessRequest;
import net.jradius.packet.AccessResponse;
import net.jradius.packet.attribute.RadiusAttribute;

/**
 * Service providing convenience functions for the RADIUS AuthenticationProvider
 * implementation.
 *
 * @author Michael Jumper
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

        // Attempt bind
        RadiusPacket radPack;
        try {
            radPack = radiusService.authenticate(credentials.getUsername(),
                                                credentials.getPassword());
        }
        catch (GuacamoleException e) {
            logger.error("Cannot configure RADIUS server: {}", e.getMessage());
            logger.debug("Error configuring RADIUS server.", e);
            radPack = null;
        }

        // If configure fails, permission to login is denied
        if (radPack == null) {
            logger.debug("Nothing in the RADIUS packet.");
            throw new GuacamoleInvalidCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);
        }
        else if (radPack instanceof AccessReject) {
            logger.debug("Login has been rejected by RADIUS server.");
            throw new GuacamoleInvalidCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);
        }
        else if (radPack instanceof AccessChallenge) {
            try {
                String replyMsg = radPack.getAttributeValue("Reply-Message").toString();
                String radState = radPack.getAttributeValue("State").toString();
                logger.debug("RADIUS sent challenge response: {}", replyMsg);
                logger.debug("RADIUS sent state: {}", radState);
                Field radiusResponseField = new RadiusChallengeResponseField(replyMsg, radState);
                CredentialsInfo responseCredentials = new CredentialsInfo(Collections.singletonList(radiusResponseField));
                throw new GuacamoleInsufficientCredentialsException("LOGIN.INFO_RADIUS_ADDL_REQUIRED", responseCredentials);
           }
           catch(UnknownAttributeException e) {
                logger.error("Error in talks with RADIUS server.");
                logger.debug("RADIUS challenged by didn't provide right attributes.");
                throw new GuacamoleInvalidCredentialsException("Authentication error.", CredentialsInfo.USERNAME_PASSWORD);
           }
        }
        else if (radPack instanceof AccessAccept) {
            try {

                // Return AuthenticatedUser if bind succeeds
                AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
                authenticatedUser.init(credentials);
                return authenticatedUser;
            }
            finally {
                radiusService.disconnect();
            }
        }
        else
            throw new GuacamoleInvalidCredentialsException("Unknown error trying to authenticate.", CredentialsInfo.USERNAME_PASSWORD);

    }

}
