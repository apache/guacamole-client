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

package org.apache.guacamole.rest.schema;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.rest.ObjectRetrievalService;
import org.apache.guacamole.rest.auth.AuthenticationService;
import org.apache.guacamole.protocols.ProtocolInfo;

/**
 * A REST service which provides access to descriptions of the properties,
 * attributes, etc. of objects used within the Guacamole REST API.
 *
 * @author Michael Jumper
 */
@Path("/schema/{dataSource}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SchemaRESTService {

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
     * Retrieves the possible attributes of a user object.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext dictating the available user attributes.
     *
     * @return
     *     A collection of forms which describe the possible attributes of a
     *     user object.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the possible attributes.
     */
    @GET
    @Path("/users/attributes")
    public Collection<Form> getUserAttributes(@QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier)
            throws GuacamoleException {

        // Retrieve all possible user attributes
        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);
        return userContext.getUserAttributes();

    }

    /**
     * Retrieves the possible attributes of a connection object.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext dictating the available connection attributes.
     *
     * @return
     *     A collection of forms which describe the possible attributes of a
     *     connection object.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the possible attributes.
     */
    @GET
    @Path("/connections/attributes")
    public Collection<Form> getConnectionAttributes(@QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier)
            throws GuacamoleException {

        // Retrieve all possible connection attributes
        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);
        return userContext.getConnectionAttributes();

    }

    /**
     * Retrieves the possible attributes of a connection group object.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext dictating the available connection group
     *     attributes.
     *
     * @return
     *     A collection of forms which describe the possible attributes of a
     *     connection group object.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the possible attributes.
     */
    @GET
    @Path("/connectionGroups/attributes")
    public Collection<Form> getConnectionGroupAttributes(@QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier)
            throws GuacamoleException {

        // Retrieve all possible connection group attributes
        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        UserContext userContext = retrievalService.retrieveUserContext(session, authProviderIdentifier);
        return userContext.getConnectionGroupAttributes();

    }

    /**
     * Gets a map of protocols defined in the system - protocol name to protocol.
     *
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param authProviderIdentifier
     *     The unique identifier of the AuthenticationProvider associated with
     *     the UserContext dictating the protocols available. Currently, the
     *     UserContext actually does not dictate this, the the same set of
     *     protocols will be retrieved for all users, though the identifier
     *     given here will be validated.
     *
     * @return
     *     A map of protocol information, where each key is the unique name
     *     associated with that protocol.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the available protocols.
     */
    @GET
    @Path("/protocols")
    public Map<String, ProtocolInfo> getProtocols(@QueryParam("token") String authToken,
            @PathParam("dataSource") String authProviderIdentifier)
            throws GuacamoleException {

        // Verify the given auth token and auth provider identifier are valid
        GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
        retrievalService.retrieveUserContext(session, authProviderIdentifier);

        // Get and return a map of all protocols.
        Environment env = new LocalEnvironment();
        return env.getProtocols();

    }

}
