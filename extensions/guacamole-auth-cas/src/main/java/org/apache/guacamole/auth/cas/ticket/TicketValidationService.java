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
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.cas.conf.ConfigurationService;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;

/**
 * Service for validating ID tickets forwarded to us by the client, verifying
 * that they did indeed come from the CAS service.
 */
public class TicketValidationService {

    /**
     * Service for retrieving CAS configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Validates and parses the given ID ticket, returning the username contained
     * therein, as defined by the username claim type given in
     * guacamole.properties. If the username claim type is missing or the ID
     * ticket is invalid, an exception is thrown instead.
     *
     * @param ticket
     *     The ID ticket to validate and parse.
     *
     * @return
     *     The username contained within the given ID ticket.
     *
     * @throws GuacamoleException
     *     If the ID ticket is not valid, the username claim type is missing, or
     *     guacamole.properties could not be parsed.
     */
    public String processUsername(String ticket) throws GuacamoleException {
        AttributePrincipal principal = null;

        // Retrieve the configured CAS URL and establish a ticket validator
        String casServerUrl = confService.getAuthorizationEndpoint();
        Cas20ProxyTicketValidator sv = new Cas20ProxyTicketValidator(casServerUrl);
        sv.setAcceptAnyProxy(true);
        try {
            String confRedirectURI = confService.getRedirectURI();
            Assertion a = sv.validate(ticket, confRedirectURI);
            principal = a.getPrincipal();
        } 
        catch (TicketValidationException e) {
            throw new GuacamoleException("Ticket validation failed.", e);
        }

        return principal.getName();

    }

}
