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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.onelogin.saml2.Auth;
import com.onelogin.saml2.authn.AuthnRequestParams;
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.exception.ValidationError;
import com.onelogin.saml2.settings.Saml2Settings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.apache.guacamole.auth.saml.conf.SAMLConfiguration;
import org.apache.guacamole.net.auth.IdentifierGenerator;
import org.xml.sax.SAXException;

/**
 * Service which abstracts the internals of handling SAML requests and
 * responses across one or more configured SAML identity providers.
 */
@Singleton
public class SAMLService {

    /**
     * Service for retrieving SAML configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Manager of active SAML authentication attempts.
     */
    @Inject
    private SAMLAuthenticationSessionManager sessionManager;

    /**
     * Creates a new SAML request for the default configured SAML identity
     * provider, beginning the overall authentication flow that will ultimately
     * result in an asserted user identity if the user is successfully
     * authenticated by the SAML IdP.
     *
     * @return
     *     The URI of the SSO endpoint of the default SAML IdP that the user
     *     must be redirected to.
     *
     * @throws GuacamoleException
     *     If an error prevents the SAML request and redirect URI from being
     *     generated, or if no default identity provider is configured.
     */
    public URI createRequest() throws GuacamoleException {
        return createRequest(confService.getRequiredDefaultConfiguration());
    }

    /**
     * Creates a new SAML request for the SAML identity provider having the
     * given identifier.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @return
     *     The URI of the SSO endpoint of the specified SAML IdP that the user
     *     must be redirected to.
     *
     * @throws GuacamoleException
     *     If no identity provider with the given ID is configured, or if an
     *     error prevents the SAML request and redirect URI from being
     *     generated.
     */
    public URI createRequest(String idpId) throws GuacamoleException {
        return createRequest(confService.getRequiredConfiguration(idpId));
    }

    /**
     * Creates a new SAML request for the given SAML identity provider
     * configuration, binding the identity provider ID and issued request ID to
     * a server-side {@link SAMLAuthenticationSession} keyed by a random
     * {@code RelayState}.
     *
     * @param config
     *     The configuration of the SAML identity provider.
     *
     * @return
     *     The URI of the SSO endpoint of the SAML IdP that the user must be
     *     redirected to.
     *
     * @throws GuacamoleException
     *     If an error prevents the SAML request and redirect URI from being
     *     generated.
     */
    public URI createRequest(SAMLConfiguration config) throws GuacamoleException {

        Saml2Settings samlSettings = confService.getSamlSettings(config);

        // Produce redirect for continuing the authentication process with
        // the SAML IdP
        try {
            Auth auth = new Auth(samlSettings, null, null);

            // Generate a unique ID to use for the relay state
            String identifier = IdentifierGenerator.generateIdentifier();

            // Create the request URL for the SAML IdP
            String requestUrl = auth.login(
                    identifier,
                    new AuthnRequestParams(false, false, true),
                    true);

            // Create a new authentication session to represent this attempt while
            // it is in progress, binding both the request ID and IdP ID
            SAMLAuthenticationSession session = new SAMLAuthenticationSession(
                    auth.getLastRequestId(),
                    config.getId(),
                    config.getAuthenticationTimeout() * 60000L);

            // Save the session with the unique relay state ID
            sessionManager.defer(session, identifier);

            return new URI(requestUrl);
        }
        catch (IOException e) {
            throw new GuacamoleServerException("SAML authentication request "
                    + "could not be encoded: " + e.getMessage(), e);
        }
        catch (URISyntaxException e) {
            throw new GuacamoleServerException("SAML IdP redirect could not "
                    + "be generated due to an error in the URI syntax: "
                    + e.getMessage(), e);
        }
        catch (SettingsException e) {
            throw new GuacamoleServerException("Error while attempting to sign "
                    + "request using provided private key / certificate: "
                    + e.getMessage(), e);
        }

    }

    /**
     * Processes the given SAML response, as received by the shared SAML ACS
     * endpoint at the given URL, resolving the applicable identity provider
     * from the server-side session referenced by {@code relayState}.
     *
     * @param url
     *     The URL of the ACS endpoint that received the SAML response.
     *
     * @param relayState
     *     The "RelayState" value originally provided in the SAML request,
     *     identifying the in-progress authentication session.
     *
     * @param encodedResponse
     *     The base64-encoded response received from the SAML IdP.
     *
     * @return
     *     The {@link SAMLAuthenticationSession} associated with the in-progress
     *     authentication attempt, now populated with the validated
     *     {@link AssertedIdentity}.
     *
     * @throws GuacamoleException
     *     If the given SAML response is not valid, or if the configuration
     *     information required to validate or decrypt the response cannot be
     *     read.
     */
    public SAMLAuthenticationSession processResponse(String url, String relayState,
            String encodedResponse) throws GuacamoleException {
        return processResponse(null, url, relayState, encodedResponse);
    }

    /**
     * Processes the given SAML response, as received by a SAML ACS endpoint at
     * the given URL, verifying that the response matches both the in-progress
     * authentication session referenced by {@code relayState} and (when
     * specified) {@code expectedIdpId}.
     *
     * @param expectedIdpId
     *     The identity provider ID from the per-IdP ACS callback URL path, or
     *     null when received at the shared callback endpoint. When non-null,
     *     this must match the identity provider bound to the session.
     *
     * @param url
     *     The URL of the ACS endpoint that received the SAML response.
     *
     * @param relayState
     *     The "RelayState" value originally provided in the SAML request,
     *     identifying the in-progress authentication session.
     *
     * @param encodedResponse
     *     The base64-encoded response received from the SAML IdP.
     *
     * @return
     *     The {@link SAMLAuthenticationSession} associated with the in-progress
     *     authentication attempt, now populated with the validated
     *     {@link AssertedIdentity}.
     *
     * @throws GuacamoleException
     *     If the given SAML response is not valid, does not match the expected
     *     identity provider, or if configuration required to validate or
     *     decrypt the response cannot be read.
     */
    public SAMLAuthenticationSession processResponse(String expectedIdpId,
            String url, String relayState, String encodedResponse)
            throws GuacamoleException {

        if (relayState == null)
            throw new GuacamoleSecurityException("\"RelayState\" value "
                    + "is missing from SAML response.");

        SAMLAuthenticationSession session = sessionManager.resume(relayState);
        if (session == null)
            throw new GuacamoleSecurityException("\"RelayState\" value "
                    + "included with SAML response is not valid.");

        // Enforce strict binding between the ACS callback IdP (if specified)
        // and the IdP that initiated the authentication session
        String sessionIdpId = session.getIdentityProviderId();
        if (expectedIdpId != null && !expectedIdpId.equals(sessionIdpId))
            throw new GuacamoleSecurityException("SAML response received at "
                    + "callback for identity provider \"" + expectedIdpId
                    + "\", which does not match the identity provider \""
                    + sessionIdpId + "\" associated with the authentication "
                    + "session.");

        SAMLConfiguration config = confService.getConfiguration(sessionIdpId);
        if (config == null)
            throw new GuacamoleSecurityException("SAML identity provider \""
                    + sessionIdpId + "\" associated with the authentication "
                    + "session is not configured.");

        try {

            // Decode received SAML response using the specific IdP's settings
            SamlResponse response = new SamlResponse(
                    confService.getSamlSettings(config),
                    url, encodedResponse);

            // Validate SAML response timestamp, signature, InResponseTo, etc.
            if (!response.isValid(session.getRequestID())) {
                Exception validationException = response.getValidationException();
                throw new GuacamoleSecurityException("SAML response did not "
                        + "pass validation: " + validationException.getMessage(),
                        validationException);
            }

            // Parse identity asserted by the SAML IdP, recording the IdP ID
            // and its configured group attribute for this assertion
            session.setIdentity(new AssertedIdentity(response,
                    config.getId(), config.getGroupAttribute()));
            return session;

        }
        catch (ValidationError e) {
            throw new GuacamoleSecurityException("SAML response did not pass "
                    + "validation: " + e.getMessage(), e);
        }
        catch (SettingsException e) {
            throw new GuacamoleServerException("Current SAML settings are "
                    + "insufficient to decrypt/parse the received SAML "
                    + "response.", e);
        }
        catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            throw new GuacamoleServerException("XML contents of SAML "
                    + "response could not be parsed.", e);
        }
        catch (IOException e) {
            throw new GuacamoleServerException("Contents of SAML response "
                    + "could not be decrypted/read.", e);
        }

    }

}
