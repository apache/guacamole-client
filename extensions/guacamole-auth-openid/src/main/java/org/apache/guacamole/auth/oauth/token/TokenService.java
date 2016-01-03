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

package org.apache.guacamole.auth.oauth.token;

import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.representation.Form;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.auth.oauth.AuthenticationProviderService;
import org.apache.guacamole.auth.oauth.conf.ConfigurationService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides relatively abstract means of producing authentication tokens from
 * the codes received from OAuth services.
 */
public class TokenService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

    /**
     * Service for retrieving OAuth configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Jersey HTTP client.
     */
    @Inject
    private Client client;

    /**
     * Given an authorization code previously received from the OAuth service
     * via the "code" parameter provided to the redirect URL, retrieves and
     * returns an authentication token.
     *
     * @param code
     *     The value of the "code" parameter received from the OAuth service.
     *
     * @return
     *     The authentication roken response received from the OAuth service.
     *
     * @throws GuacamoleException
     *     If required properties within guacamole.properties cannot be read,
     *     or if an error occurs while contacting the OAuth service.
     */
    public TokenResponse getTokenFromCode(String code)
            throws GuacamoleException {

        try {

            // Generate POST data
            Form form = new Form();
            form.add("code", code);
            form.add("client_id", confService.getClientID());
            form.add("client_secret", confService.getClientSecret());
            form.add("redirect_uri", confService.getRedirectURI());
            form.add("grant_type", "authorization_code");

            // POST code and client information to OAuth token endpoint
            return client.resource(confService.getTokenEndpoint())
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(TokenResponse.class, form);

        }

        // Log any failure reaching the OAuth service
        catch (UniformInterfaceException e) {
            logger.debug("POST to token endpoint failed.", e);
            throw new GuacamoleServerException("Unable to POST to token endpoint.", e);
        }

    }

}
