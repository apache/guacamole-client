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

package org.apache.guacamole.auth.openid.conf;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information regarding the OpenID
 * service.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the authorization endpoint (URI) of the OpenID service as
     * configured with guacamole.properties.
     *
     * @return
     *     The authorization endpoint of the OpenID service, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the authorization
     *     endpoint property is missing.
     */
    public String getAuthorizationEndpoint() throws GuacamoleException {
        return environment.getRequiredProperty(OpenIDGuacamoleProperties.OPENID_AUTHORIZATION_ENDPOINT);
    }

    /**
     * Returns the OpenID client ID which should be submitted to the OpenID
     * service when necessary, as configured with guacamole.properties. This
     * value is typically provided by the OpenID service when OpenID credentials
     * are generated for your application.
     *
     * @return
     *     The client ID to use when communicating with the OpenID service,
     *     as configured with guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the client ID
     *     property is missing.
     */
    public String getClientID() throws GuacamoleException {
        return environment.getRequiredProperty(OpenIDGuacamoleProperties.OPENID_CLIENT_ID);
    }

    /**
     * Returns the URI that the OpenID service should redirect to after
     * the authentication process is complete, as configured with
     * guacamole.properties. This must be the full URL that a user would enter
     * into their browser to access Guacamole.
     *
     * @return
     *     The client secret to use when communicating with the OpenID service,
     *     as configured with guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the redirect URI
     *     property is missing.
     */
    public String getRedirectURI() throws GuacamoleException {
        return environment.getRequiredProperty(OpenIDGuacamoleProperties.OPENID_REDIRECT_URI);
    }

    /**
     * Returns the issuer to expect for all received ID tokens, as configured
     * with guacamole.properties.
     *
     * @return
     *     The issuer to expect for all received ID tokens, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the issuer property
     *     is missing.
     */
    public String getIssuer() throws GuacamoleException {
        return environment.getRequiredProperty(OpenIDGuacamoleProperties.OPENID_ISSUER);
    }

    /**
     * Returns the endpoint (URI) of the JWKS service which defines how
     * received ID tokens (JWTs) shall be validated, as configured with
     * guacamole.properties.
     *
     * @return
     *     The endpoint (URI) of the JWKS service which defines how received ID
     *     tokens (JWTs) shall be validated, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the JWKS endpoint
     *     property is missing.
     */
    public String getJWKSEndpoint() throws GuacamoleException {
        return environment.getRequiredProperty(OpenIDGuacamoleProperties.OPENID_JWKS_ENDPOINT);
    }

    /**
     * Returns the claim type which contains the authenticated user's username
     * within any valid JWT, as configured with guacamole.properties.
     *
     * @return
     *     The claim type which contains the authenticated user's username
     *     within any valid JWT, as configured with guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the username claim
     *     type property is missing.
     */
    public String getUsernameClaimType() throws GuacamoleException {
        return environment.getRequiredProperty(OpenIDGuacamoleProperties.OPENID_USERNAME_CLAIM_TYPE);
    }

}
