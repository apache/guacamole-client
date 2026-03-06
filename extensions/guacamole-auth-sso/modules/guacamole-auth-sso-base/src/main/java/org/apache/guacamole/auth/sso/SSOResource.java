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
package org.apache.guacamole.auth.sso;

import com.google.inject.Inject;
import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.apache.guacamole.GuacamoleException;

/**
 * REST API resource that provides allows the user to be manually redirected to
 * the applicable identity provider. Implementations may also provide
 * additional resources and endpoints beneath this resource as needed.
 */
public class SSOResource {

    /**
     * Service for authenticating users using CAS.
     */
    @Inject
    private SSOAuthenticationProviderService authService;

    /**
     * Redirects the user to the relevant identity provider. If the SSO
     * extension defining this resource is not the primary extension, and thus
     * the user will not be automatically redirected to the IdP, this endpoint
     * allows that redirect to occur manually upon a link/button click.
     *
     * @return
     *     An HTTP Response that will redirect the user to the IdP.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing the redirect from being created.
     */
    @GET
    @Path("login")
    public Response redirectToIdentityProvider() throws GuacamoleException {
        return Response.seeOther(authService.getLoginURI()).build();
    }

    /**
     * Redirects the user to the identity provider's logout endpoint, if
     * configured. This allows the user to log out from both Guacamole and the
     * SSO identity provider. If no logout endpoint is configured, a 204 No
     * Content response is returned.
     *
     * @param idToken
     *     The ID token from the user's authentication session, if available.
     *     This will be included as the id_token_hint parameter in the logout
     *     request. May be null.
     *
     * @return
     *     An HTTP Response that will redirect the user to the IdP logout
     *     endpoint if configured, or a 204 No Content response otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing the redirect from being created.
     */
    @GET
    @Path("logout")
    public Response getLogoutRedirect(
            @QueryParam("id_token") String idToken) throws GuacamoleException {
        URI logoutURI = authService.getLogoutURI(idToken);
        if (logoutURI != null)
            return Response.seeOther(logoutURI).build();
        else
            return Response.noContent().build();
    }

}
