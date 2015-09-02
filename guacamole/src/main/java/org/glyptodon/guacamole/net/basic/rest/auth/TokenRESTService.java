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

package org.glyptodon.guacamole.net.basic.rest.auth;

import com.google.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
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
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.auth.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleCredentialsException;
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
 * @author Michael Jumper
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
     * All configured authentication providers which can be used to
     * authenticate users or retrieve data associated with authenticated users.
     */
    @Inject
    private List<AuthenticationProvider> authProviders;
    
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
     * Attempts authentication against all AuthenticationProviders, in order,
     * using the provided credentials. The first authentication failure takes
     * priority, but remaining AuthenticationProviders are attempted. If any
     * AuthenticationProvider succeeds, the resulting AuthenticatedUser is
     * returned, and no further AuthenticationProviders are tried.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     The AuthenticatedUser given by the highest-priority
     *     AuthenticationProvider for which the given credentials are valid.
     *
     * @throws GuacamoleException
     *     If the given credentials are not valid for any
     *     AuthenticationProvider, or if an error occurs while authenticating
     *     the user.
     */
    private AuthenticatedUser authenticateUser(Credentials credentials)
        throws GuacamoleException {

        GuacamoleCredentialsException authFailure = null;

        // Attempt authentication against each AuthenticationProvider
        for (AuthenticationProvider authProvider : authProviders) {

            // Attempt authentication
            try {
                AuthenticatedUser authenticatedUser = authProvider.authenticateUser(credentials);
                if (authenticatedUser != null)
                    return authenticatedUser;
            }

            // First failure takes priority for now
            catch (GuacamoleCredentialsException e) {
                if (authFailure == null)
                    authFailure = e;
            }

        }

        // If a specific failure occured, rethrow that
        if (authFailure != null)
            throw authFailure;

        // Otherwise, request standard username/password
        throw new GuacamoleInvalidCredentialsException(
            "Permission Denied.",
            CredentialsInfo.USERNAME_PASSWORD
        );

    }

    /**
     * Re-authenticates the given AuthenticatedUser against the
     * AuthenticationProvider that originally created it, using the given
     * Credentials.
     *
     * @param authenticatedUser
     *     The AuthenticatedUser to re-authenticate.
     *
     * @param credentials
     *     The Credentials to use to re-authenticate the user.
     *
     * @return
     *     A AuthenticatedUser which may have been updated due to re-
     *     authentication.
     *
     * @throws GuacamoleException
     *     If an error prevents the user from being re-authenticated.
     */
    private AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleException {

        // Get original AuthenticationProvider
        AuthenticationProvider authProvider = authenticatedUser.getAuthenticationProvider();

        // Re-authenticate the AuthenticatedUser against the original AuthenticationProvider only
        authenticatedUser = authProvider.updateAuthenticatedUser(authenticatedUser, credentials);
        if (authenticatedUser == null)
            throw new GuacamoleSecurityException("User re-authentication failed.");

        return authenticatedUser;

    }

    /**
     * Returns the AuthenticatedUser associated with the given session and
     * credentials, performing a fresh authentication and creating a new
     * AuthenticatedUser if necessary.
     *
     * @param existingSession
     *     The current GuacamoleSession, or null if no session exists yet.
     *
     * @param credentials
     *     The Credentials to use to authenticate the user.
     *
     * @return
     *     The AuthenticatedUser associated with the given session and
     *     credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating or re-authenticating the
     *     user.
     */
    private AuthenticatedUser getAuthenticatedUser(GuacamoleSession existingSession,
            Credentials credentials) throws GuacamoleException {

        try {

            // Re-authenticate user if session exists
            if (existingSession != null)
                return updateAuthenticatedUser(existingSession.getAuthenticatedUser(), credentials);

            // Otherwise, attempt authentication as a new user
            AuthenticatedUser authenticatedUser = authenticateUser(credentials);
            if (logger.isInfoEnabled())
                logger.info("User \"{}\" successfully authenticated from {}.",
                        authenticatedUser.getIdentifier(),
                        getLoggableAddress(credentials.getRequest()));

            return authenticatedUser;

        }

        // Log and rethrow any authentication errors
        catch (GuacamoleException e) {

            // Get request and username for sake of logging
            HttpServletRequest request = credentials.getRequest();
            String username = credentials.getUsername();

            // Log authentication failures with associated usernames
            if (username != null) {
                if (logger.isWarnEnabled())
                    logger.warn("Authentication attempt from {} for user \"{}\" failed.",
                            getLoggableAddress(request), username);
            }

            // Log anonymous authentication failures
            else if (logger.isDebugEnabled())
                logger.debug("Anonymous authentication attempt from {} failed.",
                        getLoggableAddress(request));

            // Rethrow exception
            throw e;

        }

    }

    /**
     * Returns all UserContexts associated with the given AuthenticatedUser,
     * updating existing UserContexts, if any. If no UserContexts are yet
     * associated with the given AuthenticatedUser, new UserContexts are
     * generated by polling each available AuthenticationProvider.
     *
     * @param existingSession
     *     The current GuacamoleSession, or null if no session exists yet.
     *
     * @param authenticatedUser
     *     The AuthenticatedUser that has successfully authenticated or re-
     *     authenticated.
     *
     * @return
     *     A List of all UserContexts associated with the given
     *     AuthenticatedUser.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating or updating any UserContext.
     */
    private List<UserContext> getUserContexts(GuacamoleSession existingSession,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        List<UserContext> userContexts = new ArrayList<UserContext>(authProviders.size());

        // If UserContexts already exist, update them and add to the list
        if (existingSession != null) {

            // Update all old user contexts
            List<UserContext> oldUserContexts = existingSession.getUserContexts();
            for (UserContext oldUserContext : oldUserContexts) {

                // Update existing UserContext
                AuthenticationProvider authProvider = oldUserContext.getAuthenticationProvider();
                UserContext userContext = authProvider.updateUserContext(oldUserContext, authenticatedUser);

                // Add to available data, if successful
                if (userContext != null)
                    userContexts.add(userContext);

                // If unsuccessful, log that this happened, as it may be a bug
                else
                    logger.debug("AuthenticationProvider \"{}\" retroactively destroyed its UserContext.",
                            authProvider.getClass().getName());

            }

        }

        // Otherwise, create new UserContexts from available AuthenticationProviders
        else {

            // Get UserContexts from each available AuthenticationProvider
            for (AuthenticationProvider authProvider : authProviders) {

                // Generate new UserContext
                UserContext userContext = authProvider.getUserContext(authenticatedUser);

                // Add to available data, if successful
                if (userContext != null)
                    userContexts.add(userContext);

            }

        }

        return userContexts;

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
    @AuthProviderRESTExposure
    public APIAuthenticationResult createToken(@FormParam("username") String username,
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

        // Build credentials from request
        Credentials credentials = getCredentials(request, username, password);

        // Get up-to-date AuthenticatedUser and associated UserContexts
        AuthenticatedUser authenticatedUser = getAuthenticatedUser(existingSession, credentials);
        List<UserContext> userContexts = getUserContexts(existingSession, authenticatedUser);

        // Update existing session, if it exists
        String authToken;
        if (existingSession != null) {
            authToken = token;
            existingSession.setAuthenticatedUser(authenticatedUser);
            existingSession.setUserContexts(userContexts);
        }

        // If no existing session, generate a new token/session pair
        else {
            authToken = authTokenGenerator.getToken();
            tokenSessionMap.put(authToken, new GuacamoleSession(environment, authenticatedUser, userContexts));
            logger.debug("Login was successful for user \"{}\".", authenticatedUser.getIdentifier());
        }

        // Build list of all available auth providers
        List<String> authProviderIdentifiers = new ArrayList<String>(userContexts.size());
        for (UserContext userContext : userContexts)
            authProviderIdentifiers.add(userContext.getAuthenticationProvider().getIdentifier());

        // Return possibly-new auth token
        return new APIAuthenticationResult(
            authToken,
            authenticatedUser.getIdentifier(),
            authenticatedUser.getAuthenticationProvider().getIdentifier(),
            authProviderIdentifiers
        );

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
