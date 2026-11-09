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


package org.apache.guacamole.auth.jdbc.reset;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles "Forgot password?" requests from the login page. The caller always
 * gets the same answer at the same speed: the request is only counted
 * against its source address before being queued, and the user lookup and
 * mail happen on a background thread. Mail only ever goes to the e-mail
 * address stored for the account.
 */
@Singleton
public class SelfServicePasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(SelfServicePasswordResetService.class);

    private final PasswordResetService resetService;

    private final UserService userService;

    private final Executor executor;

    /**
     * Per-address request counters, each expiring an hour after it was first
     * incremented.
     */
    private final Cache<String, AtomicInteger> addressCounts = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100000)
            .build();

    /**
     * Creates the service with a single background thread and a bounded
     * queue, so a flood of requests cannot exhaust memory.
     */
    @Inject
    public SelfServicePasswordResetService(PasswordResetService resetService,
            UserService userService) {
        this(resetService, userService, new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100),
                runnable -> {
                    Thread thread = new Thread(runnable, "password-reset-mailer");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    /**
     * Creates the service with the given executor.
     */
    SelfServicePasswordResetService(PasswordResetService resetService,
            UserService userService, Executor executor) {
        this.resetService = resetService;
        this.userService = userService;
        this.executor = executor;
    }

    /**
     * Accepts a reset request. Never reveals whether the account exists:
     * returns normally in every case.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the user is looked up.
     *
     * @param username
     *     The username typed by the requester.
     *
     * @param credentials
     *     Credentials carrying only the request's address details.
     *
     * @param maxPerAddress
     *     The maximum number of requests accepted per source address per
     *     hour.
     */
    public void request(final AuthenticationProvider authenticationProvider,
            final String username, final Credentials credentials, int maxPerAddress) {

        String address = credentials.getRemoteAddress();
        logger.info("Self-service password reset requested for \"{}\" from {}.", username, address);

        if (username == null || username.trim().isEmpty())
            return;

        if (countAddress(address) > maxPerAddress) {
            logger.warn("Self-service password reset request from {} dropped: "
                    + "more than {} requests in an hour.", address, maxPerAddress);
            return;
        }

        try {
            executor.execute(() -> process(authenticationProvider, username, credentials));
        }
        catch (RejectedExecutionException e) {
            logger.warn("Self-service password reset request for \"{}\" dropped: "
                    + "too many requests are queued.", username);
        }

    }

    /**
     * Counts a request from the given address.
     *
     * @param address
     *     The source address of the request.
     *
     * @return
     *     The number of requests from that address within the current hour,
     *     including this one.
     */
    int countAddress(String address) {

        try {
            return addressCounts.get(String.valueOf(address), AtomicInteger::new).incrementAndGet();
        }
        catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }

    }

    /**
     * Looks up the named database user.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the user is looked up.
     *
     * @param username
     *     The username typed by the requester.
     *
     * @param credentials
     *     Credentials carrying only the request's address details.
     *
     * @return
     *     The user, or null if there is no such user.
     *
     * @throws GuacamoleException
     *     If the lookup fails.
     */
    protected ModeledUser findUser(AuthenticationProvider authenticationProvider,
            String username, Credentials credentials) throws GuacamoleException {
        ModeledAuthenticatedUser user = userService.retrieveUserForPasswordReset(
                authenticationProvider, username, credentials);
        return user == null ? null : user.getUser();
    }

    /**
     * Sends the link if the account exists and is usable, and silently skips
     * it otherwise.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the user is looked up.
     *
     * @param username
     *     The username typed by the requester.
     *
     * @param credentials
     *     Credentials carrying only the request's address details.
     */
    void process(AuthenticationProvider authenticationProvider, String username,
            Credentials credentials) {

        try {

            ModeledUser user = findUser(authenticationProvider, username, credentials);
            String email = (user == null) ? null : user.getModel().getEmailAddress();

            String skipped = null;
            if (user == null)
                skipped = "no such user";
            else if (user.isDisabled())
                skipped = "account disabled";
            else if (!user.isAccountValid())
                skipped = "account not valid";
            else if (!user.isAccountAccessible())
                skipped = "outside the account's access window";
            else if (email == null || email.trim().isEmpty())
                skipped = "no e-mail address";

            if (skipped != null) {
                logger.info("Self-service password reset for \"{}\" not sent: {}.", username, skipped);
                return;
            }

            resetService.sendResetLink(user);
            logger.info("Self-service password reset link sent to user \"{}\".", user.getIdentifier());

        }
        catch (GuacamoleException | RuntimeException e) {
            logger.warn("Self-service password reset for \"{}\" failed: {}", username, e.getMessage());
            logger.debug("Self-service password reset failed.", e);
        }

    }

}
