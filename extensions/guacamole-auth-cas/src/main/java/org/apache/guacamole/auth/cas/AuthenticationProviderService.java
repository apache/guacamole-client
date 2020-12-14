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
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.auth.cas.conf.ConfigurationService;
import org.apache.guacamole.auth.cas.form.CASTicketField;
import org.apache.guacamole.auth.cas.ticket.TicketValidationService;
import org.apache.guacamole.auth.cas.user.CASAuthenticatedUser;
import org.apache.guacamole.language.TranslatableMessage;

/**
 * Service providing convenience functions for the CAS AuthenticationProvider
 * implementation.
 */
public class AuthenticationProviderService {

    /**
     * Service for retrieving CAS configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for validating received ID tickets.
     */
    @Inject
    private TicketValidationService ticketService;

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     A CASAuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public CASAuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Pull CAS ticket from request if present
        HttpServletRequest request = credentials.getRequest();
        if (request != null) {
            String ticket = request.getParameter(CASTicketField.PARAMETER_NAME);
            if (ticket != null) {
                return ticketService.validateTicket(ticket, credentials);
            }
        }

        // Request CAS ticket
        throw new GuacamoleInvalidCredentialsException("Invalid login.",
            new CredentialsInfo(Arrays.asList(new Field[] {

                // CAS-specific ticket (will automatically redirect the user
                // to the authorization page via JavaScript)
                new CASTicketField(
                    confService.getAuthorizationEndpoint(),
                    confService.getRedirectURI(),
                    new TranslatableMessage("LOGIN.INFO_CAS_REDIRECT_PENDING")
                )

            }))
        );

    }

}
