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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.cluster.ClusterLogoutHandler;
import org.apache.guacamole.cluster.ClusterProperties;
import org.apache.guacamole.cluster.ClusterSecurityPolicy;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.NoOpClusterStore;
import org.apache.guacamole.cluster.RehydratableAuthenticationProvider;
import org.apache.guacamole.cluster.TokenIdentity;
import org.apache.guacamole.cluster.redis.RedisClusterStore;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.GuacamoleCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.event.AuthenticationFailureEvent;
import org.apache.guacamole.net.event.AuthenticationRequestReceivedEvent;
import org.apache.guacamole.net.event.AuthenticationSuccessEvent;
import org.apache.guacamole.rest.event.ListenerService;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * A service for performing authentication checks in REST endpoints.
 */
@Singleton
public class AuthenticationService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

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
     * The HTTP request currently being served. Guice's servlet scope supplies
     * this per request, which is what lets a rebuilt session carry the details
     * of the request that triggered the rebuild.
     */
    @Inject
    private com.google.inject.Provider<HttpServletRequest> requestProvider;

    /**
     * A property which configures the session timeout for the Guacamole REST
     * API, in minutes. This is the same property HashTokenSessionMap reads; it
     * is re-declared here because that declaration is private to that class.
     */
    private static final IntegerGuacamoleProperty API_SESSION_TIMEOUT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "api-session-timeout"; }

    };

    /**
     * The default session timeout, in minutes, used if "api-session-timeout"
     * is not set. This matches the default of HashTokenSessionMap.
     */
    private static final int DEFAULT_API_SESSION_TIMEOUT = 60;

    /**
     * The cluster store holding the identity behind each session token issued
     * by any replica, allowing a session to be rebuilt on a replica other than
     * the one it authenticated against. This is a no-op store unless
     * clustering is enabled.
     *
     * The webapp cannot share the JDBC extension's store: extensions load in
     * their own classloaders, so this is a second Lettuce client per replica.
     */
    private final ClusterStore clusterStore = createClusterStore();

    /**
     * Raw tokens issued or rebuilt by this replica, by their hash.
     *
     * A cluster logout message carries only the hash, while the local session
     * map is keyed by the raw token, so this is the only way to connect the
     * two. It never leaves the JVM, and holds nothing the session map does not
     * already hold.
     */
    private final ConcurrentMap<String, String> localTokensByHash =
            new ConcurrentHashMap<String, String>();

    /**
     * Returns the idle lifetime of a session, in seconds, as configured by
     * "api-session-timeout". Tokens published to the cluster expire after this
     * long without use, matching the expiry of the local session map.
     *
     * @return
     *     The configured API session timeout, in seconds.
     */
    private static int sessionTimeoutSeconds() {

        try {
            return LocalEnvironment.getInstance().getProperty(API_SESSION_TIMEOUT,
                    DEFAULT_API_SESSION_TIMEOUT) * 60;
        }

        // Fall back to the default rather than failing the request, as the
        // timeout governs only how long the cluster remembers a token
        catch (GuacamoleException e) {
            logger.warn("Unable to read the API session timeout. Using the "
                    + "default of {} minutes for cluster session expiry.",
                    DEFAULT_API_SESSION_TIMEOUT, e);
            return DEFAULT_API_SESSION_TIMEOUT * 60;
        }

    }

    /**
     * Creates the cluster store this replica uses for session identity, based
     * on the cluster properties of the local environment.
     *
     * @return
     *     A Redis-backed store if clustering is enabled, a no-op store
     *     otherwise.
     *
     * @throws IllegalStateException
     *     If clustering is enabled but the configured Redis URI is refused by
     *     the cluster security policy.
     */
    private static ClusterStore createClusterStore() {

        String redisUri;
        boolean allowInsecure;
        String caCertificate;

        try {

            // Clustering is opt-in; without it, sessions remain local to the
            // replica exactly as before
            Environment environment = LocalEnvironment.getInstance();
            if (!ClusterProperties.isEnabled(environment))
                return new NoOpClusterStore();

            // Read how to reach Redis and how strictly to secure the connection
            redisUri = environment.getProperty(
                    ClusterProperties.CLUSTER_REDIS_URI, "redis://localhost:6379");

            allowInsecure = environment.getProperty(
                    ClusterProperties.CLUSTER_ALLOW_INSECURE_REDIS, false);

            caCertificate = environment.getProperty(
                    ClusterProperties.CLUSTER_REDIS_CA_CERT);

        }

        // A property that cannot be read at all must not stop the webapp from
        // serving sessions; it degrades to replica-local ones
        catch (GuacamoleException e) {
            logger.warn("Unable to determine whether clustering is enabled. "
                    + "Sessions will not survive the loss of a replica.", e);
            return new NoOpClusterStore();
        }

        // Check the URI against the security policy before connecting, logging
        // the policy's verdict alongside confirmation that clustering is active
        try {
            logger.info("Session tokens will be shared across the cluster. {} A "
                    + "session will survive the loss of the replica it "
                    + "authenticated against.",
                    ClusterSecurityPolicy.check(redisUri, allowInsecure));
        }

        // A refused URI is fatal rather than degraded. Degrading would disable
        // the token store silently while the JDBC extension refused separately,
        // leaving an operator with a broken cluster and no single reason
        catch (GuacamoleException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        // The web application holds its own Redis client, identified to the
        // store as "webapp", separate from that of any extension
        return new RedisClusterStore(redisUri, 30000L, "webapp", caCertificate);

    }

    /**
     * Returns the SHA-256 of the given auth token, hex-encoded.
     *
     * The token is a bearer credential, so the cluster stores its hash rather
     * than the token itself. A Redis dump, a backup, or an operator listing
     * keys then yields nothing usable for impersonation, while exact-match
     * lookup -- the only operation needed -- still works.
     *
     * @param authToken
     *     The token to hash.
     *
     * @return
     *     The hex-encoded SHA-256 of the given token.
     */
    private static String hashToken(String authToken) {

        // Digest the UTF-8 bytes of the token and encode the result as
        // lowercase hexadecimal
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(authToken.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash)
                hex.append(String.format("%02x", b));

            return hex.toString();

        }

        // SHA-256 is required of every Java implementation
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }

    }

    /**
     * The name of the HTTP header that may contain the authentication token
     * used by the Guacamole REST API.
     */
    public static final String TOKEN_HEADER_NAME = "Guacamole-Token";

    /**
     * The name of the query parameter that may contain the authentication
     * token used by the Guacamole REST API.
     */
    public static final String TOKEN_PARAMETER_NAME = "token";

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
     * @throws GuacamoleAuthenticationProcessException
     *     If the given credentials are not valid for any
     *     AuthenticationProvider, or if an error occurs while authenticating
     *     the user.
     */
    private AuthenticatedUser authenticateUser(Credentials credentials)
        throws GuacamoleAuthenticationProcessException {

        AuthenticationProvider failedAuthProvider = null;
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
                if (authFailure == null || authFailure instanceof GuacamoleInvalidCredentialsException) {
                    failedAuthProvider = authProvider;
                    authFailure = e;
                }
            }

            // Catch other credentials exceptions and assign the first one
            catch (GuacamoleCredentialsException e) {
                if (authFailure == null) {
                    failedAuthProvider = authProvider;
                    authFailure = e;
                }
            }

            catch (GuacamoleException | RuntimeException | Error e) {
                throw new GuacamoleAuthenticationProcessException("User "
                        + "authentication was aborted.", authProvider, e);
            }

        }

        throw new GuacamoleAuthenticationProcessException("User authentication "
                + "failed.", failedAuthProvider, authFailure);

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
     * @throws GuacamoleAuthenticationProcessException
     *     If an error prevents the user from being re-authenticated.
     */
    private AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleAuthenticationProcessException {

        // Get original AuthenticationProvider
        AuthenticationProvider authProvider = authenticatedUser.getAuthenticationProvider();

        try {

            // Re-authenticate the AuthenticatedUser against the original AuthenticationProvider only
            authenticatedUser = authProvider.updateAuthenticatedUser(authenticatedUser, credentials);
            if (authenticatedUser == null)
                throw new GuacamoleSecurityException("User re-authentication failed.");

            return authenticatedUser;

        }
        catch (GuacamoleException | RuntimeException | Error e) {
            throw new GuacamoleAuthenticationProcessException("User re-authentication failed.", authProvider, e);
        }

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
     * @throws GuacamoleAuthenticationProcessException
     *     If an error occurs while authenticating or re-authenticating the
     *     user.
     */
    private AuthenticatedUser getAuthenticatedUser(GuacamoleSession existingSession,
            Credentials credentials) throws GuacamoleAuthenticationProcessException {

        // Re-authenticate user if session exists
        if (existingSession != null) {
            AuthenticatedUser updatedUser = updateAuthenticatedUser(
                    existingSession.getAuthenticatedUser(), credentials);
            return updatedUser;
        }

        // Otherwise, attempt authentication as a new user
        AuthenticatedUser authenticatedUser = AuthenticationService.this.authenticateUser(credentials);
        return authenticatedUser;

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
     * @throws GuacamoleAuthenticationProcessException
     *     If an error occurs while creating or updating any UserContext.
     */
    private List<DecoratedUserContext> getUserContexts(GuacamoleSession existingSession,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleAuthenticationProcessException {

        List<DecoratedUserContext> userContexts = new ArrayList<>(authProviders.size());

        // If UserContexts already exist, update them and add to the list
        if (existingSession != null) {

            // Update all old user contexts
            List<DecoratedUserContext> oldUserContexts = existingSession.getUserContexts();
            for (DecoratedUserContext userContext : oldUserContexts) {

                UserContext oldUserContext = userContext.getUndecoratedUserContext();

                // Update existing UserContext
                AuthenticationProvider authProvider = oldUserContext.getAuthenticationProvider();
                UserContext updatedUserContext;
                try {
                    updatedUserContext = authProvider.updateUserContext(oldUserContext, authenticatedUser, credentials);
                }
                catch (GuacamoleException | RuntimeException | Error e) {
                    throw new GuacamoleAuthenticationProcessException("User "
                            + "authentication aborted during UserContext update.",
                            authProvider, e);
                }

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
                UserContext userContext;
                try {
                    userContext = authProvider.getUserContext(authenticatedUser);
                }
                catch (GuacamoleException | RuntimeException | Error e) {
                    throw new GuacamoleAuthenticationProcessException("User "
                            + "authentication aborted during initial "
                            + "UserContext creation.", authProvider, e);
                }

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

        String authToken;
        try {

            // Allow extensions to make updated to credentials prior to
            // actual authentication (NOTE: We do this here instead of in a
            // separate function to ensure that failure events accurately
            // represent the credentials that failed when a chain of credential
            // updates is involved)
            for (AuthenticationProvider authProvider : authProviders) {
                try {
                    credentials = authProvider.updateCredentials(credentials);
                }
                catch (GuacamoleException | RuntimeException | Error e) {
                    throw new GuacamoleAuthenticationProcessException("User "
                            + "authentication aborted during credential "
                            + "update/revision.", authProvider, e);
                }
            }

            // Fire pre-authentication event before ANY authn/authz occurs at all
            final Credentials updatedCredentials = credentials;
            listenerService.handleEvent((AuthenticationRequestReceivedEvent) () -> updatedCredentials);

            // Pull existing session if token provided
            GuacamoleSession existingSession;
            if (token != null)
                existingSession = tokenSessionMap.get(token);
            else
                existingSession = null;

            // Get up-to-date AuthenticatedUser and associated UserContexts
            AuthenticatedUser authenticatedUser = getAuthenticatedUser(existingSession, updatedCredentials);
            List<DecoratedUserContext> userContexts = getUserContexts(existingSession, authenticatedUser, updatedCredentials);

            // Update existing session, if it exists
            if (existingSession != null) {
                authToken = token;
                existingSession.setAuthenticatedUser(authenticatedUser);
                existingSession.setUserContexts(userContexts);
            }

            // If no existing session, generate a new token/session pair
            else {
                authToken = authTokenGenerator.getToken();
                tokenSessionMap.put(authToken, new GuacamoleSession(listenerService, authenticatedUser, userContexts));
                publishToken(authToken, authenticatedUser);
            }

            // Report authentication success
            try {
                listenerService.handleEvent(new AuthenticationSuccessEvent(authenticatedUser,
                        existingSession != null));
            }
            catch (GuacamoleException e) {
                throw new GuacamoleAuthenticationProcessException("User "
                        + "authentication aborted by event listener.", null, e);
            }

        }

        // Log and rethrow any authentication errors
        catch (GuacamoleAuthenticationProcessException e) {

            // NOTE: The credentials referenced here are intentionally NOT the
            // final updatedCredentials reference (though they may often be
            // equivalent) to ensure that failure events accurately represent
            // the credentials that failed if that failure occurs in the middle
            // of a chain of credential updates via updateCredentials()
            listenerService.handleEvent(new AuthenticationFailureEvent(credentials,
                    e.getAuthenticationProvider(), e.getCause()));

            // Rethrow exception
            e.rethrowCause();

            // This line SHOULD be unreachable unless a bug causes
            // rethrowCause() to not actually rethrow the underlying failure
            Throwable cause = e.getCause();
            if (cause != null)
                logger.warn("An underlying internal error was not correctly "
                        + "rethrown by rethrowCause(): {}", cause.getMessage(), cause);
            else
                logger.warn("An underlying internal error was not correctly "
                        + "rethrown by rethrowCause().");

            throw e.getCauseAsGuacamoleException();

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

        // A session this replica does not hold may still be live elsewhere in
        // the cluster
        if (session == null)
            session = rehydrateSession(authToken);

        // Authentication failed.
        if (session == null)
            throw new GuacamoleUnauthorizedException("Permission Denied.");

        // Keep the cluster's idle expiry in step with local access
        clusterStore.touchToken(hashToken(authToken), sessionTimeoutSeconds());

        return session;

    }

    /**
     * Subscribes this replica to logouts issued elsewhere in the cluster, so
     * that a session logged out on any replica is also dropped from this
     * replica's local session map. Invoked by Guice once this service has been
     * constructed, as a method annotated with @Inject.
     */
    @Inject
    public void registerLogoutHandler() {
        clusterStore.onLogout(new ClusterLogoutHandler() {

            @Override
            public void loggedOut(String tokenHash) {
                invalidateLocalSession(tokenHash);
            }

        });
    }

    /**
     * Drops the session behind the given token hash, if this replica holds one.
     *
     * Without this, logging out on one replica would leave a fully usable
     * session in another replica's local map until it timed out, which is a
     * worse property than the single-node behaviour this started from.
     *
     * @param tokenHash
     *     The hash of the token which has been logged out.
     */
    private void invalidateLocalSession(String tokenHash) {

        // Map the hash carried by the logout message back to the raw token;
        // a hash this replica never issued or rebuilt has nothing to drop
        String authToken = localTokensByHash.remove(tokenHash);
        if (authToken == null)
            return;

        // Remove and invalidate the local session, if it still exists
        GuacamoleSession session = tokenSessionMap.remove(authToken);
        if (session != null) {
            session.invalidate();
            logger.debug("Dropped a session logged out on another replica.");
        }

    }

    /**
     * Publishes the identity behind a newly-issued token to the cluster, so
     * that the session can be rebuilt on another replica. Only the hash of the
     * token is published, together with the username, the identifier of the
     * authenticating provider, and the address the user connected from; no
     * password or other credential leaves this replica.
     *
     * @param authToken
     *     The token which was issued.
     *
     * @param authenticatedUser
     *     The user the token identifies.
     */
    private void publishToken(String authToken, AuthenticatedUser authenticatedUser) {

        Credentials credentials = authenticatedUser.getCredentials();
        String tokenHash = hashToken(authToken);

        // Store the identity under the token's hash, expiring after the same
        // idle period as the local session
        clusterStore.putToken(tokenHash, new TokenIdentity(
                authenticatedUser.getIdentifier(),
                authenticatedUser.getAuthenticationProvider().getIdentifier(),
                credentials != null ? credentials.getRemoteAddress() : null,
                credentials != null ? credentials.getRemoteHostname() : null,
                System.currentTimeMillis()),
                sessionTimeoutSeconds());

        // Remember the raw token so that a cluster logout of this hash can
        // find the local session
        localTokensByHash.put(tokenHash, authToken);

    }

    /**
     * Rebuilds the session behind a token which this replica does not hold,
     * typically because the replica that issued it has since gone away. The
     * identity stored in the cluster for the token is handed back to the
     * authentication provider which originally authenticated the user, which
     * decides whether that user may still have a session. If it agrees, the
     * user's contexts are re-read from every provider and a new session is
     * placed in this replica's local session map under the same token.
     *
     * @param authToken
     *     The token whose session should be rebuilt.
     *
     * @return
     *     The rebuilt session, or null if the token is unknown to the cluster
     *     or its provider does not permit rebuilding.
     */
    private GuacamoleSession rehydrateSession(String authToken) {

        // Look up the identity the cluster holds for this token; a token the
        // cluster does not know is simply invalid
        TokenIdentity identity = clusterStore.getToken(hashToken(authToken));
        if (identity == null)
            return null;

        // Find the provider which originally authenticated the user
        AuthenticationProvider authProvider = null;
        for (AuthenticationProvider candidate : authProviders) {
            if (candidate.getIdentifier().equals(identity.getAuthProviderIdentifier())) {
                authProvider = candidate;
                break;
            }
        }

        // Default deny: only a provider which declares itself rehydratable may
        // have its sessions rebuilt. Every provider is wrapped in
        // AuthenticationProviderFacade, which
        // implements this interface and denies on behalf of any provider that
        // does not. The check below therefore passes for all of them, and the
        // real gate is a null return from rehydrate().
        if (!(authProvider instanceof RehydratableAuthenticationProvider)) {
            logger.debug("Session for user \"{}\" will not be rebuilt: provider "
                    + "\"{}\" is not rehydratable.", identity.getUsername(),
                    identity.getAuthProviderIdentifier());
            return null;
        }

        try {

            // Built from the request that triggered the rebuild. A fabricated
            // Credentials is not an option: its constructor copies the request,
            // and RequestDetails dereferences it. The live request is also the
            // more accurate source -- the rebuilt session is being used from
            // here and now, not from wherever it was first authenticated.
            Credentials skeleton = new Credentials(null, null,
                    requestProvider.get());

            // Ask the provider to vouch for the user again; it may refuse, for
            // example if the account no longer exists
            AuthenticatedUser authenticatedUser =
                    ((RehydratableAuthenticationProvider) authProvider)
                            .rehydrate(identity.getUsername(), skeleton);

            if (authenticatedUser == null) {
                logger.debug("Session for user \"{}\" will not be rebuilt: "
                        + "provider \"{}\" declined.", identity.getUsername(),
                        identity.getAuthProviderIdentifier());
                return null;
            }

            // Permissions are re-read here, so an account disabled or
            // de-permissioned since login is caught
            List<DecoratedUserContext> userContexts =
                    getUserContexts(null, authenticatedUser, skeleton);

            // Hold the rebuilt session locally under the original token, and
            // record its hash so a later cluster logout can find it
            GuacamoleSession session = new GuacamoleSession(listenerService,
                    authenticatedUser, userContexts);
            tokenSessionMap.put(authToken, session);
            localTokensByHash.put(hashToken(authToken), authToken);

            // No AuthenticationSuccessEvent is fired here, deliberately. That
            // event drives login accounting and brute-force banning, and a
            // replica restart must not present as a burst of logins.
            logger.info("Session for user \"{}\" rebuilt on this replica.",
                    identity.getUsername());
            return session;

        }

        // A session that cannot be rebuilt is simply not rebuilt; the user
        // authenticates again, which is the non-clustered behaviour. Note that
        // GuacamoleAuthenticationProcessException extends GuacamoleException,
        // so this one catch covers both.
        catch (GuacamoleException e) {
            logger.warn("Unable to rebuild session for user \"{}\".",
                    identity.getUsername(), e);
            return null;
        }

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

        // RESTExceptionMapper calls this for every unauthorized response,
        // including those carrying no token at all
        if (authToken == null)
            return false;

        String tokenHash = hashToken(authToken);

        // Withdraw from the cluster before anything else, so that no replica
        // can rebuild the session from this point on
        clusterStore.removeToken(tokenHash);

        // Then take our own copy, and only then announce the logout. The
        // announcement is delivered to this replica too, and a subscriber that
        // ran first would remove the session before the line below could see
        // it, causing this method to report that no such token existed for a
        // logout that had in fact succeeded.
        GuacamoleSession session = tokenSessionMap.remove(authToken);
        localTokensByHash.remove(tokenHash);
        clusterStore.publishLogout(tokenHash);

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

    /**
     * Returns the authentication token sent within the given request, if
     * present, or null if otherwise. Authentication tokens may be sent via
     * the "Guacamole-Token" header or the "token" query parameter. If both
     * the header and a parameter are used, the header is given priority.
     *
     * @param request
     *     The HTTP request to retrieve the authentication token from.
     *
     * @return
     *     The authentication token within the given request, or null if no
     *     token is present.
     */
    public String getAuthenticationToken(ContainerRequest request) {

        // Give priority to token within HTTP header
        String token = request.getHeaderString(TOKEN_HEADER_NAME);
        if (token != null && !token.isEmpty())
            return token;

        // If no token was provided via HTTP headers, fall back to using
        // query parameters
        token = request.getUriInfo().getQueryParameters().getFirst(TOKEN_PARAMETER_NAME);
        if (token != null && !token.isEmpty())
            return token;

        return null;

    }


    /**
     * Returns the cluster store this replica holds session identity in.
     *
     * Exposed so that the metrics endpoint can sample the store's gauges
     * through the same store instance, rather than constructing a second
     * store or referring to the implementation class directly.
     *
     * @return
     *     The web application's cluster store.
     */
    public ClusterStore getClusterStore() {
        return clusterStore;
    }

    /**
     * Releases the cluster resources held by this service.
     *
     * Called from GuacamoleServletContextListener.contextDestroyed. Without
     * it, redeploying the web application within the servlet container would
     * leave behind a Lettuce client, its Netty event loops, and the
     * authenticated connection and credential that opened it, in the memory of
     * a web application that is supposed to have gone away.
     */
    public void shutdown() {
        clusterStore.shutdown();
    }

}
