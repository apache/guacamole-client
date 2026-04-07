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

package org.apache.guacamole.auth.openid;

import org.apache.guacamole.net.auth.AuthenticationSession;

/**
 * Representation of an in-progress OpenID authentication attempt.
 */
public class OpenIDAuthenticationSession extends AuthenticationSession {
    /**
     * The PKCE challenge verifier.
     */
    private String verifier = null;

    /**
     * The redirect URI used by the identity provide.
     */
    private String redirect_uri = null;
    
    /**
     * The code returned by the identity provider use to exchange for a token
     */
    private String code = null;

    /**
     * Creates a new AuthenticationSession representing an in-progress OpenID
     * authentication attempt.
     *
     * @param expires
     *     The number of milliseconds that may elapse before this session must
     *     be considered invalid.
     */
    public OpenIDAuthenticationSession(long expires) {
        super(expires);
    }

    /**
     * Set the pkce_verifier
     *
     * @param verifier
     *     The verifier to be stored
     */
    public void setVerifier(String verifier) {
        this.verifier = verifier;
    }

    /**
     * Returns the stored PKCE verifier
     *
     * @return
     *     The PKCE verifier
     */
    public String getVerifier() {
        return verifier;
    }

    /**
     * Set the redirect URI sent to the identity provider
     *
     * @param redirect_uri
     *     The redirect UTI to be stored
     */
    public void setRedirectURI(String redirect_uri) {
        this.redirect_uri = redirect_uri;
    }

    /**
     * Returns the stored redirect URI sent to the identity provider
     *
     * @return
     *     The redirect URI
     */
    public String getRedirectURI() {
        return redirect_uri;
    }

    /**
     * Set the code returned by the identity provider to exchange for a token
     *
     * @param code
     *     The code to be stored
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Returns the stored code returned by the identity provider to exchange for a token
     *
     * @return
     *     The stored code
     */
    public String getCode() {
        return code;
    }
    
}

