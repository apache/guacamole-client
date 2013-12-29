/*
 * Copyright (C) 2013 Glyptodon LLC
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
