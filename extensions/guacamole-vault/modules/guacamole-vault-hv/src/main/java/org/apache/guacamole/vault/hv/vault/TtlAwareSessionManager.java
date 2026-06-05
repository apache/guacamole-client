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

package org.apache.guacamole.vault.hv.vault;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LoginToken;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.VaultLoginException;
import org.springframework.vault.client.VaultHttpHeaders;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Vault token manager that automatically handles token renewal and
 * re-authentication. This manager is TTL-aware and will:
 *
 * - Proactively renew renewable tokens before they expire
 * - Re-authenticate non-renewable tokens before they expire
 * - Skip renewal for tokens with TTL=0 (non-expiring tokens)
 * - Handle both LoginToken and VaultToken types.
 */
public final class TtlAwareSessionManager implements SessionManager {
    /**
     * The outcome of the renew token process
     */
    private enum Outcome { CONTINUE, REAUTH, STOP }

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TtlAwareSessionManager.class);

    /**
     * The client authentication method used to obtain new tokens.
     */
    private final ClientAuthentication clientAuthentication;

    /**
     * HTTP client
     */
    private final RestOperations restOperations;

    /**
     * Spring-managed scheduler for renewal tasks.
     */
    private final TaskScheduler scheduler;

    /**
     * The delay (in milliseconds) before token expiration when renewal
     * should occur.
     */
    private final long renewalDelayMillis;

    /**
     * Reference to the currently scheduled renewal task, if any.
     */
    private final AtomicReference<ScheduledFuture<?>> scheduledRenewal =
        new AtomicReference<>();

    /**
     * The current token.
     */
    private final AtomicReference<VaultToken> token = new AtomicReference<>();

    /**
     * Creates a new TTL-aware Vault session manager. Need to stay with
     * spring-vault 2.3.x at the moment until migration to Tomcat10 of
     * Guacamole. The lifecycleAwareSessionManager of version 2.3 is
     * not functional. If updating to a future version of spring-vault
     * this could extend the LifecycleAwareSessionManager and delegate
     * most of the work to it. Expired or non renewable tokens would still
     * need to be treated here
     *
     * @param clientAuthentication
     *     The authentication method to use for obtaining tokens.
     *
     * @param restOperations
     *     A RestOperations configured for the Vault endpoint.
     *
     * @param scheduler
     *     Spring TaskScheduler used for renewal scheduling.
     *
     * @param renewalDelayMillis
     *     The delay (in milliseconds) before token expiration when renewal
     *     should occur.
     */
    public TtlAwareSessionManager(
            final ClientAuthentication clientAuthentication,
            final RestOperations restOperations,
            final TaskScheduler scheduler,
            final long renewalDelayMillis) {

        this.clientAuthentication = clientAuthentication;
        this.restOperations = restOperations;
        this.scheduler = scheduler;
        this.renewalDelayMillis = renewalDelayMillis;

        // Obtain the initial token and schedule its renewal
        try {
            final VaultToken token = clientAuthentication.login();
            if (token != null && !token.getToken().isEmpty()) {
                this.token.set(token);
                scheduleRenewal(token);
                logger.info("TtlAwareSessionManager initialized with renewal delay of {} ms", renewalDelayMillis);
            }
        }
        catch (VaultLoginException e) {
            // An Authentication exception at this point is non recoverable
            logger.error("Non recoverable error initializing TtlAwareSessionManager : {}", e.getMessage());
        }
        catch (VaultException e) {
            // Recoverable error: reschedule authentication
            scheduleRenewal(null);
        }
    }

    /**
     * Closes the session token manager and cleans up resources.
     * Cancels any pending renewal tasks.
     *
     * Note: TaskScheduler is container-managed and must not be shut down here.
     */
    public void close() {
        logger.debug("Closing TtlAwareSessionManager");
        stopScheduling();
    }

    /**
     * Small helper function to stop scheduling before new scheduled task
     * or in case of unrecoverable errors or non-expiring tokens.
     */
    private void stopScheduling() {
        final ScheduledFuture<?> future = scheduledRenewal.getAndSet(null);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Schedules a renewal task for the given token if it has a TTL > 0.
     * For renewable tokens, this will attempt to renew the token. For
     * non-renewable tokens, this will re-authenticate to get a new token.
     *
     * @param token
    *       The token to schedule renewal for.
     */
    private void scheduleRenewal(final VaultToken token) {

        // Cancel any existing scheduled renewal
        stopScheduling();

        if (token == null || token.getToken().isEmpty()) {
            // Vault not ready or available ? Try again in 10 seconds
            logger.debug("Null token detected. Vault not ready ? Rescheduling authentication");
            scheduledRenewal.set(scheduler.schedule(this::renewTokenAsync, Instant.now().plusMillis(10_000)));
        }
        else { 
            try {
                final TokenInfo tokenInfo = getTokenInfo(token);

                // Skip scheduling if TTL is 0 (non-expiring token)
                if (tokenInfo.ttlSeconds == 0) {
                    logger.info("Skipping renewal scheduling for non-expiring token");
                    return;
                }

                final long delay = calculateDelayUntilRenewal(tokenInfo.ttlSeconds);

                if (delay <= 0) {
                    // Token is already past renewal threshold; renew immediately
                    logger.debug("Token is past renewal threshold, renewing immediately");
                    renewTokenAsync();
                }
                else {
                    logger.debug("Scheduling token renewal in {} ms", delay);
                    scheduledRenewal.set(scheduler.schedule(this::renewTokenAsync, Instant.now().plusMillis(delay)));
                }
            }
            catch (VaultException e) {
                logger.warn("Failed to get token information, attempting immediate renewal: {}", e.getMessage());
                renewTokenAsync();
            }
        }
    }

    /**
     * Contains token information extracted from Vault responses.
     */
    public static class TokenInfo {
        /** The time-to-live of a token*/
        public final long ttlSeconds;
        /** Whether the token is renewable or not */
        public final boolean renewable;

        /**
         * Constructor for token information extrcted from Vault responses
         *
         * @param ttlSeconds
         *     The number of second till the token expires, or zero is non expirying
         *
         * @param renewable
         *     Boolean flag of whether the token is renewable
         */
        public TokenInfo(final long ttlSeconds, final boolean renewable) {
            this.ttlSeconds = ttlSeconds;
            this.renewable = renewable;
        }
    }

    /**
     * Gets token information (TTL and renewable status) from the given VaultToken.
     * If the token is already a LoginToken, extracts info from it.
     * Otherwise, looks up the token to get its information.
     *
     * @param token The VaultToken to get information for.
     *
     * @return TokenInfo containing TTL and renewable status.
     *
     * @throws VaultException If the token lookup fails.
     */
    private TokenInfo getTokenInfo(final VaultToken token) {
        final long ttlSeconds;
        final boolean isRenewable;

        if (token instanceof LoginToken) {
            final LoginToken loginToken = (LoginToken) token;
            final Duration leaseDuration = loginToken.getLeaseDuration();
            ttlSeconds = leaseDuration != null ? leaseDuration.getSeconds() : 0;
            isRenewable = loginToken.isRenewable();

            logger.debug("Token is already a LoginToken. TTL: {}, Renewable: {}", ttlSeconds, isRenewable);
        }
        else {
            logger.debug("Looking up token information");

            final HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", token.getToken());
            final HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            final ResponseEntity<Map> response;
            try {
                response = restOperations.exchange(
                        "/auth/token/lookup-self",
                        HttpMethod.GET,
                        requestEntity,
                        Map.class
                );
            }
            catch (RestClientException e) {
                throw new VaultException("Vault request failed: " + e.getMessage(), e);
            }

            @SuppressWarnings("unchecked")
            final Map<String, Object> data =
                    Optional.ofNullable(response)
                            .map(ResponseEntity::getBody)
                            .map(body -> body.get("data"))
                            .filter(Map.class::isInstance)
                            .map(Map.class::cast)
                            .orElseThrow(() ->
                                    new VaultException("Failed to lookup token: no response data")
                            );

            final Object val = data.get("ttl");
            isRenewable = Boolean.TRUE.equals(data.get("renewable"));
            ttlSeconds = val instanceof Number ? ((Number) val).longValue() : 0L;

            logger.debug("Token lookup successful. TTL: {}, Renewable: {}", ttlSeconds, isRenewable);
        }

        return new TokenInfo(ttlSeconds, isRenewable);
    }     

    /**
     * Calculates the delay until the token should be renewed.
     *
     * @param ttlSeconds The token's TTL in seconds.
     *
     * @return The delay in milliseconds until renewal should occur.
     */
    private long calculateDelayUntilRenewal(final long ttlSeconds) {
        final long expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        final long delay = expiryTime - renewalDelayMillis - System.currentTimeMillis();

        logger.debug("Calculated renewal delay: {} ms for token with TTL: {} seconds", delay, ttlSeconds);

        return delay;
    }

    /**
     * Asynchronously renews the current token. For renewable tokens,
     * attempts to renew via the Vault API. For non-renewable tokens,
     * re-authenticates to get a new token.
     */
    private void renewTokenAsync() {
        final VaultToken currentToken = getSessionToken();

        if (currentToken == null || currentToken.getToken().isEmpty()) {
            logger.debug("No current token to renew, attempting re-authentication");
            attemptLogin();
            return;
        }

        TokenInfo tokenInfo;

        try {
            tokenInfo = getTokenInfo(currentToken);
        }
        catch (VaultException e) {
            logger.debug("Token lookup fail, attempting re-authentication : " + e.getMessage());
            attemptLogin();
            return;
        }
        catch (ClassCastException | IllegalArgumentException e) {
            logger.error("Non-recoverable error during token lookup: {}", e.getMessage());
            stopScheduling();
            return;
        }

        if (tokenInfo.ttlSeconds == 0) {
            logger.info("Token TTL is zero. Stop renewal for non-expiring tokens");
            stopScheduling();
            return;
        }

        final VaultToken newToken;

        if (tokenInfo.renewable) {
            try {
                logger.debug("Renewing token");
                newToken = renewToken(currentToken);
            }
            catch (VaultException e) {
                logger.warn("Token renewal failed, re-authenticating: {}", e.getMessage());
                attemptLogin();
                return;
            }
            catch (ClassCastException | IllegalArgumentException e) {
                logger.error("Non-recoverable error during token renewal: {}", e.getMessage());
                stopScheduling();
                return;
            }
        }
        else {
            logger.debug("Re-authenticating for non-renewable token");
            attemptLogin();
            return;
        }

        try {
            tokenInfo = getTokenInfo(newToken);
        }
        catch (VaultException e) {
            logger.debug("Token lookup fail, attempting re-authentication : " + e.getMessage());
            attemptLogin();
            return;
        }
        catch (ClassCastException | IllegalArgumentException e) {
            logger.error("Non-recoverable error during token renewal: {}", e.getMessage());
            stopScheduling();
            return;
        }            

        final long delay = calculateDelayUntilRenewal(tokenInfo.ttlSeconds);
        if (delay <= 0) {
            // Token is already past renewal threshold; renew immediately
            logger.info("Token is past renewal threshold during renewal, re-authenticating");
            attemptLogin();
            return;
        }

        logger.debug("Successfully renewed the token");
        setSessionToken(newToken);
    }

    /**
     * Renews a renewable token via the Vault API.
     *
     * @param token
     *     The token to renew.
     *
     * @return
     *      A new LoginToken with updated TTL.
     *
     * @throws VaultException
     *      If the renewal fails, but the error is recoverable. Non
     *      recoverable exceptions are bubbled up.
     */
    private LoginToken renewToken(final VaultToken token) throws VaultException {
        final VaultResponse response;
        
        try {
            response = restOperations.postForObject("/auth/token/renew-self",
                    new HttpEntity<>(VaultHttpHeaders.from(token)), VaultResponse.class);
        }
        catch (RestClientException e) {
            throw new VaultException("Vault request failed: " + e.getMessage(), e);
        }
    
        if (response == null || response.getAuth() == null) {
            throw new VaultException("Token renewal failed: no response data");
        }

        final String renewedToken = (String) response.getAuth().get("client_token");

        if (renewedToken == null) {
            throw new VaultException("Token renewal failed: missing token in response");
        }

        final Number ttl = (Number) response.getAuth().get("lease_duration");
        final Boolean renewable = (Boolean) response.getAuth().get("renewable");

        final long ttlSeconds = ttl != null ? ttl.longValue() : 0;
        final boolean isRenewable = renewable != null && renewable;

        final LoginToken newToken = LoginToken.builder()
                .token(renewedToken)
                .leaseDuration(Duration.ofSeconds(ttlSeconds))
                .renewable(isRenewable)
                .build();

        logger.info("Vault token renewed successfully. New TTL: {}, Renewable: {}", ttlSeconds, isRenewable);

        return newToken;
    }

    /*
     * Attempt a login via clientAuthentication class, and reschedule
     * in case of a recoverable failure.
     */
    private void attemptLogin() {

        try {
            VaultToken newToken = clientAuthentication.login();

            // If we have a VaultToken here the above might not throw a
            // VaultException for an invalid token. We use getTokenInfo
            // to force a VaultExpception for an invalid token, and avoid
            // an infinite loop. Since we have the token info, might as
            // well promote it to a LoginToken directly
            final TokenInfo tokenInfo = getTokenInfo(newToken);
            if (!(newToken instanceof LoginToken)) {
                newToken = LoginToken.builder()
                        .token(newToken.getToken())
                        .leaseDuration(Duration.ofSeconds(tokenInfo.ttlSeconds))
                        .renewable(tokenInfo.renewable)
                        .build();
            }

            logger.info("Vault login successful. New TTL: {}, Renewable: {}",
                    tokenInfo.ttlSeconds, tokenInfo.renewable);
            setSessionToken(newToken);
        }
        catch (VaultException e) {
            logger.warn("Recoverable authentication failure, rescheduling renewal : {}", e.getMessage());
            stopScheduling();
            scheduledRenewal.set(scheduler.schedule(this::renewTokenAsync,
                    Instant.now().plusMillis(10_000)));
        }
        catch (ClassCastException | IllegalArgumentException e) {
            logger.error("Non-recoverable authentication failure: {}", e.getMessage());
            stopScheduling();
        }
    }

    /**
     * Returns the current session token.
     */
    @Override
    public VaultToken getSessionToken() {
        return this.token.get();
    }

    /**
     * Sets the current token and schedules its renewal if applicable.
     *
     * @param token
     *      Can be either a Null, VaultToken or a LoginToken
     */
    public void setSessionToken(final VaultToken token) {
        if (token != null && !token.getToken().isEmpty()) {
            this.token.set(token);
        }
        scheduleRenewal(token);
    }
}
