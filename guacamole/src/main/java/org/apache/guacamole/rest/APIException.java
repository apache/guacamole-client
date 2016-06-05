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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.protocol.GuacamoleStatus;

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
     * Creates a new APIException which represents an error that occurred within
     * an intercepted Guacamole stream. The nature of that error will be
     * described by a given status code, which should be the status code
     * provided by the "ack" instruction that reported the error.
     *
     * @param status
     *     The Guacamole protocol status code describing the error that
     *     occurred within the intercepted stream.
     *
     * @param message
     *     An arbitrary human-readable message describing the error that
     *     occurred.
     */
    public APIException(int status, String message) {
        this(new APIError(status, message));
    }

    /**
     * Creates a new APIException which represents an error that occurred within
     * an intercepted Guacamole stream. The nature of that error will be
     * described by a given Guacamole protocol status, which should be the
     * status associated with the code provided by the "ack" instruction that
     * reported the error.
     *
     * @param status
     *     The Guacamole protocol status describing the error that occurred
     *     within the intercepted stream.
     *
     * @param message
     *     An arbitrary human-readable message describing the error that
     *     occurred.
     */
    public APIException(GuacamoleStatus status, String message) {
        this(status.getGuacamoleStatusCode(), message);
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
