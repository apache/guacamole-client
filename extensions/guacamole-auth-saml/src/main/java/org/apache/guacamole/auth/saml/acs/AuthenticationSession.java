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

package org.apache.guacamole.auth.saml.acs;

/**
 * Representation of an in-progress SAML authentication attempt.
 */
public class AuthenticationSession {

    /**
     * The absolute point in time after which this authentication session is
     * invalid. This value is a UNIX epoch timestamp, as may be returned by
     * {@link System#currentTimeMillis()}.
     */
    private final long expirationTimestamp;

    /**
     * The request ID of the SAML request associated with the authentication
     * attempt.
     */
    private final String requestId;

    /**
     * The identity asserted by the SAML IdP, or null if authentication has not
     * yet completed successfully.
     */
    private AssertedIdentity identity = null;

    /**
     * Creates a new AuthenticationSession representing an in-progress SAML
     * authentication attempt.
     *
     * @param requestId
     *     The request ID of the SAML request associated with the
     *     authentication attempt.
     *
     * @param expires
     *     The number of milliseconds that may elapse before this session must
     *     be considered invalid.
     */
    public AuthenticationSession(String requestId, long expires) {
        this.expirationTimestamp = System.currentTimeMillis() + expires;
        this.requestId = requestId;
    }

    /**
     * Returns whether this authentication session is still valid (has not yet
     * expired). If an identity has been asserted by the SAML IdP, this
     * considers also whether the SAML response asserting that identity has
     * expired.
     *
     * @return
     *     true if this authentication session is still valid, false if it has
     *     expired.
     */
    public boolean isValid() {
        return System.currentTimeMillis() < expirationTimestamp
                && (identity == null || identity.isValid());
    }

    /**
     * Returns the request ID of the SAML request associated with the
     * authentication attempt.
     *
     * @return
     *     The request ID of the SAML request associated with the
     *     authentication attempt.
     */
    public String getRequestID() {
        return requestId;
    }

    /**
     * Marks this authentication attempt as completed and successful, with the
     * user having been asserted as having the given identity by the SAML IdP.
     *
     * @param identity
     *     The identity asserted by the SAML IdP.
     */
    public void setIdentity(AssertedIdentity identity) {
        this.identity = identity;
    }

    /**
     * Returns the identity asserted by the SAML IdP. If authentication has not
     * yet completed successfully, this will be null.
     *
     * @return
     *     The identity asserted by the SAML IdP, or null if authentication has
     *     not yet completed successfully.
     */
    public AssertedIdentity getIdentity() {
        return identity;
    }

}
