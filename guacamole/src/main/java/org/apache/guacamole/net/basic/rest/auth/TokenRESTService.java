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

package org.apache.guacamole.net.basic.rest.auth;

import com.google.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.basic.GuacamoleSession;
import org.apache.guacamole.net.basic.rest.APIRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for managing auth tokens via the Guacamole REST API.
 * 
 * @author James Muehlner
 * @author Michael Jumper
 */
@Path("/tokens")
@Produces(MediaType.APPLICATION_JSON)
public class TokenRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TokenRESTService.class);

    /**
     * Service for authenticating users and managing their Guacamole sessions.
     */
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Returns the credentials associated with the given request, using the
     * provided username and password.
     *
     * @param request
     *     The request to use to derive the credentials.
     *
     * @param username
     *     The username to associate with the credentials, or null if the
     *     username should be derived from the request.
     *
     * @param password
     *     The password to associate with the credentials, or null if the
     *     password should be derived from the request.
     *
     * @return
     *     A new Credentials object whose contents have been derived from the
     *     given request, along with the provided username and password.
     */
    private Credentials getCredentials(HttpServletRequest request,
            String username, String password) {

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

        return credentials;

    }

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
     * @param consumedRequest
     *     The HttpServletRequest associated with the login attempt. The
     *     parameters of this request may not be accessible, as the request may
     *     have been fully consumed by JAX-RS.
     *
     * @param parameters
     *     A MultivaluedMap containing all parameters from the given HTTP
     *     request. All request parameters must be made available through this
     *     map, even if those parameters are no longer accessible within the
     *     now-fully-consumed HTTP request.
     *
     * @return
     *     An authentication response object containing the possible-new auth
     *     token, as well as other related data.
     *
     * @throws GuacamoleException
     *     If an error prevents successful authentication.
     */
    @POST
    public APIAuthenticationResult createToken(@FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("token") String token,
            @Context HttpServletRequest consumedRequest,
            MultivaluedMap<String, String> parameters)
            throws GuacamoleException {

        // Reconstitute the HTTP request with the map of parameters
        HttpServletRequest request = new APIRequest(consumedRequest, parameters);

        // Build credentials from request
        Credentials credentials = getCredentials(request, username, password);

        // Create/update session producing possibly-new token
        token = authenticationService.authenticate(credentials, token);

        // Pull corresponding session
        GuacamoleSession session = authenticationService.getGuacamoleSession(token);
        if (session == null)
            throw new GuacamoleResourceNotFoundException("No such token.");

        // Build list of all available auth providers
        List<UserContext> userContexts = session.getUserContexts();
        List<String> authProviderIdentifiers = new ArrayList<String>(userContexts.size());
        for (UserContext userContext : userContexts)
            authProviderIdentifiers.add(userContext.getAuthenticationProvider().getIdentifier());

        // Return possibly-new auth token
        AuthenticatedUser authenticatedUser = session.getAuthenticatedUser();
        return new APIAuthenticationResult(
            token,
            authenticatedUser.getIdentifier(),
            authenticatedUser.getAuthenticationProvider().getIdentifier(),
            authProviderIdentifiers
        );

    }

    /**
     * Invalidates a specific auth token, effectively logging out the associated
     * user.
     * 
     * @param authToken
     *     The token being invalidated.
     *
     * @throws GuacamoleException
     *     If the specified token does not exist.
     */
    @DELETE
    @Path("/{token}")
    public void invalidateToken(@PathParam("token") String authToken)
            throws GuacamoleException {

        // Invalidate session, if it exists
        if (!authenticationService.destroyGuacamoleSession(authToken))
            throw new GuacamoleResourceNotFoundException("No such token.");

    }

}
