/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic.rest.connection;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceNotFoundException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.net.basic.rest.connectiongroup.APIConnectionGroup;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for handling connection CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/connections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectionRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConnectionRESTService.class);
    
    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;
    
    /**
     * Retrieves an individual connection.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionID
     *     The identifier of the connection to retrieve.
     *
     * @return
     *     The connection having the given identifier.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the connection.
     */
    @GET
    @Path("/{connectionID}")
    @AuthProviderRESTExposure
    public APIConnection getConnection(@QueryParam("token") String authToken, 
            @PathParam("connectionID") String connectionID) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the connection directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, Connection> connectionDirectory =
                rootGroup.getConnectionDirectory();

        // Get the connection
        Connection connection = connectionDirectory.get(connectionID);
        if (connection == null)
            throw new GuacamoleResourceNotFoundException("No such connection: \"" + connectionID + "\"");

        return new APIConnection(connection);

    }

    /**
     * Retrieves the parameters associated with a single connection.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionID
     *     The identifier of the connection.
     *
     * @return
     *     A map of parameter name/value pairs.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the connection parameters.
     */
    @GET
    @Path("/{connectionID}/parameters")
    @AuthProviderRESTExposure
    public Map<String, String> getConnectionParameters(@QueryParam("token") String authToken, 
            @PathParam("connectionID") String connectionID) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Get the connection directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, Connection> connectionDirectory =
                rootGroup.getConnectionDirectory();

        // Get the connection
        Connection connection = connectionDirectory.get(connectionID);
        if (connection == null)
            throw new GuacamoleResourceNotFoundException("No such connection: \"" + connectionID + "\"");

        // Retrieve connection configuration
        GuacamoleConfiguration config = connection.getConfiguration();

        // Return parameter map
        return config.getParameters();

    }

    /**
     * Retrieves the usage history of a single connection.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionID
     *     The identifier of the connection.
     *
     * @return
     *     A list of connection records, describing the start and end times of
     *     various usages of this connection.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the connection history.
     */
    @GET
    @Path("/{connectionID}/history")
    @AuthProviderRESTExposure
    public List<? extends ConnectionRecord> getConnectionHistory(@QueryParam("token") String authToken, 
            @PathParam("connectionID") String connectionID) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Get the connection directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, Connection> connectionDirectory =
                rootGroup.getConnectionDirectory();

        // Get the connection
        Connection connection = connectionDirectory.get(connectionID);
        if (connection == null)
            throw new GuacamoleResourceNotFoundException("No such connection: \"" + connectionID + "\"");

        return connection.getHistory();

    }

    /**
     * Deletes an individual connection.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionID
     *     The identifier of the connection to delete.
     *
     * @throws GuacamoleException
     *     If an error occurs while deleting the connection.
     */
    @DELETE
    @Path("/{connectionID}")
    @AuthProviderRESTExposure
    public void deleteConnection(@QueryParam("token") String authToken, @PathParam("connectionID") String connectionID) 
            throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);

        // Get the connection directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, Connection> connectionDirectory =
                rootGroup.getConnectionDirectory();

        // Make sure the connection is there before trying to delete
        if (connectionDirectory.get(connectionID) == null)
            throw new GuacamoleResourceNotFoundException("No such connection: \"" + connectionID + "\"");

        // Delete the connection
        connectionDirectory.remove(connectionID);

    }

    /**
     * Retrieves a single connection group from the given user context. If
     * the given identifier is null or the root identifier, the root connection
     * group will be returned.
     *
     * @param userContext
     *     The user context to retrieve the connection group from.
     *
     * @param identifier
     *     The identifier of the connection group to retrieve.
     *
     * @return
     *     The connection group having the given identifier, or the root
     *     connection group if the identifier is null or the root identifier.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the connection group, or if the
     *     connection group does not exist.
     */
    private ConnectionGroup retrieveConnectionGroup(UserContext userContext,
            String identifier) throws GuacamoleException {

        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();

        // Use root group if identifier is null (or the standard root identifier)
        if (identifier == null || identifier.equals(APIConnectionGroup.ROOT_IDENTIFIER))
            return rootGroup;

        // Pull specified connection group otherwise
        Directory<String, ConnectionGroup> directory = rootGroup.getConnectionGroupDirectory();
        ConnectionGroup connectionGroup = directory.get(identifier);

        if (connectionGroup == null)
            throw new GuacamoleResourceNotFoundException("No such connection group: \"" + identifier + "\"");

        return connectionGroup;

    }

    /**
     * Creates a new connection and returns the identifier of the new
     * connection.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connection
     *     The connection to create.
     *
     * @return
     *     The identifier of the new connection.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the connection.
     */
    @POST
    @AuthProviderRESTExposure
    public String createConnection(@QueryParam("token") String authToken,
            APIConnection connection) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Validate that connection data was provided
        if (connection == null)
            throw new GuacamoleClientException("Connection JSON must be submitted when creating connections.");

        // Retrieve parent group
        String parentID = connection.getParentIdentifier();
        ConnectionGroup parentConnectionGroup = retrieveConnectionGroup(userContext, parentID);

        // Add the new connection
        Directory<String, Connection> connectionDirectory = parentConnectionGroup.getConnectionDirectory();
        connectionDirectory.add(new APIConnectionWrapper(connection));

        // Return the new connection identifier
        return connection.getIdentifier();

    }
  
    /**
     * Updates an existing connection. If the parent identifier of the
     * connection is changed, the connection will also be moved to the new
     * parent group.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param connectionID
     *     The identifier of the connection to update.
     *
     * @param connection
     *     The connection data to update the specified connection with.
     *
     * @throws GuacamoleException
     *     If an error occurs while updating the connection.
     */
    @PUT
    @Path("/{connectionID}")
    @AuthProviderRESTExposure
    public void updateConnection(@QueryParam("token") String authToken, 
            @PathParam("connectionID") String connectionID, APIConnection connection) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Validate that connection data was provided
        if (connection == null)
            throw new GuacamoleClientException("Connection JSON must be submitted when updating connections.");

        // Get the connection directory
        ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
        Directory<String, Connection> connectionDirectory =
                rootGroup.getConnectionDirectory();
        
        // Make sure the connection is there before trying to update
        Connection existingConnection = connectionDirectory.get(connectionID);
        if (existingConnection == null)
            throw new GuacamoleResourceNotFoundException("No such connection: \"" + connectionID + "\"");
        
        // Retrieve connection configuration
        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(connection.getProtocol());
        config.setParameters(connection.getParameters());

        // Update the connection
        existingConnection.setConfiguration(config);
        existingConnection.setName(connection.getName());
        connectionDirectory.update(existingConnection);

    }
    
}
