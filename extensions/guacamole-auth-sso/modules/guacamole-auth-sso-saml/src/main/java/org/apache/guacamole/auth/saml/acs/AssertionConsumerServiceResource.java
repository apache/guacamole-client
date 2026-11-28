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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.saml.AuthenticationProviderService;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.apache.guacamole.auth.saml.conf.SAMLConfiguration;
import org.apache.guacamole.auth.sso.SSOResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API resource that provides SAML assertion consumer service (ACS)
 * endpoints as well as multi-IdP provider discovery, login, and logout
 * endpoints. SAML identity providers issue an HTTP POST to the ACS endpoint
 * asserting the user's identity when the user has successfully authenticated.
 */
public class AssertionConsumerServiceResource extends SSOResource {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AssertionConsumerServiceResource.class);

    /**
     * The SAML authentication provider service.
     */
    @Inject
    private AuthenticationProviderService samlAuthService;

    /**
     * The configuration service for this module.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Manager of active SAML authentication attempts.
     */
    @Inject
    private SAMLAuthenticationSessionManager sessionManager;

    /**
     * Service for processing SAML requests/responses.
     */
    @Inject
    private SAMLService saml;

    /**
     * Returns a public list of all configured SAML identity providers so the
     * Guacamole login UI can render a sign-in option for each provider.
     *
     * @return
     *     A list of {@link SAMLIdentityProviderInfo} descriptors in
     *     configuration order.
     *
     * @throws GuacamoleException
     *     If the SAML configuration cannot be read or validated.
     */
    @GET
    @Path("providers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SAMLIdentityProviderInfo> getIdentityProviders()
            throws GuacamoleException {

        boolean multiIdp = confService.isMultiIdpConfigured();
        Collection<SAMLConfiguration> configs = confService.getConfigurations();
        List<SAMLIdentityProviderInfo> providers = new ArrayList<>(configs.size());

        for (SAMLConfiguration config : configs) {
            String loginUrl = multiIdp
                    ? "api/ext/saml/login/" + config.getId()
                    : "api/ext/saml/login";
            providers.add(new SAMLIdentityProviderInfo(
                    config.getId(), config.getDisplayName(), loginUrl));
        }

        return providers;

    }

    /**
     * Redirects the user to the SSO login endpoint of the specified SAML
     * identity provider.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @return
     *     An HTTP 303 See Other response redirecting the user to the SAML IdP.
     *
     * @throws GuacamoleException
     *     If no identity provider with the given ID is configured, or if the
     *     SAML request cannot be generated.
     */
    @GET
    @Path("login/{idpId}")
    public Response redirectToIdentityProvider(@PathParam("idpId") String idpId)
            throws GuacamoleException {
        return Response.seeOther(samlAuthService.getLoginURI(idpId)).build();
    }

    /**
     * Redirects the user to the logout endpoint of the specified SAML identity
     * provider, if configured. If no logout endpoint is configured for that
     * identity provider, a 204 No Content response is returned.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @param idToken
     *     Optional ID token (unused by SAML).
     *
     * @return
     *     An HTTP 303 See Other response redirecting to the IdP logout
     *     endpoint if configured, or 204 No Content otherwise.
     *
     * @throws GuacamoleException
     *     If no identity provider with the given ID is configured, or if
     *     configuration cannot be parsed.
     */
    @GET
    @Path("logout/{idpId}")
    public Response getLogoutRedirect(
            @PathParam("idpId") String idpId,
            @QueryParam("id_token") String idToken) throws GuacamoleException {
        URI logoutURI = samlAuthService.getLogoutURI(idpId, idToken);
        if (logoutURI != null)
            return Response.seeOther(logoutURI).build();
        return Response.noContent().build();
    }

    /**
     * Processes a SAML response submitted via HTTP POST, optionally validating
     * that the response belongs to {@code expectedIdpId}.
     *
     * @param expectedIdpId
     *     The identity provider ID from the callback URL path, or null when
     *     handled by the shared callback endpoint.
     *
     * @param relayState
     *     The "RelayState" value originally provided in the SAML request.
     *
     * @param samlResponse
     *     The base64-encoded SAML response returned by the SAML IdP.
     *
     * @param consumedRequest
     *     The HttpServletRequest associated with the SAML response.
     *
     * @return
     *     An HTTP 303 See Other response redirecting back to Guacamole.
     *
     * @throws GuacamoleException
     *     If configuration information required to determine the redirect URL
     *     cannot be read.
     */
    private Response handleSamlResponse(String expectedIdpId, String relayState,
            String samlResponse, HttpServletRequest consumedRequest)
            throws GuacamoleException {

        SAMLConfiguration expectedConfig = expectedIdpId != null
                ? confService.getConfiguration(expectedIdpId)
                : null;

        URI fallbackBase = expectedConfig != null
                ? expectedConfig.getCallbackUrl()
                : confService.getFallbackCallbackUrl();

        try {

            // Validate and parse identity asserted by SAML IdP
            SAMLAuthenticationSession session = saml.processResponse(
                    expectedIdpId,
                    consumedRequest.getRequestURL().toString(),
                    relayState, samlResponse);

            SAMLConfiguration sessionConfig = confService.getConfiguration(
                    session.getIdentityProviderId());
            URI guacBase = sessionConfig != null
                    ? sessionConfig.getCallbackUrl()
                    : fallbackBase;

            // Store asserted identity for future retrieval via redirect
            String identifier = sessionManager.defer(session);

            // Redirect user such that Guacamole's authentication system can
            // retrieve the relevant SAML identity (stored above)
            UriBuilder redirectBuilder = UriBuilder.fromUri(guacBase)
                    .queryParam(AuthenticationProviderService.AUTH_SESSION_QUERY_PARAM, identifier);

            if (confService.isMultiIdpConfigured())
                redirectBuilder.queryParam(
                        AuthenticationProviderService.AUTH_IDP_QUERY_PARAM,
                        session.getIdentityProviderId());

            return Response.seeOther(redirectBuilder.build()).build();

        }

        // If invalid, redirect back to main page to re-attempt authentication
        catch (GuacamoleException e) {
            if (expectedIdpId != null)
                logger.warn("Authentication attempted for SAML identity provider \"{}\" with an invalid SAML response: {}",
                        expectedIdpId, e.getMessage(), e);
            else
                logger.warn("Authentication attempted with an invalid SAML response: {}",
                        e.getMessage(), e);

            return Response.seeOther(fallbackBase).build();
        }

    }

    /**
     * Processes the SAML response submitted by a SAML IdP via HTTP POST to the
     * shared ACS endpoint. If SSO has been successful, the user is redirected
     * back to Guacamole to complete authentication. If SSO has failed, the
     * user is redirected back to Guacamole to re-attempt authentication.
     *
     * @param relayState
     *     The "RelayState" value originally provided in the SAML request,
     *     which is the transient session identifier of the in-progress
     *     authentication attempt.
     *
     * @param samlResponse
     *     The encoded response returned by the SAML IdP.
     *
     * @param consumedRequest
     *     The HttpServletRequest associated with the SAML response.
     *
     * @return
     *     An HTTP Response redirecting the user back to Guacamole.
     *
     * @throws GuacamoleException
     *     If configuration information required for processing SAML responses
     *     cannot be read.
     */
    @POST
    @Path("callback")
    public Response processSamlResponse(
            @FormParam("RelayState") String relayState,
            @FormParam("SAMLResponse") String samlResponse,
            @Context HttpServletRequest consumedRequest)
            throws GuacamoleException {
        return handleSamlResponse(null, relayState, samlResponse, consumedRequest);
    }

    /**
     * Processes the SAML response submitted by a specific SAML IdP via HTTP
     * POST to its per-IdP ACS endpoint. The {@code idpId} path parameter must
     * match the identity provider bound to the in-progress session referenced
     * by {@code relayState}.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @param relayState
     *     The "RelayState" value originally provided in the SAML request.
     *
     * @param samlResponse
     *     The encoded response returned by the SAML IdP.
     *
     * @param consumedRequest
     *     The HttpServletRequest associated with the SAML response.
     *
     * @return
     *     An HTTP Response redirecting the user back to Guacamole.
     *
     * @throws GuacamoleException
     *     If configuration information required for processing SAML responses
     *     cannot be read.
     */
    @POST
    @Path("callback/{idpId}")
    public Response processSamlResponse(
            @PathParam("idpId") String idpId,
            @FormParam("RelayState") String relayState,
            @FormParam("SAMLResponse") String samlResponse,
            @Context HttpServletRequest consumedRequest)
            throws GuacamoleException {
        return handleSamlResponse(idpId, relayState, samlResponse, consumedRequest);
    }

}
