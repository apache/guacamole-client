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

package org.apache.guacamole.auth.openid.form;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableMessage;

/**
 * Field definition which represents the token returned by an OpenID Connect
 * service.
 */
public class TokenField extends RedirectField {

    /**
     * The standard HTTP parameter which will be included within the URL by all
     * OpenID services upon successful authentication and redirect.
     */
    public static final String PARAMETER_NAME = "id_token";

    /**
     * Creates a new field which requests authentication via OpenID connect.
     * Successful authentication at the OpenID Connect service will result in
     * the client being redirected to the specified redirect URI. The OpenID
     * token will be embedded in the fragment (the part following the hash
     * symbol) of that URI, which the JavaScript side of this extension will
     * move to the query parameters.
     *
     * @param authorizationEndpoint
     *     The full URL of the endpoint accepting OpenID authentication
     *     requests.
     *
     * @param scope
     *     The space-delimited list of OpenID scopes to request from the
     *     identity provider, such as "openid" or "openid email profile".
     *
     * @param clientID
     *     The ID of the OpenID client. This is normally determined ahead of
     *     time by the OpenID service through some manual credential request
     *     procedure.
     *
     * @param redirectURI
     *     The URI that the OpenID service should redirect to upon successful
     *     authentication.
     *
     * @param nonce
     *     A random string unique to this request. To defend against replay
     *     attacks, this value must cease being valid after its first use.
     * 
     * @param redirectMessage
     *     The message that will be displayed to the user during redirect.  This
     *     will be processed through Guacamole's translation system.
     */
    public TokenField(URI authorizationEndpoint, String scope,
            String clientID, URI redirectURI, String nonce,
            TranslatableMessage redirectMessage) {

        super(PARAMETER_NAME, UriBuilder.fromUri(authorizationEndpoint)
                .queryParam("scope", scope)
                .queryParam("response_type", "id_token")
                .queryParam("client_id", clientID)
                .queryParam("redirect_uri", redirectURI)
                .queryParam("nonce", nonce)
                .build(),
                redirectMessage);

    }

}
