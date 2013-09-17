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
import com.google.inject.servlet.RequestScoped;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.APIError;
import org.glyptodon.guacamole.net.basic.rest.auth.TokenUserContextMap;

/**
 * A REST Service for handling connection CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/api/connection")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectionRESTService {
    
    /**
     * The map of auth tokens to users for the REST endpoints.
     */
    @Inject
    private TokenUserContextMap tokenUserMap;
    
    /**
     * A service for managing the REST endpoint Connection objects. 
     */
    @Inject
    private ConnectionService connectionService;
    
    @GET
    public List<Connection> getConnections(@QueryParam("token") String authToken, @QueryParam("parentID") String parentID) {
        UserContext userContext = tokenUserMap.get(authToken);
       
        // authentication failed.
        if(userContext == null)
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                .entity(new APIError("Permission Denied.")).build());
        
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
            
            Directory<String, org.glyptodon.guacamole.net.auth.Connection> connectionDirectory = 
                    parentConnectionGroup.getConnectionDirectory();
            
            // Get the list of connection names
            List<org.glyptodon.guacamole.net.auth.Connection> connections 
                    = new ArrayList<org.glyptodon.guacamole.net.auth.Connection>();
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
    
}
