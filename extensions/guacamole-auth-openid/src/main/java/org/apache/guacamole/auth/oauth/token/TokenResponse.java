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

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The response produced from a successful request to the token endpoint of an
 * OAuth service.
 */
public class TokenResponse {

    /**
     * An arbitrary access token which can be used for future requests against
     * the API associated with the OAuth service.
     */
    private String accessToken;

    /**
     * The type of token present. This will always be "Bearer".
     */
    private String tokenType;

    /**
     * The number of seconds the access token will remain valid.
     */
    private int expiresIn;

    /**
     * A JWT (JSON Web Token) which containing identity information which has
     * been cryptographically signed.
     */
    private String idToken;

    /**
     * Returns an arbitrary access token which can be used for future requests
     * against the API associated with the OAuth service.
     *
     * @return
     *     An arbitrary access token provided by the OAuth service.
     */
    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the arbitrary access token which can be used for future requests
     * against the API associated with the OAuth service.
     *
     * @param accessToken
     *     The arbitrary access token provided by the OAuth service.
     */
    @JsonProperty("access_token")
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Returns the type of token present in this response. This should always
     * be "Bearer".
     *
     * @return
     *     The type of token present in this response.
     */
    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the type of token present in this response. This should always be
     * "Bearer".
     *
     * @param tokenType
     *     The type of token present in this response, which should be
     *     "Bearer".
     */
    @JsonProperty("token_type")
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Returns the number of seconds the access token within this response will
     * remain valid.
     *
     * @return
     *     The number of seconds the access token within this response will
     *     remain valid.
     */
    @JsonProperty("expires_in")
    public int getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the number of seconds the access token within this response will
     * remain valid.
     *
     * @param expiresIn
     *     The number of seconds the access token within this response will
     *     remain valid.
     */
    @JsonProperty("expires_in")
    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Returns a JWT (JSON Web Token) containing identity information which has
     * been cryptographically signed by the OAuth service.
     *
     * @return
     *     A JWT (JSON Web Token) containing identity information which has
     *     been cryptographically signed by the OAuth service.
     */
    @JsonProperty("id_token")
    public String getIdToken() {
        return idToken;
    }

    /**
     * Sets the JWT (JSON Web Token) containing identity information which has
     * been cryptographically signed by the OAuth service.
     *
     * @param idToken
     *     A JWT (JSON Web Token) containing identity information which has
     *     been cryptographically signed by the OAuth service.
     */
    @JsonProperty("id_token")
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

}
