package org.glyptodon.guacamole.net.basic.rest;

import javax.ws.rs.core.Response;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
