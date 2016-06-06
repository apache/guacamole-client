/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.rest;

import java.util.Collection;
import javax.ws.rs.core.Response;
import org.apache.guacamole.form.Field;

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
     * The associated Guacamole protocol status code.
     */
    private final Integer statusCode;

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
        PERMISSION_DENIED(Response.Status.FORBIDDEN),

        /**
         * An error occurred within an intercepted stream, terminating that
         * stream. The Guacamole protocol status code of that error can be
         * retrieved with getStatusCode().
         */
        STREAM_ERROR(Response.Status.BAD_REQUEST);

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
     * Creates a new APIError of type STREAM_ERROR and having the given
     * Guacamole protocol status code and human-readable message. The status
     * code and message should be taken directly from the "ack" instruction
     * causing the error.
     *
     * @param statusCode
     *     The Guacamole protocol status code describing the error that
     *     occurred within the intercepted stream.
     *
     * @param message
     *     An arbitrary human-readable message describing the error that
     *     occurred.
     */
    public APIError(int statusCode, String message) {
        this.type       = Type.STREAM_ERROR;
        this.message    = message;
        this.statusCode = statusCode;
        this.expected   = null;
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
        this.type       = type;
        this.message    = message;
        this.statusCode = null;
        this.expected   = null;
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
        this.type       = type;
        this.message    = message;
        this.statusCode = null;
        this.expected   = expected;
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
     * Returns the Guacamole protocol status code associated with the error
     * that occurred. This is only valid for errors of type STREAM_ERROR.
     *
     * @return
     *     The Guacamole protocol status code associated with the error that
     *     occurred. If the error is not of type STREAM_ERROR, this will be
     *     null.
     */
    public Integer getStatusCode() {
        return statusCode;
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
