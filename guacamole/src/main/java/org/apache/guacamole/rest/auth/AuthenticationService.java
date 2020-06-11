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

package org.apache.guacamole.rest.auth;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.event.AuthenticationFailureEvent;
import org.apache.guacamole.net.event.AuthenticationSuccessEvent;
import org.apache.guacamole.rest.event.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for performing authentication checks in REST endpoints.
 */
public class AuthenticationService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

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
     * Service for applying or reapplying layers of decoration.
     */
    @Inject
    private DecorationService decorationService;

    /**
     * The service to use to notify registered authentication listeners.
     */
    @Inject
    private ListenerService listenerService;

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
     * Regular expression which matches any Port Number.
     */
    private static final String PORT_NUMBER_REGEX = "(:[0-9]{1,5})?";
    
    /**
     * Pattern which matches valid values of the de-facto standard
     * "X-Forwarded-For" header.
     */
    private static final Pattern X_FORWARDED_FOR = Pattern.compile("^" + IP_ADDRESS_REGEX + PORT_NUMBER_REGEX + "(, " + IP_ADDRESS_REGEX + PORT_NUMBER_REGEX + ")*$");

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

            // Insufficient credentials should take precedence
            catch (GuacamoleInsufficientCredentialsException e) {
                if (authFailure == null || authFailure instanceof GuacamoleInvalidCredentialsException)
                    authFailure = e;
            }
            
            // Catch other credentials exceptions and assign the first one
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
     * Notify all bound listeners that a successful authentication
     * has occurred.
     *
     * @param authenticatedUser
     *      The user that was successfully authenticated.
     *
     * @throws GuacamoleException
     *      If thrown by a listener.
     */
    private void fireAuthenticationSuccessEvent(AuthenticatedUser authenticatedUser)
            throws GuacamoleException {
        listenerService.handleEvent(new AuthenticationSuccessEvent(authenticatedUser));
    }

    /**
     * Notify all bound listeners that an authentication attempt has failed.
     *
     * @param credentials
     *      The credentials that failed to authenticate.
     *
     * @throws GuacamoleException
     *      If thrown by a listener.
     */
    private void fireAuthenticationFailedEvent(Credentials credentials)
            throws GuacamoleException {
        listenerService.handleEvent(new AuthenticationFailureEvent(credentials));
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
            if (existingSession != null) {
                AuthenticatedUser updatedUser = updateAuthenticatedUser(
                        existingSession.getAuthenticatedUser(), credentials);
                fireAuthenticationSuccessEvent(updatedUser);
                return updatedUser;
            }

            // Otherwise, attempt authentication as a new user
            AuthenticatedUser authenticatedUser = AuthenticationService.this.authenticateUser(credentials);
            fireAuthenticationSuccessEvent(authenticatedUser);

            if (logger.isInfoEnabled())
                logger.info("User \"{}\" successfully authenticated from {}.",
                        authenticatedUser.getIdentifier(),
                        getLoggableAddress(credentials.getRequest()));

            return authenticatedUser;

        }

        // Log and rethrow any authentication errors
        catch (GuacamoleException e) {

            fireAuthenticationFailedEvent(credentials);

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
     * @param credentials
     *     The Credentials provided by the user in the most recent
     *     authentication attempt.
     *
     * @return
     *     A List of all UserContexts associated with the given
     *     AuthenticatedUser.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating or updating any UserContext.
     */
    private List<DecoratedUserContext> getUserContexts(GuacamoleSession existingSession,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        List<DecoratedUserContext> userContexts =
                new ArrayList<DecoratedUserContext>(authProviders.size());

        // If UserContexts already exist, update them and add to the list
        if (existingSession != null) {

            // Update all old user contexts
            List<DecoratedUserContext> oldUserContexts = existingSession.getUserContexts();
            for (DecoratedUserContext userContext : oldUserContexts) {

                UserContext oldUserContext = userContext.getUndecoratedUserContext();

                // Update existing UserContext
                AuthenticationProvider authProvider = oldUserContext.getAuthenticationProvider();
                UserContext updatedUserContext = authProvider.updateUserContext(oldUserContext, authenticatedUser, credentials);

                // Add to available data, if successful
                if (updatedUserContext != null)
                    userContexts.add(decorationService.redecorate(userContext,
                            updatedUserContext, authenticatedUser, credentials));

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
                    userContexts.add(decorationService.decorate(userContext,
                            authenticatedUser, credentials));

            }

        }

        return userContexts;

    }

    /**
     * Authenticates a user using the given credentials and optional
     * authentication token, returning the authentication token associated with
     * the user's Guacamole session, which may be newly generated. If an
     * existing token is provided, the authentication procedure will attempt to
     * update or reuse the provided token, but it is possible that a new token
     * will be returned. Note that this function CANNOT return null.
     *
     * @param credentials
     *     The credentials to use when authenticating the user.
     *
     * @param token
     *     The authentication token to use if attempting to re-authenticate an
     *     existing session, or null to request a new token.
     *
     * @return
     *     The authentication token associated with the newly created or
     *     existing session.
     *
     * @throws GuacamoleException
     *     If the authentication or re-authentication attempt fails.
     */
    public String authenticate(Credentials credentials, String token)
        throws GuacamoleException {

        // Pull existing session if token provided
        GuacamoleSession existingSession;
        if (token != null)
            existingSession = tokenSessionMap.get(token);
        else
            existingSession = null;

        // Get up-to-date AuthenticatedUser and associated UserContexts
        AuthenticatedUser authenticatedUser = getAuthenticatedUser(existingSession, credentials);
        List<DecoratedUserContext> userContexts = getUserContexts(existingSession, authenticatedUser, credentials);

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

        return authToken;

    }

    /**
     * Finds the Guacamole session for a given auth token, if the auth token
     * represents a currently logged in user. Throws an unauthorized error
     * otherwise.
     *
     * @param authToken The auth token to check against the map of logged in users.
     * @return The session that corresponds to the provided auth token.
     * @throws GuacamoleException If the auth token does not correspond to any
     *                            logged in user.
     */
    public GuacamoleSession getGuacamoleSession(String authToken) 
            throws GuacamoleException {
        
        // Try to get the session from the map of logged in users.
        GuacamoleSession session = tokenSessionMap.get(authToken);
       
        // Authentication failed.
        if (session == null)
            throw new GuacamoleUnauthorizedException("Permission Denied.");
        
        return session;

    }

    /**
     * Invalidates a specific authentication token and its corresponding
     * Guacamole session, effectively logging out the associated user. If the
     * authentication token is not valid, this function has no effect.
     *
     * @param authToken
     *     The token being invalidated.
     *
     * @return
     *     true if the given authentication token was valid and the
     *     corresponding Guacamole session was destroyed, false if the given
     *     authentication token was not valid and no action was taken.
     */
    public boolean destroyGuacamoleSession(String authToken) {

        // Remove corresponding GuacamoleSession if the token is valid
        GuacamoleSession session = tokenSessionMap.remove(authToken);
        if (session == null)
            return false;

        // Invalidate the removed session
        session.invalidate();
        return true;

    }

    /**
     * Returns all UserContexts associated with a given auth token, if the auth
     * token represents a currently logged in user. Throws an unauthorized
     * error otherwise.
     *
     * @param authToken
     *     The auth token to check against the map of logged in users.
     *
     * @return
     *     A List of all UserContexts associated with the provided auth token.
     *
     * @throws GuacamoleException
     *     If the auth token does not correspond to any logged in user.
     */
    public List<DecoratedUserContext> getUserContexts(String authToken)
            throws GuacamoleException {
        return getGuacamoleSession(authToken).getUserContexts();
    }

}
