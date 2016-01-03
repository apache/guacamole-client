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

package org.apache.guacamole.auth.oauth.conf;

import com.google.inject.Inject;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information regarding the OAuth service.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the authorization endpoint (URI) of the OAuth service as
     * configured with guacamole.properties.
     *
     * @return
     *     The authorization endpoint of the OAuth service, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the authorization
     *     endpoint property is missing.
     */
    public String getAuthorizationEndpoint() throws GuacamoleException {
        return environment.getRequiredProperty(OAuthGuacamoleProperties.OAUTH_AUTHORIZATION_ENDPOINT);
    }

    /**
     * Returns the token endpoint (URI) of the OAuth service as configured with
     * guacamole.properties.
     *
     * @return
     *     The token endpoint of the OAuth service, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the authorization
     *     endpoint property is missing.
     */
    public String getTokenEndpoint() throws GuacamoleException {
        return environment.getRequiredProperty(OAuthGuacamoleProperties.OAUTH_TOKEN_ENDPOINT);
    }

    /**
     * Returns the OAuth client ID which should be submitted to the OAuth
     * service when necessary, as configured with guacamole.properties. This
     * value is typically provided by the OAuth service when OAuth credentials
     * are generated for your application.
     *
     * @return
     *     The client ID to use when communicating with the OAuth service,
     *     as configured with guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the client ID
     *     property is missing.
     */
    public String getClientID() throws GuacamoleException {
        return environment.getRequiredProperty(OAuthGuacamoleProperties.OAUTH_CLIENT_ID);
    }

    /**
     * Returns the OAuth client secret which should be submitted to the OAuth
     * service when necessary, as configured with guacamole.properties. This
     * value is typically provided by the OAuth service when OAuth credentials
     * are generated for your application.
     *
     * @return
     *     The client secret to use when communicating with the OAuth service,
     *     as configured with guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the client secret
     *     property is missing.
     */
    public String getClientSecret() throws GuacamoleException {
        return environment.getRequiredProperty(OAuthGuacamoleProperties.OAUTH_CLIENT_SECRET);
    }

    /**
     * Returns the URI that the OAuth service should redirect to after
     * the authentication process is complete, as configured with
     * guacamole.properties. This must be the full URL that a user would enter
     * into their browser to access Guacamole.
     *
     * @return
     *     The client secret to use when communicating with the OAuth service,
     *     as configured with guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the client secret
     *     property is missing.
     */
    public String getRedirectURI() throws GuacamoleException {
        return environment.getRequiredProperty(OAuthGuacamoleProperties.OAUTH_REDIRECT_URI);
    }

}
