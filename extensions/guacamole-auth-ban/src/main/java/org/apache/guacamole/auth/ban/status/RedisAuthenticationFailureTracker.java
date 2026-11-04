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

package org.apache.guacamole.auth.ban.status;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.redis.RedisClusterStore;
import org.apache.guacamole.language.TranslatableGuacamoleClientTooManyException;
import org.apache.guacamole.net.auth.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuthenticationFailureTracker which counts failures across every replica, so
 * that an attacker behind a load balancer is limited to maxAttempts in total
 * rather than to maxAttempts per replica.
 *
 * Failures are never cleared by a successful login. That matches
 * InMemoryAuthenticationFailureTracker, where success and a received request
 * take the identical code path and only the passage of time removes a count.
 */
public class RedisAuthenticationFailureTracker implements AuthenticationFailureTracker {

    /**
     * Logger for this class.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(RedisAuthenticationFailureTracker.class);

    /**
     * The stale window, in milliseconds, passed to the cluster store. It
     * governs only node liveness, which is irrelevant to failure counting
     * (failure counts expire on their own), but the store's constructor
     * requires a value.
     */
    private static final long UNUSED_STALE_WINDOW_MS = 30000L;

    /**
     * The cluster store in which failure counts are held, shared by every
     * replica so that each sees the same total for an address.
     */
    private final ClusterStore clusterStore;

    /**
     * The number of failures, across the whole cluster, after which further
     * authentication attempts from an address are blocked.
     */
    private final int maxAttempts;

    /**
     * The number of seconds an address remains blocked. This is also the
     * lifetime of the address's failure count in the cluster store, which is
     * refreshed by each new failure.
     */
    private final int banDuration;

    /**
     * Tracker used whenever the cluster cannot be reached. A Redis outage must
     * degrade to replica-local banning, never to no banning.
     */
    private final AuthenticationFailureTracker fallback;

    /**
     * Creates a new RedisAuthenticationFailureTracker which counts
     * authentication failures across the whole cluster.
     *
     * @param redisUri
     *     The URI of the Redis service holding cluster state.
     *
     * @param caCertificatePath
     *     Path to the PEM CA certificate that signed the Redis server
     *     certificate, or null to use the JVM truststore.
     *
     * @param maxAttempts
     *     The number of failures after which an address is blocked.
     *
     * @param banDuration
     *     The number of seconds an address remains blocked.
     *
     * @param maxAddresses
     *     The maximum number of addresses the fallback tracker will hold. This
     *     bounds the fallback only; the cluster counter has no such bound.
     */
    public RedisAuthenticationFailureTracker(String redisUri, String caCertificatePath,
            int maxAttempts, int banDuration, long maxAddresses) {
        this.clusterStore = new RedisClusterStore(redisUri, UNUSED_STALE_WINDOW_MS,
                "auth-ban", caCertificatePath);
        this.maxAttempts = maxAttempts;
        this.banDuration = banDuration;
        this.fallback = new InMemoryAuthenticationFailureTracker(maxAttempts,
                banDuration, maxAddresses);
    }

    /**
     * Blocks the request if the given count has reached the configured limit.
     *
     * @param address
     *     The address the request originated from.
     *
     * @param failures
     *     The number of failures recorded for that address.
     *
     * @throws GuacamoleException
     *     If the address has reached the configured limit.
     */
    private void blockIfBanned(String address, int failures)
            throws GuacamoleException {

        if (failures < maxAttempts)
            return;

        logger.warn("Blocking authentication attempt from address \"{}\" due to "
                + "number of authentication failures across the cluster.", address);
        throw new TranslatableGuacamoleClientTooManyException("Too "
                + "many failed authentication attempts.",
                "LOGIN.ERROR_TOO_MANY_ATTEMPTS");

    }

    /**
     * Applies the cluster-wide brute-force rules to one authentication request.
     *
     * @param credentials
     *     The credentials associated with the request.
     *
     * @param failed
     *     Whether the request is known to have failed. If the status of the
     *     request is not yet known, this should be false.
     *
     * @throws GuacamoleException
     *     If the authentication request is being blocked due to brute force
     *     prevention rules.
     */
    private void notifyAuthenticationStatus(Credentials credentials, boolean failed)
            throws GuacamoleException {

        // Ignore requests that do not contain explicit parameters of any kind
        if (credentials.isEmpty())
            return;

        // Determine originating address of the authentication request
        String address = credentials.getRemoteAddress();
        if (address == null)
            throw new GuacamoleServerException("Source address cannot be determined.");

        // Atomically increment the cluster-wide count on failure, or merely
        // read it otherwise. Either call returns a negative value if Redis
        // could not be reached.
        int failures;
        if (failed)
            failures = clusterStore.recordAuthenticationFailure(address, banDuration);
        else
            failures = clusterStore.getAuthenticationFailures(address);

        // The cluster could not answer: fall back to this replica's own view
        // rather than letting every attempt through
        if (failures < 0) {
            if (failed)
                fallback.notifyAuthenticationFailed(credentials);
            else
                fallback.notifyAuthenticationRequestReceived(credentials);
            return;
        }

        // Log failures, then block if the cluster-wide total is at the limit
        if (failed)
            logger.info("Authentication has failed for address \"{}\" (current "
                    + "total failures across the cluster: {}/{}).", address,
                    failures, maxAttempts);

        blockIfBanned(address, failures);

    }

    @Override
    public void notifyAuthenticationRequestReceived(Credentials credentials)
            throws GuacamoleException {
        notifyAuthenticationStatus(credentials, false);
    }

    @Override
    public void notifyAuthenticationSuccess(Credentials credentials)
            throws GuacamoleException {
        notifyAuthenticationStatus(credentials, false);
    }

    @Override
    public void notifyAuthenticationFailed(Credentials credentials)
            throws GuacamoleException {
        notifyAuthenticationStatus(credentials, true);
    }

}
