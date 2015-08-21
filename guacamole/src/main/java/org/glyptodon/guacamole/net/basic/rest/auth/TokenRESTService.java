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
import java.util.regex.Pattern;
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
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.auth.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.glyptodon.guacamole.net.basic.GuacamoleSession;
import org.glyptodon.guacamole.net.basic.rest.APIError;
import org.glyptodon.guacamole.net.basic.rest.APIRequest;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.APIException;
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
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;
    
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
     * Regular expression which matches any IPv4 address.
     */
    private static final String IPV4_ADDRESS_REGEX = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})";

    /**
     * Regular expression which matches any IPv6 address.
     */
    private static final String IPV6_ADDRESS_REGEX = "([0-9a-fA-F]*(:[0-9a-fA-F]*){0,7})";

    /**
     * Regular expression which matches any IP address, regardless of version.
     */
    private static final String IP_ADDRESS_REGEX = "(" + IPV4_ADDRESS_REGEX + "|" + IPV6_ADDRESS_REGEX + ")";

    /**
     * Pattern which matches valid values of the de-facto standard
     * "X-Forwarded-For" header.
     */
    private static final Pattern X_FORWARDED_FOR = Pattern.compile("^" + IP_ADDRESS_REGEX + "(, " + IP_ADDRESS_REGEX + ")*$");

    /**
     * Returns a formatted string containing an IP address, or list of IP
     * addresses, which represent the HTTP client and any involved proxies. As
     * the headers used to determine proxies can easily be forged, this data is
     * superficially validated to ensure that it at least looks like a list of
     * IPs.
     *
     * @param request
     *     The HTTP request to format.
     *
     * @return
     *     A formatted string containing one or more IP addresses.
     */
    private String getLoggableAddress(HttpServletRequest request) {

        // Log X-Forwarded-For, if present and valid
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && X_FORWARDED_FOR.matcher(header).matches())
            return "[" + header + ", " + request.getRemoteAddr() + "]";

        // If header absent or invalid, just use source IP
        return request.getRemoteAddr();

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
     * @return The auth token for the newly logged-in user.
     * @throws GuacamoleException If an error prevents successful login.
     */
    @POST
    @AuthProviderRESTExposure
    public APIAuthToken createToken(@FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("token") String token,
            @Context HttpServletRequest consumedRequest,
            MultivaluedMap<String, String> parameters)
            throws GuacamoleException {

        // Reconstitute the HTTP request with the map of parameters
        HttpServletRequest request = new APIRequest(consumedRequest, parameters);
        
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

        AuthenticatedUser authenticatedUser;
        try {

            // Re-authenticate user if session exists
            if (existingSession != null)
                authenticatedUser = authProvider.updateAuthenticatedUser(existingSession.getAuthenticatedUser(), credentials);

            /// Otherwise, authenticate as a new user
            else {

                authenticatedUser = authProvider.authenticateUser(credentials);

                // Log successful authentication
                if (authenticatedUser != null && logger.isInfoEnabled())
                    logger.info("User \"{}\" successfully authenticated from {}.",
                            authenticatedUser.getIdentifier(), getLoggableAddress(request));

            }

            // Request standard username/password if no user was produced
            if (authenticatedUser == null)
                throw new GuacamoleInvalidCredentialsException("Permission Denied.",
                        CredentialsInfo.USERNAME_PASSWORD);

        }
        catch (GuacamoleException e) {

            // Log authentication failures with associated usernames
            if (username != null) {
                if (logger.isWarnEnabled())
                    logger.warn("Authentication attempt from {} for user \"{}\" failed.",
                            getLoggableAddress(request), username);
            }

            // Log anonymous authentication failures
            else if (logger.isDebugEnabled())
                logger.debug("Anonymous authentication attempt from {} failed.",
                        getLoggableAddress(request), username);

            throw e;
        }

        // Generate or update user context
        UserContext userContext;
        if (existingSession != null)
            userContext = authProvider.updateUserContext(existingSession.getUserContext(), authenticatedUser);
        else
            userContext = authProvider.getUserContext(authenticatedUser);

        // STUB: Request standard username/password if no user context was produced
        if (userContext == null)
            throw new GuacamoleInvalidCredentialsException("Permission Denied.",
                    CredentialsInfo.USERNAME_PASSWORD);

        // Update existing session, if it exists
        String authToken;
        if (existingSession != null) {
            authToken = token;
            existingSession.setAuthenticatedUser(authenticatedUser);
            existingSession.setUserContext(userContext);
        }

        // If no existing session, generate a new token/session pair
        else {
            authToken = authTokenGenerator.getToken();
            tokenSessionMap.put(authToken, new GuacamoleSession(environment, authenticatedUser, userContext));
        }
        
        logger.debug("Login was successful for user \"{}\".", authenticatedUser.getIdentifier());
        return new APIAuthToken(authToken, authenticatedUser.getIdentifier());

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
            throw new APIException(APIError.Type.NOT_FOUND, "No such token.");

        session.invalidate();

    }

}
