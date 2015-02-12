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
import java.util.Collections;
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
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermissionSet;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermissionSet;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.ObjectRetrievalService;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
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
     * Service for convenient retrieval of objects.
     */
    @Inject
    private ObjectRetrievalService retrievalService;
    
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
        
        // Retrieve the requested connection
        return new APIConnection(retrievalService.retrieveConnection(userContext, connectionID));

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
        User self = userContext.self();

        // Retrieve permission sets
        SystemPermissionSet systemPermissions = self.getSystemPermissions();
        ObjectPermissionSet<String> connectionPermissions = self.getConnectionPermissions();

        // Deny access if adminstrative or update permission is missing
        if (!systemPermissions.hasPermission(SystemPermission.Type.ADMINISTER)
         && !connectionPermissions.hasPermission(ObjectPermission.Type.UPDATE, connectionID))
            throw new GuacamoleSecurityException("Permission to read connection parameters denied.");

        // Retrieve the requested connection
        Connection connection = retrievalService.retrieveConnection(userContext, connectionID);

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
    public List<ConnectionRecord> getConnectionHistory(@QueryParam("token") String authToken, 
            @PathParam("connectionID") String connectionID) throws GuacamoleException {

        UserContext userContext = authenticationService.getUserContext(authToken);
        
        // Retrieve the requested connection's history
        Connection connection = retrievalService.retrieveConnection(userContext, connectionID);
        return Collections.<ConnectionRecord>unmodifiableList(connection.getHistory());

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

        // Delete the specified connection
        connectionDirectory.remove(connectionID);

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
        ConnectionGroup parentConnectionGroup = retrievalService.retrieveConnectionGroup(userContext, parentID);

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
        
        // Retrieve connection to update
        Connection existingConnection = retrievalService.retrieveConnection(userContext, connectionID);

        // Build updated configuration
        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol(connection.getProtocol());
        config.setParameters(connection.getParameters());

        // Update the connection
        existingConnection.setConfiguration(config);
        existingConnection.setName(connection.getName());
        connectionDirectory.update(existingConnection);

        // Get old and new parents
        String oldParentIdentifier = existingConnection.getParentIdentifier();
        ConnectionGroup updatedParentGroup = retrievalService.retrieveConnectionGroup(userContext, connection.getParentIdentifier());

        // Update connection parent, if changed
        if (    (oldParentIdentifier != null && !oldParentIdentifier.equals(updatedParentGroup.getIdentifier()))
             || (oldParentIdentifier == null && updatedParentGroup.getIdentifier() != null))
            connectionDirectory.move(connectionID, updatedParentGroup.getConnectionDirectory());

    }
    
}
