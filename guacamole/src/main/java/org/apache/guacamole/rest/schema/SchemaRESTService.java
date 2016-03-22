/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
