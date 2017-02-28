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

import com.google.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A method interceptor which wraps custom exception handling around methods
 * which can throw GuacamoleExceptions and which are exposed through the REST
 * interface. The various types of GuacamoleExceptions are automatically
 * translated into appropriate HTTP responses, including JSON describing the
 * error that occurred.
 */
public class RESTExceptionWrapper implements MethodInterceptor {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RESTExceptionWrapper.class);

    /**
     * Service for authenticating users and managing their Guacamole sessions.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Determines whether the given set of annotations describes an HTTP
     * request parameter of the given name. For a parameter to be associated
     * with an HTTP request parameter, it must be annotated with either the
     * <code>@QueryParam</code> or <code>@FormParam</code> annotations.
     *
     * @param annotations
     *     The annotations associated with the Java parameter being checked.
     *
     * @param name
     *     The name of the HTTP request parameter.
     *
     * @return
     *     true if the given set of annotations describes an HTTP request
     *     parameter having the given name, false otherwise.
     */
    private boolean isRequestParameter(Annotation[] annotations, String name) {

        // Search annotations for associated HTTP parameters
        for (Annotation annotation : annotations) {

            // Check if parameter is associated with the HTTP query string
            if (annotation instanceof QueryParam && name.equals(((QueryParam) annotation).value()))
                return true;

            // Failing that, check whether the parameter is associated with the
            // HTTP request body
            if (annotation instanceof FormParam && name.equals(((FormParam) annotation).value()))
                return true;

        }

        // No parameter annotations are present
        return false;

    }

    /**
     * Returns the authentication token that was passed in the given method
     * invocation. If the given method invocation is not associated with an
     * HTTP request (it lacks the appropriate JAX-RS annotations) or there is
     * no authentication token, null is returned.
     *
     * @param invocation
     *     The method invocation whose corresponding authentication token
     *     should be determined.
     *
     * @return
     *     The authentication token passed in the given method invocation, or
     *     null if there is no such token.
     */
    private String getAuthenticationToken(MethodInvocation invocation) {

        Method method = invocation.getMethod();

        // Get the types and annotations associated with each parameter
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] parameterTypes = method.getParameterTypes();

        // The Java standards require these to be parallel arrays
        assert(parameterAnnotations.length == parameterTypes.length);

        // Iterate through all parameters, looking for the authentication token
        for (int i = 0; i < parameterTypes.length; i++) {

            // Only inspect String parameters
            Class<?> parameterType = parameterTypes[i];
            if (parameterType != String.class)
                continue;

            // Parameter must be declared as a REST service parameter
            Annotation[] annotations = parameterAnnotations[i];
            if (!isRequestParameter(annotations, "token"))
                continue;

            // The token parameter has been found - return its value
            Object[] args = invocation.getArguments();
            return (String) args[i];

        }

        // No token parameter is defined
        return null;

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws WebApplicationException {

        try {

            // Invoke wrapped method
            try {
                return invocation.proceed();
            }

            // Ensure any associated session is invalidated if unauthorized
            catch (GuacamoleUnauthorizedException e) {

                // Pull authentication token from request
                String token = getAuthenticationToken(invocation);

                // If there is an associated auth token, invalidate it
                if (authenticationService.destroyGuacamoleSession(token))
                    logger.debug("Implicitly invalidated session for token \"{}\".", token);

                // Continue with exception processing
                throw e;

            }

        }

        // Translate GuacamoleException subclasses to HTTP error codes
        catch (GuacamoleSecurityException e) {
            throw new APIException(Response.Status.FORBIDDEN, e);
        }
        catch (GuacamoleResourceNotFoundException e) {
            throw new APIException(Response.Status.NOT_FOUND, e);
        }
        catch (GuacamoleClientException e) {
            throw new APIException(Response.Status.BAD_REQUEST, e);
        }
        catch (GuacamoleException e) {
            throw new APIException(Response.Status.INTERNAL_SERVER_ERROR, e);
        }

        // Rethrow unchecked exceptions such that they are properly wrapped
        catch (Throwable t) {

            // Log all reasonable details of error
            String message = t.getMessage();
            if (message != null)
                logger.error("Unexpected internal error: {}", message);
            else
                logger.error("An internal error occurred, but did not contain "
                           + "an error message. Enable debug-level logging for "
                           + "details.");

            // Ensure internal errors are fully logged at the debug level
            logger.debug("Unexpected error in REST endpoint.", t);

            throw new APIException(Response.Status.INTERNAL_SERVER_ERROR,
                    new GuacamoleException("Unexpected internal error.", t));

        }

    }

}
