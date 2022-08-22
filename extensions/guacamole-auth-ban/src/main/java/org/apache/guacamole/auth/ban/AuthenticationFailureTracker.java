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

package org.apache.guacamole.auth.ban;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.language.TranslatableGuacamoleClientTooManyException;
import org.apache.guacamole.net.auth.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides automated tracking and blocking of IP addresses that repeatedly
 * fail authentication.
 */
public class AuthenticationFailureTracker {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFailureTracker.class);

    /**
     * All authentication failures currently being tracked, stored by the
     * associated IP address.
     */
    private final Cache<String, AuthenticationFailureStatus> failures;

    /**
     * The maximum number of failed authentication attempts allowed before an
     * address is temporarily banned.
     */
    private final int maxAttempts;

    /**
     * The length of time that each address should be banned after reaching the
     * maximum number of failed authentication attempts, in seconds.
     */
    private final int banDuration;

    /**
     * Creates a new AuthenticationFailureTracker that automatically blocks
     * authentication attempts based on the provided blocking criteria.
     *
     * @param maxAttempts
     *     The maximum number of failed authentication attempts allowed before
     *     an address is temporarily banned.
     *
     * @param banDuration
     *     The length of time that each address should be banned after reaching
     *     the maximum number of failed authentication attempts, in seconds.
     *
     * @param maxAddresses
     *     The maximum number of unique IP addresses that should be tracked
     *     before discarding older tracked failures.
     */
    public AuthenticationFailureTracker(int maxAttempts, int banDuration,
            long maxAddresses) {

        this.maxAttempts = maxAttempts;
        this.banDuration = banDuration;

        // Inform administrator of configured behavior
        if (maxAttempts <= 0) {
            logger.info("Maximum failed authentication attempts has been set "
                    + "to {}. Automatic banning of brute-force authentication "
                    + "attempts will be disabled.", maxAttempts);
        }
        else if (banDuration <= 0) {
            logger.info("Ban duration for addresses that repeatedly fail "
                    + "authentication has been set to {}. Automatic banning "
                    + "of brute-force authentication attempts will be "
                    + "disabled.", banDuration);
        }
        else {
            logger.info("Addresses will be automatically banned for {} "
                    + "seconds after {} failed authentication attempts.",
                    banDuration, maxAttempts);
        }

        // Limit maximum number of tracked addresses to configured upper bound
        this.failures = Caffeine.newBuilder()
                .maximumSize(maxAddresses)
                .build();

        logger.info("Up to {} unique addresses will be tracked/banned at any "
                + " given time.", maxAddresses);

    }

    /**
     * Returns whether the given Credentials do not contain any specific
     * authentication parameters, including HTTP parameters. An authentication
     * request that contains no parameters whatsoever will tend to be the
     * first, anonymous, credential-less authentication attempt that results in
     * the initial login screen rendering.
     *
     * @param credentials
     *     The Credentials object to test.
     *
     * @return
     *     true if the given Credentials contain no authentication parameters
     *     whatsoever, false otherwise.
     */
    private boolean isEmpty(Credentials credentials) {

        // An authentication request that contains an explicit username or
        // password (even if blank) is non-empty, regardless of how the values
        // were passed
        if (credentials.getUsername() != null || credentials.getPassword() != null)
            return false;

        // All further tests depend on HTTP request details
        HttpServletRequest request = credentials.getRequest();
        if (request == null)
            return true;

        // An authentication request is non-empty if it contains any HTTP
        // parameters at all or contains an authentication token
        return !request.getParameterNames().hasMoreElements()
                && request.getHeader("Guacamole-Token") == null;

    }

    /**
     * Reports that the given address has just failed to authenticate and
     * returns the AuthenticationFailureStatus that represents that failure. If
     * the address isn't already being tracked, it will begin being tracked as
     * of this call. If the address is already tracked, the returned
     * AuthenticationFailureStatus will represent past authentication failures,
     * as well.
     *
     * @param address
     *     The address that has failed to authenticate.
     *
     * @return
     *     An AuthenticationFailureStatus that represents this latest
     *     authentication failure for the given address, as well as any past
     *     failures.
     */
    private AuthenticationFailureStatus getAuthenticationFailure(String address) {

        AuthenticationFailureStatus status = failures.get(address,
                (addr) -> new AuthenticationFailureStatus(maxAttempts, banDuration));

        status.notifyFailed();
        return status;

    }

    /**
     * Reports that an authentication request has been received, as well as
     * whether that request is known to have failed. If the associated address
     * is currently being blocked, an exception will be thrown.
     *
     * @param credentials
     *     The credentials associated with the authentication request.
     *
     * @param failed
     *     Whether the request is known to have failed. If the status of the
     *     request is not yet known, this should be false.
     *
     * @throws GuacamoleException
     *     If the authentication request is being blocked due to brute force
     *     prevention rules.
     */
    private void notifyAuthenticationStatus(Credentials credentials,
            boolean failed) throws GuacamoleException {

        // Do not track/ban if tracking or banning are disabled
        if (maxAttempts <= 0 || banDuration <= 0)
            return;

        // Ignore requests that do not contain explicit parameters of any kind
        if (isEmpty(credentials))
            return;

        // Determine originating address of the authentication request
        String address = credentials.getRemoteAddress();
        if (address == null)
            throw new GuacamoleServerException("Source address cannot be determined.");

        // Get current failure status for the address associated with the
        // authentication request, adding/updating that status if the request
        // was itself a failure
        AuthenticationFailureStatus status;
        if (failed) {
            status = getAuthenticationFailure(address);
            logger.debug("Authentication has failed for address \"{}\" (current total failures: {}/{}).",
                    address, status.getFailures(), maxAttempts);
        }
        else
            status = failures.getIfPresent(address);

        if (status != null) {

            // Explicitly block further processing of authentication/authorization
            // if too many failures have occurred
            if (status.isBlocked()) {
                logger.debug("Blocking authentication attempt from address \"{}\" due to number of authentication failures.", address);
                throw new TranslatableGuacamoleClientTooManyException("Too "
                        + "many failed authentication attempts.",
                        "LOGIN.ERROR_TOO_MANY_ATTEMPTS");
            }

            // Clean up tracking of failures if the address is no longer
            // relevant (all failures are sufficiently old)
            else if (!status.isValid()) {
                logger.debug("Removing address \"{}\" from tracking as there are no recent authentication failures.", address);
                failures.invalidate(address);
            }

        }

    }

    /**
     * Reports that an authentication request has been received, but it is
     * either not yet known whether the request has succeeded or failed. If the
     * associated address is currently being blocked, an exception will be
     * thrown.
     *
     * @param credentials
     *     The credentials associated with the authentication request.
     *
     * @throws GuacamoleException
     *     If the authentication request is being blocked due to brute force
     *     prevention rules.
     */
    public void notifyAuthenticationRequestReceived(Credentials credentials)
            throws GuacamoleException {
        notifyAuthenticationStatus(credentials, false);
    }

    /**
     * Reports that an authentication request has been received and has
     * succeeded. If the associated address is currently being blocked, an
     * exception will be thrown.
     *
     * @param credentials
     *     The credentials associated with the successful authentication
     *     request.
     *
     * @throws GuacamoleException
     *     If the authentication request is being blocked due to brute force
     *     prevention rules.
     */
    public void notifyAuthenticationSuccess(Credentials credentials)
            throws GuacamoleException {
        notifyAuthenticationStatus(credentials, false);
    }

    /**
     * Reports that an authentication request has been received and has
     * failed. If the associated address is currently being blocked, an
     * exception will be thrown.
     *
     * @param credentials
     *     The credentials associated with the failed authentication request.
     *
     * @throws GuacamoleException
     *     If the authentication request is being blocked due to brute force
     *     prevention rules.
     */
    public void notifyAuthenticationFailed(Credentials credentials)
            throws GuacamoleException {
        notifyAuthenticationStatus(credentials, true);
    }

}
