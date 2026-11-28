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

package org.apache.guacamole.auth.saml.conf;

import java.io.File;
import java.net.URI;
import org.apache.guacamole.GuacamoleException;

/**
 * Configuration information defining how a SAML Identity Provider (IdP) should
 * be contacted, validated, and presented to users.
 */
public interface SAMLConfiguration {

    /**
     * The identifier used for the implicit single IdP when configured solely
     * via guacamole.properties.
     */
    String DEFAULT_ID = "default";

    /**
     * Returns the unique URL-safe identifier of this SAML identity provider.
     *
     * @return
     *     The unique identifier of this SAML identity provider.
     */
    String getId();

    /**
     * Returns the human-readable display name of this SAML identity provider
     * as shown on the Guacamole login screen. May be null if the default
     * translated SAML label should be used.
     *
     * @return
     *     The human-readable display name of this SAML identity provider, or
     *     null to use the default translated label.
     */
    String getDisplayName();

    /**
     * Returns whether this SAML identity provider is explicitly designated as
     * the default identity provider when multiple identity providers are
     * configured.
     *
     * @return
     *     true if this identity provider is designated as the default, false
     *     otherwise.
     */
    boolean isDefault();

    /**
     * Returns the URI of the file or remote endpoint containing the XML
     * metadata associated with this SAML IdP, or null if not configured.
     *
     * @return
     *     The URI of the SAML IdP metadata, or null if not configured.
     *
     * @throws GuacamoleException
     *     If the metadata URI cannot be parsed.
     */
    URI getIdpMetadata() throws GuacamoleException;

    /**
     * Returns the SSO URL of this SAML IdP when metadata XML is not used,
     * or null if not configured.
     *
     * @return
     *     The SSO URL of this SAML IdP, or null if not configured.
     *
     * @throws GuacamoleException
     *     If the IdP URL cannot be parsed.
     */
    URI getIdpUrl() throws GuacamoleException;

    /**
     * Returns the SP Entity ID (client identifier) to present to this SAML
     * IdP, or null if not configured.
     *
     * @return
     *     The SP Entity ID URI, or null if not configured.
     *
     * @throws GuacamoleException
     *     If the entity ID cannot be parsed.
     */
    URI getEntityId() throws GuacamoleException;

    /**
     * Returns the base callback URL of the Guacamole installation for this
     * SAML IdP.
     *
     * @return
     *     The base callback URL of the Guacamole installation.
     *
     * @throws GuacamoleException
     *     If the callback URL cannot be parsed or is missing.
     */
    URI getCallbackUrl() throws GuacamoleException;

    /**
     * Returns the Assertion Consumer Service (ACS) URL to advertise to this
     * SAML IdP.
     *
     * @return
     *     The ACS URL for this SAML IdP.
     *
     * @throws GuacamoleException
     *     If the ACS URL or callback URL cannot be parsed or is missing.
     */
    URI getAssertionConsumerServiceUrl() throws GuacamoleException;

    /**
     * Returns whether SAML library debugging is enabled for this IdP.
     *
     * @return
     *     true if SAML debugging is enabled, false otherwise.
     *
     * @throws GuacamoleException
     *     If the debug setting cannot be parsed.
     */
    boolean getDebug() throws GuacamoleException;

    /**
     * Returns whether compression of SAML requests sent to this IdP is enabled.
     *
     * @return
     *     true if request compression is enabled, false otherwise.
     *
     * @throws GuacamoleException
     *     If the request compression setting cannot be parsed.
     */
    boolean getCompressRequest() throws GuacamoleException;

    /**
     * Returns whether compression of SAML responses returned by this IdP is
     * requested.
     *
     * @return
     *     true if response compression is requested, false otherwise.
     *
     * @throws GuacamoleException
     *     If the response compression setting cannot be parsed.
     */
    boolean getCompressResponse() throws GuacamoleException;

    /**
     * Returns whether strict SAML security validation is enforced for this IdP.
     *
     * @return
     *     true if strict SAML validation is enabled, false otherwise.
     *
     * @throws GuacamoleException
     *     If the strict setting cannot be parsed.
     */
    boolean getStrict() throws GuacamoleException;

    /**
     * Returns the name of the SAML assertion attribute containing group
     * memberships for users authenticated by this IdP.
     *
     * @return
     *     The name of the group attribute.
     *
     * @throws GuacamoleException
     *     If the group attribute cannot be parsed.
     */
    String getGroupAttribute() throws GuacamoleException;

    /**
     * Returns the maximum amount of time, in minutes, to allow for an
     * in-progress SAML authentication attempt with this IdP to complete.
     *
     * @return
     *     The authentication timeout in minutes.
     *
     * @throws GuacamoleException
     *     If the authentication timeout cannot be parsed.
     */
    int getAuthenticationTimeout() throws GuacamoleException;

    /**
     * Returns the file containing the SP X.509 certificate used when signing
     * or encrypting requests for this IdP, or null if not configured.
     *
     * @return
     *     The SP X.509 certificate file, or null if not configured.
     *
     * @throws GuacamoleException
     *     If the certificate path cannot be parsed.
     */
    File getCertificateFile() throws GuacamoleException;

    /**
     * Returns the file containing the SP private key used when signing or
     * encrypting requests for this IdP, or null if not configured.
     *
     * @return
     *     The SP private key file, or null if not configured.
     *
     * @throws GuacamoleException
     *     If the private key path cannot be parsed.
     */
    File getPrivateKeyFile() throws GuacamoleException;

    /**
     * Returns the logout endpoint URI of this SAML IdP, or null if single
     * logout is not configured for this IdP.
     *
     * @return
     *     The logout endpoint URI of this SAML IdP, or null if not configured.
     *
     * @throws GuacamoleException
     *     If the logout endpoint URI cannot be parsed.
     */
    URI getLogoutEndpoint() throws GuacamoleException;

    /**
     * Returns the explicitly configured URI to redirect to after logout from
     * this SAML IdP completes, or null if not explicitly configured (in which
     * case the callback URL should be used).
     *
     * @return
     *     The explicitly configured post-logout redirect URI, or null if not
     *     configured.
     *
     * @throws GuacamoleException
     *     If the post-logout redirect URI cannot be parsed.
     */
    URI getPostLogoutRedirectURI() throws GuacamoleException;

}
