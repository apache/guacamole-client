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

package org.apache.guacamole.net.basic.rest;

import com.google.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.basic.rest.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A method interceptor which wraps custom exception handling around methods
 * which can throw GuacamoleExceptions and which are exposed through the REST
 * interface. The various types of GuacamoleExceptions are automatically
 * translated into appropriate HTTP responses, including JSON describing the
 * error that occurred.
 *
 * @author James Muehlner
 * @author Michael Jumper
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
    public Object invoke(MethodInvocation invocation) throws Throwable {

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

        // Additional credentials are needed
        catch (GuacamoleInsufficientCredentialsException e) {

            // Generate default message
            String message = e.getMessage();
            if (message == null)
                message = "Permission denied.";

            throw new APIException(
                APIError.Type.INSUFFICIENT_CREDENTIALS,
                message,
                e.getCredentialsInfo().getFields()
            );
        }

        // The provided credentials are wrong
        catch (GuacamoleInvalidCredentialsException e) {

            // Generate default message
            String message = e.getMessage();
            if (message == null)
                message = "Permission denied.";

            throw new APIException(
                APIError.Type.INVALID_CREDENTIALS,
                message,
                e.getCredentialsInfo().getFields()
            );
        }

        // Generic permission denied
        catch (GuacamoleSecurityException e) {

            // Generate default message
            String message = e.getMessage();
            if (message == null)
                message = "Permission denied.";

            throw new APIException(
                APIError.Type.PERMISSION_DENIED,
                message
            );

        }

        // Arbitrary resource not found
        catch (GuacamoleResourceNotFoundException e) {

            // Generate default message
            String message = e.getMessage();
            if (message == null)
                message = "Not found.";

            throw new APIException(
                APIError.Type.NOT_FOUND,
                message
            );

        }
        
        // Arbitrary bad requests
        catch (GuacamoleClientException e) {

            // Generate default message
            String message = e.getMessage();
            if (message == null)
                message = "Invalid request.";

            throw new APIException(
                APIError.Type.BAD_REQUEST,
                message
            );

        }

        // All other errors
        catch (GuacamoleException e) {

            // Generate default message
            String message = e.getMessage();
            if (message == null)
                message = "Unexpected server error.";

            // Ensure internal errors are logged at the debug level
            logger.debug("Unexpected exception in REST endpoint.", e);

            throw new APIException(
                APIError.Type.INTERNAL_ERROR,
                message
            );

        }

    }

}
