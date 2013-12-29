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

import javax.ws.rs.core.Response;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A method interceptor to wrap some custom exception handling around methods
 * that expose AuthenticationProvider functionality through the REST interface.
 * Translates various types of GuacamoleExceptions into appropriate HTTP responses.
 * 
 * @author James Muehlner
 */
public class AuthProviderRESTExceptionWrapper implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // Get the logger for the intercepted class
        Logger logger = LoggerFactory.getLogger(invocation.getMethod().getDeclaringClass());
        
        try {
            return invocation.proceed();
        } catch(GuacamoleSecurityException e) {
            throw new HTTPException(Response.Status.FORBIDDEN, e.getMessage() != null ? e.getMessage() : "Permission denied.");
        } catch(GuacamoleClientException e) {
            throw new HTTPException(Response.Status.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid Request.");
        } catch(GuacamoleException e) {
            logger.error("Unexpected GuacamoleException caught while executing " + invocation.getMethod().getName() + ".", e);
            throw new HTTPException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Unexpected server error.");
        }
    }
}
