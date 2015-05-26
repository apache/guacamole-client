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

package org.glyptodon.guacamole.net.basic.rest;

import java.util.Collection;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.glyptodon.guacamole.form.Field;

/**
 * An exception that will result in the given error error information being
 * returned from the API layer. All error messages have the same format which
 * is defined by APIError.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class APIException extends WebApplicationException {

    /**
     * Construct a new APIException with the given error. All information
     * associated with this new exception will be extracted from the given
     * APIError.
     *
     * @param error
     *     The error that occurred.
     */
    public APIException(APIError error) {
        super(Response.status(error.getType().getStatus()).entity(error).build());
    }

    /**
     * Creates a new APIException with the given type and message. The
     * corresponding APIError will be created from the provided information.
     *
     * @param type
     *     The type of error that occurred.
     *
     * @param message
     *     A human-readable message describing the error.
     */
    public APIException(APIError.Type type, String message) {
        this(new APIError(type, message));
    }

    /**
     * Creates a new APIException with the given type, message, and parameter
     * information. The corresponding APIError will be created from the
     * provided information.
     *
     * @param type
     *     The type of error that occurred.
     *
     * @param message
     *     A human-readable message describing the error.
     *
     * @param expected
     *     All parameters expected in the original request, or now required as
     *     a result of the original request, as a collection of fields.
     */
    public APIException(APIError.Type type, String message, Collection<Field> expected) {
        this(new APIError(type, message, expected));
    }

}
