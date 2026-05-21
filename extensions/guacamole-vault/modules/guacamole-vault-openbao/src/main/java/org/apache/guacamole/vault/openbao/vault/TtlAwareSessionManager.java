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

package org.apache.guacamole.vault.openbao.vault;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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
public class TtlAwareSessionManager implements SessionManager {
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
            ClientAuthentication clientAuthentication,
            RestOperations restOperations,
            TaskScheduler scheduler,
            long renewalDelayMillis) {

        this.clientAuthentication = clientAuthentication;
        this.restOperations = restOperations;
        this.scheduler = scheduler;
        this.renewalDelayMillis = renewalDelayMillis;

        // Obtain the initial token and schedule its renewal
        try {
            VaultToken token = clientAuthentication.login();
            setSessionToken(token);
            if (token != null && !token.getToken().isEmpty()) {
                logger.info("TtlAwareSessionManager initialized with renewal delay of {} ms", renewalDelayMillis);
            }
        }
        catch (RuntimeException e) {
            logger.error("Non recoverable error initializing TtlAwareSessionManager : {}", e.getMessage(), e);
        }
        catch (Exception e) {
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
        ScheduledFuture<?> future = scheduledRenewal.getAndSet(null);
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
    private void scheduleRenewal(VaultToken token) {

        // Cancel any existing scheduled renewal
        stopScheduling();

        if (token == null || token.getToken().isEmpty()) {
            // Vault not ready or available ? Try again in 10 seconds
            logger.debug("Null token detected. Vault not ready ? Rescheduling authentication");
            scheduledRenewal.set(scheduler.schedule(this::renewTokenAsync, Instant.now().plusMillis(10000)));
            return;
        }

        try {
            TokenInfo tokenInfo = getTokenInfo(token);

            // Skip scheduling if TTL is 0 (non-expiring token)
            if (tokenInfo.ttlSeconds == 0) {
                logger.info("Skipping renewal scheduling for non-expiring token");
                return;
            }

            long delay = calculateDelayUntilRenewal(tokenInfo.ttlSeconds);

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

    /**
     * Contains token information extracted from Vault responses.
     */
    private static class TokenInfo {
        final long ttlSeconds;
        final boolean renewable;

        TokenInfo(long ttlSeconds, boolean renewable) {
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
    private TokenInfo getTokenInfo(VaultToken token) {

        if (token instanceof LoginToken) {
            LoginToken loginToken = (LoginToken) token;
            Duration leaseDuration = loginToken.getLeaseDuration();
            long ttlSeconds = leaseDuration != null ? leaseDuration.getSeconds() : 0;
            boolean renewable = loginToken.isRenewable();

            logger.debug("Token is already a LoginToken. TTL: {}, Renewable: {}", ttlSeconds, renewable);
            return new TokenInfo(ttlSeconds, renewable);
        }

        ResponseEntity<Map> response;

        try {
            logger.debug("Looking up token information");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", token.getToken());
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            response = restOperations.exchange("/auth/token/lookup-self",
                HttpMethod.GET, requestEntity, Map.class);
        }
        catch (RestClientException e) {
            throw new VaultException("Vault request failed: " + e.getMessage());
        }

        if (response == null || response.getBody() == null
                || !(response.getBody().get("data") instanceof Map)) {
            throw new VaultException("Failed to lookup token: no response data");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        Number ttl = (Number) data.get("ttl");
        Boolean renewable = (Boolean) data.get("renewable");

        long ttlSeconds = ttl != null ? ttl.longValue() : 0;
        boolean isRenewable = renewable != null && renewable;

        logger.debug("Token lookup successful. TTL: {}, Renewable: {}", ttlSeconds, isRenewable);

        return new TokenInfo(ttlSeconds, isRenewable);
    }

    /**
     * Calculates the delay until the token should be renewed.
     *
     * @param ttlSeconds The token's TTL in seconds.
     *
     * @return The delay in milliseconds until renewal should occur.
     */
    private long calculateDelayUntilRenewal(long ttlSeconds) {
        long expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        long delay = (expiryTime - renewalDelayMillis) - System.currentTimeMillis();

        logger.debug("Calculated renewal delay: {} ms for token with TTL: {} seconds", delay, ttlSeconds);

        return delay;
    }

    /**
     * Asynchronously renews the current token. For renewable tokens,
     * attempts to renew via the Vault API. For non-renewable tokens,
     * re-authenticates to get a new token.
     */
    private void renewTokenAsync() {

        try {
            VaultToken currentToken = getSessionToken();

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
                logger.debug("Token lookup fail, attempting re-authentication");
                attemptLogin();
                return;
            }
            catch (Exception e) {
                logger.error("Non-recoverable error during token lookup: {}", e.getMessage(), e);
                stopScheduling();
                return;
            }

            if (tokenInfo.ttlSeconds == 0) {
                logger.info("Token TTL is zero. Stop renewal for non-expiring tokens");
                stopScheduling();
                return;
            }

            VaultToken newToken;

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
                catch (Exception e) {
                    logger.error("Non-recoverable error during token renewal: {}", e.getMessage(), e);
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
                logger.debug("Token lookup fail, attempting re-authentication");
                attemptLogin();
                return;
            }

            long delay = calculateDelayUntilRenewal(tokenInfo.ttlSeconds);
            if (delay <= 0) {
                // Token is already past renewal threshold; renew immediately
                logger.info("Token is past renewal threshold during renewal, re-authenticating");
                attemptLogin();
                return;
            }

            logger.debug("Successfully renewed the token");
            setSessionToken(newToken);
        }
        catch (Exception e) {
            logger.error("Unexplained failure to renew token : {}", e.getMessage(), e);
            stopScheduling();
        }
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
    private LoginToken renewToken(VaultToken token) throws VaultException {

        VaultResponse response;

        try {
            response = restOperations.postForObject("/auth/token/renew-self",
                new HttpEntity<>(VaultHttpHeaders.from(token)), VaultResponse.class);
        }
        catch (RestClientException e) {
            throw new VaultException("Vault request failed: " + e.getMessage());
        }

        if (response == null || response.getAuth() == null) {
            throw new VaultException("Token renewal failed: no response data");
        }

        String renewedToken = (String) response.getAuth().get("client_token");

        if (renewedToken == null) {
            throw new VaultException("Token renewal failed: missing token in response");
        }

        Number ttl = (Number) response.getAuth().get("lease_duration");
        Boolean renewable = (Boolean) response.getAuth().get("renewable");

        long ttlSeconds = ttl != null ? ttl.longValue() : 0;
        boolean isRenewable = renewable != null && renewable;

        LoginToken newToken = LoginToken.builder()
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
            TokenInfo tokenInfo = getTokenInfo(newToken);
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
                    Instant.now().plusMillis(10000)));
        }
        catch (Exception e) {
            logger.error("Non-recoverable authentication failure: {}", e.getMessage(), e);
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
    public void setSessionToken(VaultToken token) {
        logger.debug("Setting new token : {}", token.getToken());
        if (token != null && !token.getToken().isEmpty()) {
            this.token.set(token);
        }
        scheduleRenewal(token);
    }
}
