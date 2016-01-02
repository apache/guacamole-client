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

package org.apache.guacamole.auth.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.glyptodon.guacamole.form.Field;

/**
 * Field definition which represents the code returned by an OAuth service.
 * Within the user interface, this will be rendered as an appropriate "Log in
 * with ..." button which links to the OAuth service.
 */
public class OAuthCodeField extends Field {

    /**
     * The standard HTTP parameter which will be included within the URL by all
     * OAuth services upon successful authentication and redirect.
     */
    private static final String OAUTH_CODE_PARAMETER_NAME = "code";

    /**
     * The full URI which the field should link to.
     */
    private final String authorizationURI;

    /**
     * Creates a new OAuth "code" field which links to the given OAuth service
     * using the provided client ID. Successful authentication at the OAuth
     * service will result in the client being redirected to the specified
     * redirect URI. The OAuth code will be embedded in the query parameters of
     * that URI.
     *
     * @param authorizationEndpoint
     *     The full URL of the endpoint accepting OAuth authentication
     *     requests.
     *
     * @param clientID
     *     The ID of the OAuth client. This is normally determined ahead of
     *     time by the OAuth service through some manual credential request
     *     procedure.
     *
     * @param redirectURI
     *     The URI that the OAuth service should redirect to upon successful
     *     authentication.
     */
    public OAuthCodeField(String authorizationEndpoint, String clientID,
            String redirectURI) {

        // Init base field properties
        super(OAUTH_CODE_PARAMETER_NAME, "OAUTH_CODE");

        // Build authorization URI from given values
        try {
            this.authorizationURI = authorizationEndpoint
                    + "?scope=openid%20email%20profile"
                    + "&response_type=code"
                    + "&client_id=" + URLEncoder.encode(clientID, "UTF-8")
                    + "&redirect_uri=" + URLEncoder.encode(redirectURI, "UTF-8");
        }

        // Java is required to provide UTF-8 support
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }

    }

    /**
     * Returns the full URI that this field should link to when a new code
     * needs to be obtained from the OAuth service.
     *
     * @return
     *     The full URI that this field should link to.
     */
    public String getAuthorizationURI() {
        return authorizationURI;
    }

}
