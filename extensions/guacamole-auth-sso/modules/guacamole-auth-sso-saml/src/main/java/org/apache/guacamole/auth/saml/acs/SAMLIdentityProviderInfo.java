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
 * Public, non-sensitive summary of a configured SAML identity provider exposed
 * to the Guacamole login UI so users can select which SAML identity provider
 * to authenticate with.
 */
public class SAMLIdentityProviderInfo {

    /**
     * The unique identifier of this SAML identity provider.
     */
    private final String id;

    /**
     * The human-readable display name for this SAML identity provider, or null
     * if the default translated SAML label should be shown.
     */
    private final String displayName;

    /**
     * The relative login endpoint URL that initiates authentication against
     * this SAML identity provider.
     */
    private final String loginUrl;

    /**
     * Creates a new SAMLIdentityProviderInfo descriptor.
     *
     * @param id
     *     The unique identifier of the SAML identity provider.
     *
     * @param displayName
     *     The human-readable display name of the identity provider, or null to
     *     use the default translated SAML label.
     *
     * @param loginUrl
     *     The relative login endpoint URL for this identity provider.
     */
    public SAMLIdentityProviderInfo(String id, String displayName, String loginUrl) {
        this.id = id;
        this.displayName = displayName;
        this.loginUrl = loginUrl;
    }

    /**
     * Returns the unique identifier of this SAML identity provider.
     *
     * @return
     *     The unique identifier of this SAML identity provider.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the human-readable display name of this SAML identity provider,
     * or null if the default translated SAML label should be shown.
     *
     * @return
     *     The human-readable display name, or null.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the relative login endpoint URL that initiates authentication
     * against this SAML identity provider.
     *
     * @return
     *     The relative login endpoint URL.
     */
    public String getLoginUrl() {
        return loginUrl;
    }

}
