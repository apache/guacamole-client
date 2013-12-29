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

package org.glyptodon.guacamole.net.basic.rest.auth;

import com.google.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for authenticating to the Guacamole REST API. Given valid
 * credentials, the service will return an auth token. Invalid credentials will
 * result in a permission error.
 * 
 * @author James Muehlner
 */

@Path("/api/login")
@Produces(MediaType.APPLICATION_JSON)
public class LoginRESTService {
    
    /**
     * The authentication provider used to authenticate this user.
     */
    @Inject
    private AuthenticationProvider authProvider;
    
    /**
     * The map of auth tokens to users for the REST endpoints.
     */
    @Inject
    private TokenUserContextMap tokenUserMap;
    
    /**
     * A generator for creating new auth tokens.
     */
    @Inject
    private AuthTokenGenerator authTokenGenerator;

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(LoginRESTService.class);
    
    /**
     * Authenticates a user, generates an auth token, associates that auth token
     * with the user's UserContext for use by further requests.
     * 
     * @param username The username of the user who is to be authenticated.
     * @param password The password of the user who is to be authenticated.
     * @return The auth token for the newly logged-in user.
     */
    @POST
    @AuthProviderRESTExposure
    public APIAuthToken login(@QueryParam("username") String username,
            @QueryParam("password") String password) {
        
        Credentials credentials = new Credentials();
        credentials.setUsername(username);
        credentials.setPassword(password);
        
        UserContext userContext;
        
        try {
            userContext = authProvider.getUserContext(credentials);
        } catch(GuacamoleException e) {
            logger.error("Exception caught while authenticating user.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, 
                    "Unexpected server error.");
        }
        
        // authentication failed.
        if(userContext == null)
            throw new HTTPException(Status.UNAUTHORIZED, "Permission Denied.");
        
        String authToken = authTokenGenerator.getToken();
        
        tokenUserMap.put(authToken, userContext);
        
        return new APIAuthToken(authToken);
    }
    
}
