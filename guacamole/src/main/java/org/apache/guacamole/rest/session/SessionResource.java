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

package org.apache.guacamole.rest.session;

import com.google.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.rest.ObjectRetrievalService;
import org.apache.guacamole.rest.auth.AuthenticationService;

/**
 * A REST resource which exposes all UserContexts within a Guacamole user's
 * session.
 *
 * @author mjumper
 */
@Path("/data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SessionResource {

    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Service for convenient retrieval of objects.
     */
    @Inject
    private ObjectRetrievalService retrievalService;

    /**
     * Factory for creating UserContextResources which expose a given
     * UserContext.
     */
    @Inject
    private UserContextResourceFactory userContextResourceFactory;

    /**
     * Retrieves a resource representing the UserContext associated with the
     * AuthenticationProvider having the given identifier.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext being retrieved.
     *
     * @return
     *     A resource representing the UserContext associated with the
     *     AuthenticationProvider having the given identifier.
     *
     * @throws GuacamoleException
     *     If the authentication token or AuthenticationProvider identifier are
     *     invalid.
     */
    @Path("{dataSource}")
    public UserContextResource getUserContextResource(
            @QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier)
            throws GuacamoleException {

        // Pull UserContext defined by the given auth provider identifier
        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);

        // Return a resource exposing the retrieved UserContext
        return userContextResourceFactory.create(userContext);

    }

}
