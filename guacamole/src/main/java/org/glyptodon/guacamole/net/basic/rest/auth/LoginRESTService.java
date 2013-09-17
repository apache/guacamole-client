package org.glyptodon.guacamole.net.basic.rest.auth;

import com.google.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.rest.RESTModule;
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
 * A service for authenticating to the Guacamole REST API. Given valid
 * credentials, the service will return an auth token. Invalid credentials will
 * result in a permission error.
 * 
 * @author James Muehlner
 */


@Path("/api/login")
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
    public String login(@QueryParam("username") String username,
            @QueryParam("password") String password) {
        
        Credentials credentials = new Credentials();
        credentials.setUsername(username);
        credentials.setPassword(password);
        
        UserContext userContext;
        
        try {
            userContext = authProvider.getUserContext(credentials);
        } catch(GuacamoleException e) {
            logger.error("Exception caught while authenticating user.", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        // authentication failed.
        if(userContext == null)
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        
        String authToken = authTokenGenerator.getToken();
        
        tokenUserMap.put(authToken, userContext);
        
        return authToken;
    }
    
}
