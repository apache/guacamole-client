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

package org.glyptodon.guacamole.net.basic.rest.auth;

import com.google.inject.Inject;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.DatatypeConverter;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.GuacamoleSession;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.HTTPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for managing auth tokens via the Guacamole REST API.
 * 
 * @author James Muehlner
 */
@Path("/tokens")
@Produces(MediaType.APPLICATION_JSON)
public class TokenRESTService {
    
    /**
     * The authentication provider used to authenticate this user.
     */
    @Inject
    private AuthenticationProvider authProvider;
    
    /**
     * The map of auth tokens to sessions for the REST endpoints.
     */
    @Inject
    private TokenSessionMap tokenSessionMap;
    
    /**
     * A generator for creating new auth tokens.
     */
    @Inject
    private AuthTokenGenerator authTokenGenerator;

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TokenRESTService.class);
    
    /**
     * Authenticates a user, generates an auth token, associates that auth token
     * with the user's UserContext for use by further requests. If an existing
     * token is provided, the authentication procedure will attempt to update
     * or reuse the provided token.
     * 
     * @param username
     *     The username of the user who is to be authenticated.
     *
     * @param password
     *     The password of the user who is to be authenticated.
     *
     * @param token
     *     An optional existing auth token for the user who is to be
     *     authenticated.
     *
     * @param request
     *     The HttpServletRequest associated with the login attempt.
     *
     * @return The auth token for the newly logged-in user.
     * @throws GuacamoleException If an error prevents successful login.
     */
    @POST
    @AuthProviderRESTExposure
    public APIAuthToken createToken(@FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("token") String token,
            @Context HttpServletRequest request) throws GuacamoleException {

        // Pull existing session if token provided
        GuacamoleSession existingSession;
        if (token != null)
            existingSession = tokenSessionMap.get(token);
        else
            existingSession = null;

        // If no username/password given, try Authorization header
        if (username == null && password == null) {

            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Basic ")) {

                try {

                    // Decode base64 authorization
                    String basicBase64 = authorization.substring(6);
                    String basicCredentials = new String(DatatypeConverter.parseBase64Binary(basicBase64), "UTF-8");

                    // Pull username/password from auth data
                    int colon = basicCredentials.indexOf(':');
                    if (colon != -1) {
                        username = basicCredentials.substring(0, colon);
                        password = basicCredentials.substring(colon + 1);
                    }
                    else
                        logger.debug("Invalid HTTP Basic \"Authorization\" header received.");

                }

                // UTF-8 support is required by the Java specification
                catch (UnsupportedEncodingException e) {
                    throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
                }

            }

        } // end Authorization header fallback
        
        // Build credentials
        Credentials credentials = new Credentials();
        credentials.setUsername(username);
        credentials.setPassword(password);
        credentials.setRequest(request);
        credentials.setSession(request.getSession(true));
        
        UserContext userContext;
        try {

            // Update existing user context if session already exists
            if (existingSession != null)
                userContext = authProvider.updateUserContext(existingSession.getUserContext(), credentials);

            /// Otherwise, generate a new user context
            else
                userContext = authProvider.getUserContext(credentials);

        }
        catch(GuacamoleException e) {
            logger.error("Exception caught while authenticating user.", e);
            throw new HTTPException(Status.INTERNAL_SERVER_ERROR, 
                    "Unexpected server error.");
        }
        
        // Authentication failed.
        if (userContext == null)
            throw new HTTPException(Status.UNAUTHORIZED, "Permission Denied.");

        // Update existing session, if it exists
        String authToken;
        if (existingSession != null) {
            authToken = token;
            existingSession.setCredentials(credentials);
            existingSession.setUserContext(userContext);
        }

        // If no existing session, generate a new token/session pair
        else {
            authToken = authTokenGenerator.getToken();
            tokenSessionMap.put(authToken, new GuacamoleSession(credentials, userContext));
        }
        
        logger.debug("Login was successful for user \"{}\".", userContext.self().getIdentifier());
        return new APIAuthToken(authToken, userContext.self().getIdentifier());

    }

    /**
     * Invalidates a specific auth token, effectively logging out the associated
     * user.
     * 
     * @param authToken The token being invalidated.
     */
    @DELETE
    @Path("/{token}")
    @AuthProviderRESTExposure
    public void invalidateToken(@PathParam("token") String authToken) {
        
        GuacamoleSession session = tokenSessionMap.remove(authToken);
        if (session == null)
            throw new HTTPException(Status.NOT_FOUND, "No such token.");

        session.invalidate();

    }

}
