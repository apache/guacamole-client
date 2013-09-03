/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glyptodon.guacamole.net.basic.rest.connection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * A REST Service for handling connection CRUD operations.
 * 
 * @author James Muehlner
 */
@Path("/api/connection")
public class ConnectionService {
    
    @Path("/")
    @GET
    public String getConnections() {
        return "goo";
    }   
    
}
