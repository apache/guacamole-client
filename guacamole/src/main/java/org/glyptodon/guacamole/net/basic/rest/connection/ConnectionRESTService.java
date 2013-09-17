package org.glyptodon.guacamole.net.basic.rest.connection;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.APIError;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;

/**
 * A REST Service for handling connection CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/api/connection")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectionRESTService {
    
    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;
    
    /**
     * A service for managing the REST endpoint APIConnection objects. 
     */
    @Inject
    private ConnectionService connectionService;
    
    /**
     * Gets a list of connections with the given ConnectionGroup parentID.
     * If no parentID is provided, returns the connections from the root group.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param parentID The ID of the ConnectionGroup the connections
     *                 belong to. If null, the root connection group will be used.
     * @return The connection list.
     */
    @GET
    public List<APIConnection> getConnections(@QueryParam("token") String authToken, @QueryParam("parentID") String parentID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            // If the parent connection group is passed in, try to find it.
            ConnectionGroup parentConnectionGroup;
            if(parentID == null)
                parentConnectionGroup = userContext.getRootConnectionGroup();
            else {
                ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
                Directory<String, ConnectionGroup> connectionGroupDirectory = rootGroup.getConnectionGroupDirectory();
                parentConnectionGroup = connectionGroupDirectory.get(parentID);
            }
            
            if(parentConnectionGroup == null)
                throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                    .entity(new APIError("No ConnectionGroup found with the provided parentID."))
                    .build());
            
            Directory<String, Connection> connectionDirectory = 
                    parentConnectionGroup.getConnectionDirectory();
            
            // Get the list of connection names
            List<Connection> connections = new ArrayList<Connection>();
            Iterable<String> identifiers = connectionDirectory.getIdentifiers();
            
            // Get the connection for each name
            for(String identifier : identifiers)
                connections.add(connectionDirectory.get(identifier));
            
            return connectionService.convertConnectionList(connections);
        } catch(GuacamoleSecurityException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                .entity(new APIError("Permission Denied.")).build());
        } catch(GuacamoleException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e).build());
        }
    }
    
    /**
     * Gets an individual connection.
     * 
     * @param authToken The authentication token that is used to authenticate
     *                  the user performing the operation.
     * @param connectionID The ID of the APIConnection..
     * @return The connection.
     */
    @GET
    @Path("/{connectionID}")
    public APIConnection getConnection(@QueryParam("token") String authToken, @PathParam("connectionID") String connectionID) {
        UserContext userContext = authenticationService.getUserContextFromAuthToken(authToken);
        
        try {
            
            // Get the connection directory
            ConnectionGroup rootGroup = userContext.getRootConnectionGroup();
            Directory<String, Connection> connectionDirectory =
                    rootGroup.getConnectionDirectory();
            
            // Get the connection
            Connection connection = connectionDirectory.get(connectionID);
            
            if(connection == null)
                throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                    .entity(new APIError("No Connection found with the provided ID."))
                    .build());
            
            return new APIConnection(connection);
        } catch(GuacamoleSecurityException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                .entity(new APIError("Permission Denied.")).build());
        } catch(GuacamoleException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e).build());
        }
    }

}
