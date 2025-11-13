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
import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.sso.SSOResource;

/**
 * REST API resource that provides OpenID-specific endpoints, including logout
 * functionality.
 */
public class OpenIDResource extends SSOResource {

    /**
     * Service for retrieving OpenID configuration information.
     */
    @Inject
    private org.apache.guacamole.auth.openid.conf.ConfigurationService confService;

    /**
     * Handles logout requests. For Keycloak and other OpenID providers, this
     * redirects to the provider's logout endpoint to properly end the SSO session.
     * The client-side JavaScript will handle clearing localStorage and cookies.
     *
     * @return
     *     An HTTP Response that redirects to the OpenID provider's logout endpoint,
     *     or a success response if logout endpoint cannot be determined.
     *
     * @throws GuacamoleException
     *     If an error occurs during logout processing.
     */
    @GET
    @Path("logout")
    public Response logout() throws GuacamoleException {
        try {
            // Construct Keycloak/OpenID logout endpoint
            // Keycloak's logout endpoint is typically at: {issuer}/protocol/openid-connect/logout
            String issuer = confService.getIssuer();
            URI logoutUri = UriBuilder.fromUri(issuer)
                    .path("/protocol/openid-connect/logout")
                    .queryParam("redirect_uri", confService.getRedirectURI())
                    .build();
            
            // Redirect to Keycloak logout endpoint
            return Response.seeOther(logoutUri).build();
        }
        catch (Exception e) {
            // If logout endpoint cannot be constructed, return success
            // Client-side JavaScript will still clear localStorage and cookies
            return Response.ok().build();
        }
    }

}

