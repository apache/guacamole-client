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


package org.apache.guacamole.auth.jdbc.reset;

import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.net.RequestDetails;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * Unauthenticated REST resource exposed at /api/ext/{provider}/ while
 * self-service password reset is enabled.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SelfServicePasswordResetResource {

    /**
     * The one response every request receives, whatever the account.
     */
    public static final Map<String, String> RESPONSE = Collections.singletonMap("message",
            "If the account exists and has an e-mail address, a link was sent.");

    private final SelfServicePasswordResetService service;

    private final AuthenticationProvider authenticationProvider;

    private final int maxPerAddress;

    /**
     * Creates a resource handing requests to the given service.
     *
     * @param service
     *     The service handling requests.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider exposing this resource.
     *
     * @param maxPerAddress
     *     The maximum number of requests accepted per source address per
     *     hour.
     */
    public SelfServicePasswordResetResource(SelfServicePasswordResetService service,
            AuthenticationProvider authenticationProvider, int maxPerAddress) {
        this.service = service;
        this.authenticationProvider = authenticationProvider;
        this.maxPerAddress = maxPerAddress;
    }

    /**
     * Reports that self-service reset is available, so the login page can
     * show its link. The resource does not exist while the feature is off.
     *
     * @return
     *     {"enabled": "true"}.
     */
    @GET
    @Path("password-reset")
    public Map<String, String> getStatus() {
        return Collections.singletonMap("enabled", "true");
    }

    /**
     * Requests a reset link for the given username. The response is the same
     * whether or not the account exists, has an e-mail address or is usable.
     *
     * @param username
     *     The username typed by the requester.
     *
     * @param request
     *     The HTTP request, for its source address.
     *
     * @return
     *     The constant RESPONSE.
     */
    @POST
    @Path("password-reset")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Map<String, String> requestReset(@FormParam("username") String username,
            @Context HttpServletRequest request) {
        service.request(authenticationProvider, username,
                new Credentials(null, null, new RequestDetails(request)), maxPerAddress);
        return RESPONSE;
    }

}
