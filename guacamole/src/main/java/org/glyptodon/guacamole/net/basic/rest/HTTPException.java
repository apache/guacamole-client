package org.glyptodon.guacamole.net.basic.rest;

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
