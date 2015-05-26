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

package org.glyptodon.guacamole.net.basic.rest;

import java.util.Collection;
import javax.ws.rs.core.Response;
import org.glyptodon.guacamole.form.Field;

/**
 * Describes an error that occurred within a REST endpoint.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class APIError {

    /**
     * The error message.
     */
    private final String message;

    /**
     * All expected request parameters, if any, as a collection of fields.
     */
    private final Collection<Field> expected;

    /**
     * The type of error that occurred.
     */
    private final Type type;

    /**
     * All possible types of REST API errors.
     */
    public enum Type {

        /**
         * The requested operation could not be performed because the request
         * itself was malformed.
         */
        BAD_REQUEST(Response.Status.BAD_REQUEST),

        /**
         * The credentials provided were invalid.
         */
        INVALID_CREDENTIALS(Response.Status.FORBIDDEN),

        /**
         * The credentials provided were not necessarily invalid, but were not
         * sufficient to determine validity.
         */
        INSUFFICIENT_CREDENTIALS(Response.Status.FORBIDDEN),

        /**
         * An internal server error has occurred.
         */
        INTERNAL_ERROR(Response.Status.INTERNAL_SERVER_ERROR),

        /**
         * An object related to the request does not exist.
         */
        NOT_FOUND(Response.Status.NOT_FOUND),

        /**
         * Permission was denied to perform the requested operation.
         */
        PERMISSION_DENIED(Response.Status.FORBIDDEN);

        /**
         * The HTTP status associated with this error type.
         */
        private final Response.Status status;

        /**
         * Defines a new error type associated with the given HTTP status.
         *
         * @param status
         *     The HTTP status to associate with the error type.
         */
        Type(Response.Status status) {
            this.status = status;
        }

        /**
         * Returns the HTTP status associated with this error type.
         *
         * @return
         *     The HTTP status associated with this error type.
         */
        public Response.Status getStatus() {
            return status;
        }

    }

    /**
     * Create a new APIError with the specified error message.
     *
     * @param type
     *     The type of error that occurred.
     *
     * @param message
     *     The error message.
     */
    public APIError(Type type, String message) {
        this.type     = type;
        this.message  = message;
        this.expected = null;
    }

    /**
     * Create a new APIError with the specified error message and parameter
     * information.
     *
     * @param type
     *     The type of error that occurred.
     *
     * @param message
     *     The error message.
     *
     * @param expected
     *     All parameters expected in the original request, or now required as
     *     a result of the original request, as a collection of fields.
     */
    public APIError(Type type, String message, Collection<Field> expected) {
        this.type     = type;
        this.message  = message;
        this.expected = expected;
    }

    /**
     * Returns the type of error that occurred.
     *
     * @return
     *     The type of error that occurred.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns a collection of all required parameters, where each parameter is
     * represented by a field.
     *
     * @return
     *     A collection of all required parameters.
     */
    public Collection<Field> getExpected() {
        return expected;
    }

    /**
     * Returns a human-readable error message describing the error that
     * occurred.
     *
     * @return
     *     A human-readable error message.
     */
    public String getMessage() {
        return message;
    }

}
