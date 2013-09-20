/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glyptodon.guacamole.net.basic.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * An exception that will result in the given HTTP Status and message or entity 
 * being returned from the API layer.
 * 
 * @author James Muehlner
 */
public class HTTPException extends WebApplicationException {
    
    /**
     * Construct a new HTTPException with the given HTTP status and entity.
     * 
     * @param status The HTTP Status to use for the response.
     * @param entity The entity to use as the body of the response.
     */
    public HTTPException(Status status, Object entity) {
        super(Response.status(status).entity(entity).build());
    }
    
    /**
     * Construct a new HTTPException with the given HTTP status and message. The
     * message will be wrapped in an APIError container.
     * 
     * @param status The HTTP Status to use for the response.
     * @param entity The entity to wrap in an APIError as the body of the response.
     */
    public HTTPException(Status status, String message) {
        super(Response.status(status).entity(new APIError(message)).build());
    }
    
}
