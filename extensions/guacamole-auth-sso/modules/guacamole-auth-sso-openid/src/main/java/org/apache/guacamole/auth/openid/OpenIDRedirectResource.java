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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URI;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.openid.AuthenticationProviderService;
import org.apache.guacamole.auth.openid.conf.ConfigurationService;
import org.apache.guacamole.auth.openid.OpenIDAuthenticationSessionManager;
import org.apache.guacamole.auth.openid.util.PKCEUtil;
import org.apache.guacamole.auth.sso.SSOResource;
import org.apache.guacamole.net.auth.IdentifierGenerator;

/**
 * Local REST endpoint used by Guacamole to initiate the OIDC login with code flow and PKCE.
 * 
 * This endpoint:
 *   - receives the request from Guacamole Web UI
 *   - generates PKCE code_verifier and code_challenge
 *   - stores code_verifier in an temporary OpenIDAuthenicationSession
 *   - redirects the browser to the Identity Provider (Keycloak, etc.)
 *
 * This endpoint is the place where PKCE MUST be handled because when getLoginURI() in 
 * AuthenticationProvider is called  the authentication hasn't started yet
 */
public class OpenIDRedirectResource extends SSOResource {

    /**
     * The configuration service for this module.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Manager of active OpenID authentication attempts.
     */
    @Inject
    private OpenIDAuthenticationSessionManager sessionManager;

    /**
     * Local redirect endpoint invoked by Guacamole to pass to the identity provider
     * for code flow. Used to create and store PKCE challenges and store code values
     * returned by the identity provider.
     *
     * @param request
     *     The HttpServletRequest from Guacamole or the identity provider
     *
     * @return
     *     A redirect to the identity provider if the query parameter 'code' doesn't
     *     exist or a redirect to Guacamole to continue the authentication process
     *     with the values necessary to code flow in authenticateUser
     *
     *  @throws GuacamoleException
     *     If the PKCE challenge can not be generated or the verifier recovered
     */
    @GET
    @Path("/redirect")
    public Response redirectToFromIdentityProvider(@Context HttpServletRequest request)
            throws GuacamoleException {
        String code = request.getParameter("code");

        if (code == null) {
            UriBuilder builder = UriBuilder.fromUri(confService.getAuthorizationEndpoint());

            // Copy inbound request params
            @SuppressWarnings("unchecked")
            Map<String, String[]> params = (Map<String, String[]>) (Map) request.getParameterMap();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                for (String value : entry.getValue()) {
                    builder = builder.queryParam(entry.getKey(), value);
                }
            }
            
            // Create a new authentication session to represent this attempt while
            // it is in progress, using the request ID that was just issued
            OpenIDAuthenticationSession session = new OpenIDAuthenticationSession(
                        confService.getAuthenticationTimeout() * 60000L);

            // PKCE support
            if (confService.isPKCERequired()) {

                String codeVerifier = PKCEUtil.generateCodeVerifier();
                String codeChallenge;

                try {
                    codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
                } 
                catch (Exception e) {
                    throw new GuacamoleException("Unable to compute PKCE challenge", e);
                }

                // Store verifier for authenticateUser
                session.setVerifier(codeVerifier);

                builder.queryParam("code_challenge", codeChallenge)
                       .queryParam("code_challenge_method", "S256");
            }

            // Store redirect_uri for exchange of code for token, requires exact same uri
            String redirectURI = request.getParameter("redirect_uri");
            session.setRedirectURI(redirectURI);
            
            // Generate a unique ID to use to identify stored values
            String identifier = IdentifierGenerator.generateIdentifier();    
            builder.queryParam(AuthenticationProviderService.AUTH_SESSION_QUERY_PARAM, identifier);
            
            // Save the session with the stored values
            sessionManager.defer(session, identifier);

            return Response.seeOther(builder.build()).build();
        }

        // Retrieve the stored session
        String identifier = request.getParameter(AuthenticationProviderService.AUTH_SESSION_QUERY_PARAM);        
        OpenIDAuthenticationSession session = sessionManager.resume(identifier);
        
        if (confService.isPKCERequired()) {
            // Retrieve stored PKCE verifier
            String verifier = session.getVerifier();

            if (verifier == null)
                throw new GuacamoleException("Missing PKCE verifier from session.");
        }

        // Retrieve stored redirect URI
        String redirectURI = session.getRedirectURI();

        if (redirectURI == null)
            throw new GuacamoleException("Missing redirect URI from session.");        

        // Store the authorization code for authenticateUser()
        session.setCode(code);
        
        // Save the session with the stored values. Need to reactivate so it is
        // available for the next resume
        sessionManager.defer(session, identifier);   
        sessionManager.reactivateSession(identifier);

        // Redirect browser back to Guacamole UI to continue login
        URI resume = UriBuilder.fromUri(confService.getRedirectURI())
                .queryParam(AuthenticationProviderService.AUTH_SESSION_QUERY_PARAM, identifier)
                .build();
        return Response.seeOther(resume).build();
    }  
}
